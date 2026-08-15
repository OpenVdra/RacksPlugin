package com.racks.storage;

import com.racks.model.Rack;
import com.racks.serialization.ItemCodec;
import com.racks.model.RackPart;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One database row, with its items already serialized to bytes.
 *
 * <p>This is the hand-off type between the server threads and the database writer. A row is built on
 * the thread that owns the rack — the only thread allowed to touch its {@link org.bukkit.inventory.ItemStack}s
 * — and everything in it is immutable and Bukkit-free from then on, so the writer thread can hold on
 * to it for as long as it likes without any chance of observing a half-written rack.
 */
public record RackRow(int id, UUID world, int x, int y, int z, String variant, boolean wall,
                      String facing, short pose, @Nullable UUID owner, long createdAt,
                      byte @Nullable [] left, byte @Nullable [] right) {

    /**
     * Snapshots {@code rack} into a row. Call on the rack's owning thread; the returned row is safe
     * to pass anywhere.
     */
    public static RackRow of(Rack rack, ItemCodec codec) {
        return new RackRow(
                rack.id(),
                rack.key().world(), rack.key().x(), rack.key().y(), rack.key().z(),
                rack.variant().id(),
                rack.type().isWall(),
                rack.facing().id(),
                rack.pose(),
                rack.owner(),
                rack.createdAtGameTime(),
                codec.encode(rack.item(RackPart.LEFT)),
                codec.encode(rack.item(RackPart.RIGHT)));
    }
}
