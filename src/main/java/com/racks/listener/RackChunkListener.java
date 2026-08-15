package com.racks.listener;

import com.racks.config.PluginConfig;
import com.racks.migration.DatapackAdopter;
import com.racks.model.Rack;
import com.racks.render.RackEntityKeys;
import com.racks.render.RackRenderer;
import com.racks.scheduler.Scheduler;
import com.racks.storage.ChunkPos;
import com.racks.storage.RackRepository;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Marker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Reconciles what a chunk contains with what the database says, once its entities are available.
 *
 * <p>Two jobs:
 * <ul>
 *   <li><b>Adopt</b> racks the original data pack left standing here (see {@link DatapackAdopter}).</li>
 *   <li><b>Repair</b> a rack the database knows about whose entities are gone — a chunk restored from
 *       an older backup, an over-eager {@code /kill}, a crash between the database write and the
 *       chunk save. Without this the rack is a row nobody can see or click; with it, it comes back
 *       holding what it was holding.</li>
 * </ul>
 *
 * <p>Both are rare, and both spawn and remove entities, so the event handler itself only <i>decides</i>
 * whether there is anything to do — a scan of the list it was handed, with no world mutation — and
 * hands the work to the next tick on the chunk's own thread. Nothing this plugin does can then land
 * in the middle of the server's own chunk-loading work.
 */
public final class RackChunkListener implements Listener {

    private final Supplier<PluginConfig> config;
    private final RackRepository repository;
    private final RackRenderer renderer;
    private final RackEntityKeys keys;
    private final DatapackAdopter adopter;
    private final Scheduler scheduler;
    private final Logger logger;

    public RackChunkListener(Supplier<PluginConfig> config, RackRepository repository, RackRenderer renderer,
                             RackEntityKeys keys, DatapackAdopter adopter, Scheduler scheduler, Logger logger) {
        this.config = config;
        this.repository = repository;
        this.renderer = renderer;
        this.keys = keys;
        this.adopter = adopter;
        this.scheduler = scheduler;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        ChunkPos pos = new ChunkPos(world.getUID(), chunk.getX(), chunk.getZ());

        boolean adopt = config.get().isAdoptDatapackRacks();
        Set<Rack> known = repository.racksIn(pos);
        if (known.isEmpty() && !adopt) {
            return; // by far the common case: no racks here and nothing to import
        }

        Set<Integer> controllersPresent = new HashSet<>();
        boolean sawOrphan = false;
        for (Entity entity : event.getEntities()) {
            Integer rackId = keys.rackIdOf(entity);
            if (rackId != null) {
                if (keys.roleOf(entity) == RackEntityKeys.Role.CONTROLLER) {
                    controllersPresent.add(rackId);
                }
            } else if (adopt && entity instanceof Marker
                    && entity.getScoreboardTags().contains(RackEntityKeys.TAG_CONTROLLER)) {
                sawOrphan = true;
            }
        }

        List<Rack> broken = new ArrayList<>(0);
        for (Rack rack : known) {
            if (!controllersPresent.contains(rack.id())) {
                broken.add(rack);
            }
        }
        if (broken.isEmpty() && !sawOrphan) {
            return;
        }

        boolean adoptNow = sawOrphan;
        scheduler.runAtLocation(chunk.getBlock(8, world.getMinHeight(), 8).getLocation(),
                () -> reconcile(chunk, world, broken, adoptNow));
    }

    /** Runs a tick later, on the chunk's own thread, with the entity list re-read. */
    private void reconcile(Chunk chunk, World world, List<Rack> broken, boolean adopt) {
        if (!chunk.isLoaded()) {
            return;
        }
        for (Rack rack : broken) {
            // The rack may have been broken in the meantime; only rebuild what is still registered.
            if (repository.byId(rack.id()) != rack) {
                continue;
            }
            try {
                // Despawn first: the rack may be half there, and spawning on top of stray parts
                // would leave two frames in the same place.
                renderer.despawn(rack, world);
                renderer.spawn(rack, world);
                logger.info("Rebuilt the missing entities of rack #{} at {} {},{},{}",
                        rack.id(), world.getName(), rack.key().x(), rack.key().y(), rack.key().z());
            } catch (RuntimeException e) {
                logger.error("Could not rebuild rack #{}", rack.id(), e);
            }
        }

        if (adopt) {
            adoptAll(chunk, world);
        }
    }

    private void adoptAll(Chunk chunk, World world) {
        List<Entity> entities = List.of(chunk.getEntities());
        List<Entity> controllers = new ArrayList<>(2);
        for (Entity entity : entities) {
            if (keys.rackIdOf(entity) == null && entity instanceof Marker
                    && entity.getScoreboardTags().contains(RackEntityKeys.TAG_CONTROLLER)) {
                controllers.add(entity);
            }
        }

        int adopted = 0;
        for (Entity controller : controllers) {
            try {
                if (adopter.adopt(controller, entities, world)) {
                    adopted++;
                }
            } catch (RuntimeException e) {
                logger.error("Failed to adopt a data pack rack at {}", controller.getLocation(), e);
            }
        }
        if (adopted > 0) {
            logger.info("Adopted {} rack(s) left behind by the Racks data pack in {}", adopted, world.getName());
        }
    }
}
