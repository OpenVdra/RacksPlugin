package com.racks.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.UUID;

/**
 * Identity of a rack: the block it occupies. Used as the key of the in-memory index, so the
 * interaction path resolves a rack with one hash lookup and no database round trip.
 *
 * <p>A rack is one block, and a block holds at most one rack, so the position <i>is</i> the identity
 * — which is also what makes the {@code UNIQUE(world, x, y, z)} constraint in the schema a real
 * guard against a duplicate row ever being written for the same spot.
 */
public record RackKey(UUID world, int x, int y, int z) {

    public static RackKey of(Block block) {
        return new RackKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static RackKey of(Location location) {
        return new RackKey(location.getWorld().getUID(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /** Chunk this rack lives in, packed the way Bukkit packs chunk keys. */
    public long chunkKey() {
        return ((long) (z >> 4) << 32) | ((x >> 4) & 0xFFFFFFFFL);
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }

    /** Corner of the rack's block. Displays and drops are positioned relative to this. */
    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }

    /**
     * Centre of the rack's block — {@code align xyz positioned ~0.5 ~0.5 ~0.5}, the anchor the data
     * pack summoned the controller, body displays and item displays at.
     */
    public Location center(World world) {
        return new Location(world, x + 0.5, y + 0.5, z + 0.5);
    }

    /**
     * Centre of the block's <i>floor</i> — {@code align xyz positioned ~0.5 ~ ~0.5}, the anchor for
     * interaction entities, whose position is the bottom of their hitbox rather than its middle.
     */
    public Location floorCenter(World world) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }
}
