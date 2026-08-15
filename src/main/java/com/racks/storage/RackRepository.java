package com.racks.storage;

import com.racks.model.Rack;
import com.racks.model.RackFacing;
import com.racks.model.RackKey;
import com.racks.model.RackType;
import com.racks.model.RackVariant;
import com.racks.serialization.CodecException;
import com.racks.serialization.ItemCodec;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The plugin's view of every placed rack: an in-memory index in front of {@link SqliteRackStorage},
 * with a single-threaded write-behind writer.
 *
 * <h2>Reads never touch the database</h2>
 * Racks are world objects, not per-player data — there are at most a few thousand and they are all
 * loaded once on enable. So {@link #get(RackKey)}, which runs on every interaction with any rack, is
 * a single hash lookup on a {@link ConcurrentHashMap} with no I/O, no lock and no allocation.
 *
 * <h2>Writes never block a server thread</h2>
 * A change is applied to the in-memory {@link Rack} on the owning region thread, snapshotted into an
 * immutable {@link RackRow} <b>there</b> (so the item stacks are serialized by the thread that owns
 * them), and the row is handed to a single writer thread. Because that thread is single and the
 * queue is FIFO, and because a given rack is only ever mutated from its own region thread, the
 * writes for one rack reach SQLite in exactly the order they happened — the last one wins and it is
 * the right one.
 *
 * <h2>Why this cannot duplicate items</h2>
 * The in-memory rack is the single source of truth while the server runs; the database is a
 * write-behind mirror of it. Nothing reads a rack back from SQLite until the next startup, so a slow
 * or failed write can never produce a second copy of an item — at worst a crash loses the last
 * milliseconds of writes, and {@link #flush()} on disable drains the queue before shutdown
 * completes.
 */
public final class RackRepository implements AutoCloseable {

    private final SqliteRackStorage storage;
    private final ItemCodec codec;
    private final Logger logger;

    private final Map<RackKey, Rack> byKey = new ConcurrentHashMap<>();
    private final Map<Integer, Rack> byId = new ConcurrentHashMap<>();

    /**
     * Every rack bucketed by chunk, so "which racks are here" can be answered when a chunk's entities
     * load without walking the whole index.
     */
    private final Map<ChunkPos, Set<Rack>> racksByChunk = new ConcurrentHashMap<>();

    /**
     * Wall racks only, bucketed the same way. Kept as its own map rather than filtered out of the one
     * above because the support sweep asks the question every few ticks per chunk and should not pay
     * to skip past ground racks each time — and because a chunk holding only ground racks then needs
     * no sweep task at all.
     */
    private final Map<ChunkPos, Set<Rack>> wallRacksByChunk = new ConcurrentHashMap<>();

    private final AtomicInteger nextId = new AtomicInteger();
    private final ExecutorService writer;
    private volatile boolean closed;

    /**
     * Told when a chunk gains its first wall rack or loses its last, so the support sweep can create
     * and cancel its per-chunk tasks precisely instead of polling for changes. Set once after
     * construction, which is what keeps the repository from having to know about the sweep at all.
     */
    private volatile @Nullable WallRackListener wallRackListener;

    /** @see #wallRackListener */
    public interface WallRackListener {

        /** A chunk now holds at least one wall rack. May be called again for a chunk already active. */
        void onWallRackAdded(ChunkPos chunk);

        /** A chunk no longer holds any wall rack. */
        void onWallRackChunkEmptied(ChunkPos chunk);
    }

    public void setWallRackListener(@Nullable WallRackListener listener) {
        this.wallRackListener = listener;
    }

    public RackRepository(SqliteRackStorage storage, ItemCodec codec, Logger logger) {
        this.storage = storage;
        this.codec = codec;
        this.logger = logger;
        ThreadFactory factory = r -> {
            Thread thread = new Thread(r, "Racks-DB");
            thread.setDaemon(true);
            return thread;
        };
        this.writer = Executors.newSingleThreadExecutor(factory);
    }

    // ------------------------------------------------------------------------------------------------
    // Startup
    // ------------------------------------------------------------------------------------------------

    /**
     * Loads every stored rack into memory. Runs on the enable thread, before any listener exists.
     *
     * <p>A row whose items cannot be decoded is logged and <b>skipped</b> rather than loaded with an
     * empty slot: leaving the row untouched means a later plugin update can still recover it, where
     * loading it empty would quietly destroy somebody's gear the first time the rack was saved again.
     *
     * @return how many racks were loaded
     */
    public int loadAll() throws SQLException {
        List<RackRow> rows = storage.loadAll();
        int highestId = storage.maxId();
        nextId.set(highestId);

        int loaded = 0;
        for (RackRow row : rows) {
            Rack rack = toRack(row);
            if (rack == null) continue;
            index(rack);
            loaded++;
        }
        return loaded;
    }

    private @Nullable Rack toRack(RackRow row) {
        RackVariant variant = RackVariant.byId(row.variant());
        if (variant == null) {
            logger.warn("Rack #{} has unknown variant '{}' — skipping it", row.id(), row.variant());
            return null;
        }
        RackFacing facing = RackFacing.byId(row.facing());
        if (facing == null) {
            logger.warn("Rack #{} has unknown facing '{}' — treating it as north", row.id(), row.facing());
            facing = RackFacing.NORTH;
        }
        ItemStack left;
        ItemStack right;
        try {
            left = codec.decode(row.left());
            right = codec.decode(row.right());
        } catch (CodecException e) {
            logger.error("Rack #{} at {} {},{},{} holds an item that could not be read — leaving the row "
                            + "untouched and skipping the rack so nothing is lost",
                    row.id(), row.world(), row.x(), row.y(), row.z(), e);
            return null;
        }
        RackKey key = new RackKey(row.world(), row.x(), row.y(), row.z());
        return new Rack(row.id(), key, variant, RackType.of(row.wall()), facing,
                row.owner(), row.createdAt(), row.pose(), left, right);
    }

    // ------------------------------------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------------------------------------

    /** The rack occupying {@code key}, or null. One hash lookup; safe from any thread. */
    public @Nullable Rack get(RackKey key) {
        return byKey.get(key);
    }

    public @Nullable Rack byId(int id) {
        return byId.get(id);
    }

    public boolean contains(RackKey key) {
        return byKey.containsKey(key);
    }

    public int size() {
        return byKey.size();
    }

    public Collection<Rack> all() {
        return byKey.values();
    }

    /** Racks in one chunk, or an empty set. The returned set is live — iterate, do not mutate. */
    public Set<Rack> racksIn(ChunkPos chunk) {
        return racksByChunk.getOrDefault(chunk, Set.of());
    }

    /** Wall racks in one chunk, or an empty set. The returned set is live — iterate, do not mutate. */
    public Set<Rack> wallRacksIn(ChunkPos chunk) {
        return wallRacksByChunk.getOrDefault(chunk, Set.of());
    }

    /** Chunks that currently hold at least one wall rack. */
    public Set<ChunkPos> chunksWithWallRacks() {
        return wallRacksByChunk.keySet();
    }

    /** Next free rack id. Mirrors the data pack's {@code $next pk.custom_block.component.id} counter. */
    public int nextId() {
        return nextId.incrementAndGet();
    }

    // ------------------------------------------------------------------------------------------------
    // Mutations — call from the thread that owns the rack's block
    // ------------------------------------------------------------------------------------------------

    /**
     * Registers a newly placed (or adopted) rack and persists it.
     *
     * @return false if another rack already occupies that block, in which case nothing is changed
     */
    public boolean add(Rack rack) {
        if (byKey.putIfAbsent(rack.key(), rack) != null) {
            return false;
        }
        byId.put(rack.id(), rack);
        addToChunkBucket(rack);

        RackRow row = RackRow.of(rack, codec);
        submit("insert rack #" + rack.id(), () -> storage.insert(row));
        return true;
    }

    /** Persists a change to a rack's pose or items. The snapshot is taken here, on the caller's thread. */
    public void save(Rack rack) {
        RackRow row = RackRow.of(rack, codec);
        submit("update rack #" + rack.id(), () -> storage.update(row));
    }

    /** Removes a broken rack from the index and the database. */
    public void remove(Rack rack) {
        byKey.remove(rack.key(), rack);
        byId.remove(rack.id(), rack);
        removeFromChunkBucket(rack);

        int id = rack.id();
        submit("delete rack #" + id, () -> storage.delete(id));
    }

    private void index(Rack rack) {
        byKey.put(rack.key(), rack);
        byId.put(rack.id(), rack);
        addToChunkBucket(rack);
    }

    private void addToChunkBucket(Rack rack) {
        ChunkPos chunk = ChunkPos.of(rack.key());
        racksByChunk.computeIfAbsent(chunk, k -> ConcurrentHashMap.newKeySet()).add(rack);

        if (!rack.type().isWall()) return;
        wallRacksByChunk
                .computeIfAbsent(chunk, k -> ConcurrentHashMap.newKeySet())
                .add(rack);

        WallRackListener listener = wallRackListener;
        if (listener != null) {
            listener.onWallRackAdded(chunk);
        }
    }

    private void removeFromChunkBucket(Rack rack) {
        ChunkPos chunk = ChunkPos.of(rack.key());
        racksByChunk.computeIfPresent(chunk, (k, set) -> {
            set.remove(rack);
            return set.isEmpty() ? null : set;
        });

        if (!rack.type().isWall()) return;
        // computeIfPresent so removing the last rack and dropping the bucket happen under one map
        // lock — otherwise a concurrent add could land in a bucket that is about to disappear.
        boolean emptied = wallRacksByChunk.computeIfPresent(chunk, (k, set) -> {
            set.remove(rack);
            return set.isEmpty() ? null : set;
        }) == null;

        WallRackListener listener = wallRackListener;
        if (emptied && listener != null) {
            listener.onWallRackChunkEmptied(chunk);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Writer
    // ------------------------------------------------------------------------------------------------

    private void submit(String description, SqlTask task) {
        if (closed) {
            logger.warn("Dropping database work ({}) — the plugin is shutting down", description);
            return;
        }
        try {
            writer.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    logger.error("Database write failed: {}", description, e);
                }
            });
        } catch (RejectedExecutionException e) {
            logger.warn("Dropping database work ({}) — the writer is no longer accepting tasks", description);
        }
    }

    /**
     * Blocks until every queued write has been applied. Called on disable, so a restart never loses
     * the last few interactions.
     */
    public void flush() {
        writer.shutdown();
        closed = true;
        try {
            if (!writer.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Timed out waiting for pending rack writes to finish");
                writer.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writer.shutdownNow();
        }
    }

    @Override
    public void close() {
        flush();
        storage.close();
    }

    /** A unit of database work; checked exceptions are logged by the writer rather than thrown away. */
    @FunctionalInterface
    private interface SqlTask {
        void run() throws Exception;
    }
}
