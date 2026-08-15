package com.racks.behavior;

import com.racks.config.PluginConfig;
import com.racks.model.Rack;
import com.racks.scheduler.Scheduler;
import com.racks.service.RackService;
import com.racks.storage.ChunkPos;
import com.racks.storage.RackRepository;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Drops a hanging rack when the block it hangs on is taken away.
 *
 * <p>The data pack polled for this — one scheduled function every ten ticks, walking every wall-rack
 * controller in the world — because a data pack has no way to be told a block changed. The polling
 * is kept, because the half-second it takes for a rack to fall is something players see, but the
 * work is scoped down hard: one repeating task per <b>chunk that actually contains a wall rack</b>,
 * checking only the racks in that chunk. A server with no wall racks schedules nothing at all.
 *
 * <p>Pinning the task to a chunk is also what makes this correct on Folia. The check has to read a
 * block, which may only be done from the thread owning that block's region;
 * {@link io.papermc.paper.threadedregions.scheduler.RegionScheduler} gives exactly that thread on
 * Folia and the main thread on Paper, so one implementation serves both. It also reproduces the data
 * pack's reach for free: a region that is not ticking runs no task, just as {@code @e[...]} never
 * saw entities in unloaded chunks.
 */
public final class WallSupportService implements RackRepository.WallRackListener {

    private final Supplier<PluginConfig> config;
    private final Scheduler scheduler;
    private final RackRepository repository;
    private final RackService service;

    private final Map<ChunkPos, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private volatile boolean running;

    public WallSupportService(Supplier<PluginConfig> config, Scheduler scheduler,
                              RackRepository repository, RackService service) {
        this.config = config;
        this.scheduler = scheduler;
        this.repository = repository;
        this.service = service;
    }

    /** Starts checking. Called on enable, once every stored rack is in the index. */
    public void start() {
        running = true;
        if (config.get().isIgnoreWallRackSupport()) {
            return;
        }
        for (ChunkPos chunk : repository.chunksWithWallRacks()) {
            ensureTask(chunk);
        }
    }

    /**
     * Re-reads the setting and starts or stops accordingly. The data pack's
     * {@code settings/ignore_wall_rack_support/{true,false}} cleared and re-scheduled its function;
     * this does the same thing to the per-chunk tasks.
     */
    public void refresh() {
        if (!running) return;
        if (config.get().isIgnoreWallRackSupport()) {
            cancelAll();
        } else {
            for (ChunkPos chunk : repository.chunksWithWallRacks()) {
                ensureTask(chunk);
            }
        }
    }

    public void shutdown() {
        running = false;
        cancelAll();
    }

    // ------------------------------------------------------------------------------------------------
    // Task lifecycle
    // ------------------------------------------------------------------------------------------------

    @Override
    public void onWallRackAdded(ChunkPos chunk) {
        if (running && !config.get().isIgnoreWallRackSupport()) {
            ensureTask(chunk);
        }
    }

    @Override
    public void onWallRackChunkEmptied(ChunkPos chunk) {
        ScheduledTask task = tasks.remove(chunk);
        if (task != null) {
            task.cancel();
        }
    }

    private void ensureTask(ChunkPos chunk) {
        World world = Bukkit.getWorld(chunk.world());
        if (world == null) {
            return; // the world is not loaded; the task is created when a rack there is next touched
        }
        int period = config.get().getWallSupportCheckInterval();
        tasks.computeIfAbsent(chunk, pos ->
                scheduler.runChunkTimer(world, pos.x(), pos.z(), () -> check(pos, world), period, period));
    }

    private void cancelAll() {
        for (ScheduledTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
    }

    // ------------------------------------------------------------------------------------------------
    // The check itself
    // ------------------------------------------------------------------------------------------------

    /**
     * Runs on the thread owning {@code chunk}. Anything without a support block behind it is broken
     * with no breaker, so it drops itself and its contents the way an unattended break should.
     */
    private void check(ChunkPos chunk, World world) {
        if (config.get().isIgnoreWallRackSupport()) {
            return;
        }
        // On Paper the task ticks whether or not the chunk is loaded, so the load test is what keeps
        // this from pulling chunks back in. On Folia the region simply is not ticking and the task
        // never fires in the first place.
        if (!world.isChunkLoaded(chunk.x(), chunk.z())) {
            return;
        }

        // Snapshot before breaking: breakRack removes the rack from the very set being iterated.
        List<Rack> unsupported = null;
        for (Rack rack : repository.wallRacksIn(chunk)) {
            if (!hasSupport(rack, world)) {
                if (unsupported == null) {
                    unsupported = new ArrayList<>(2);
                }
                unsupported.add(rack);
            }
        }
        if (unsupported == null) {
            return;
        }
        for (Rack rack : unsupported) {
            service.breakRack(rack, world, null);
        }
    }

    /**
     * Whether the block behind {@code rack} still holds it up.
     *
     * <p>Deliberately tests for {@code minecraft:air} exactly, not "any air-like block", because
     * {@code blocks/rack/wall_has_support} did: a rack hung against cave air or void air counted as
     * supported there and counts as supported here.
     */
    private boolean hasSupport(Rack rack, World world) {
        Block block = rack.key().toLocation(world).getBlock();
        return block.getRelative(rack.facing().supportFace()).getType() != Material.AIR;
    }

    /** Rebuilds every task, picking up a changed check interval. Used by {@code /racks reload}. */
    public void restart() {
        cancelAll();
        refresh();
    }
}
