package com.racks.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Task scheduler built on Paper's region-aware scheduler API
 * ({@code io.papermc.paper.threadedregions.scheduler.*}). Paper implements that API on a plain
 * server too (where every "region" is the main thread), so nothing here needs to branch on the
 * platform — {@link #isFolia()} exists only for logging.
 *
 * <p>The rule the rest of the plugin follows: <b>a rack is only ever mutated from the thread that
 * owns its block</b>. On Paper that is the main thread; on Folia it is the region thread. Bukkit
 * delivers the interaction/place/break events for a rack on exactly that thread already, so the
 * hot paths need no hand-off at all — this class is for the periodic support check, the database
 * executor hand-off and shutdown.
 */
public final class Scheduler {

    private final Plugin plugin;
    private final boolean folia;

    public Scheduler(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** True when running on Folia rather than plain Paper (or a Paper fork). */
    public boolean isFolia() {
        return folia;
    }

    /** Runs once, off the region/main thread, as soon as possible. */
    public void runAsync(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    /** Runs once, off-thread, after {@code delay}. */
    public void runLaterAsync(Runnable task, long delay, TimeUnit unit) {
        Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.run(), delay, unit);
    }

    /** Runs once on the global bookkeeping thread (main thread on Paper), next tick. */
    public void runNextTick(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
    }

    /** Runs once on the thread that owns {@code location}'s region (main thread on Paper). */
    public void runAtLocation(Location location, Runnable task) {
        Bukkit.getRegionScheduler().run(plugin, location, t -> task.run());
    }

    /**
     * Runs once on the thread that owns {@code entity}, after {@code delayTicks}. An entity carries
     * its own scheduler because it moves between regions; the task is simply dropped if the entity
     * is gone by then (a player who logged straight back out, say).
     */
    public void runAtEntityLater(Entity entity, Runnable task, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, t -> task.run(), null, Math.max(1, delayTicks));
    }

    /**
     * Repeating task pinned to one chunk's region. Used for the wall-support sweep, which must read
     * blocks and therefore has to run on the owning thread. Cancel it through the returned handle.
     */
    public ScheduledTask runChunkTimer(World world, int chunkX, int chunkZ,
                                       Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getRegionScheduler().runAtFixedRate(
                plugin, world, chunkX, chunkZ, t -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
    }

    /** Cancels every task this plugin scheduled through the async and global-region schedulers. */
    public void cancelAllTasks() {
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
    }
}
