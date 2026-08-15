package com.racks.listener;

import com.racks.model.Rack;
import com.racks.service.RackService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Keeps a rack and its barrier from ever coming apart.
 *
 * <p>A ground rack stands on a barrier, which nothing in survival can break — but an operator in
 * creative can, and the data pack had no answer for that: the barrier went and the rack's entities
 * stayed, leaving a rack floating in the air that no longer existed as far as anything was concerned.
 *
 * <p>Here that break is treated as breaking the rack, which is what the person swinging clearly
 * meant. Nothing is lost that the data pack would have kept, and no orphaned entities or database
 * rows are left behind.
 */
public final class RackBlockListener implements Listener {

    private final RackService service;

    public RackBlockListener(RackService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Rack rack = service.rackAt(event.getBlock());
        if (rack == null) {
            return;
        }
        // Cancelled so the block is not broken twice: breakRack clears the barrier itself, along
        // with the entities and the database row, and drops whatever the rack was holding.
        event.setCancelled(true);
        service.breakRack(rack, event.getBlock().getWorld(), event.getPlayer());
    }
}
