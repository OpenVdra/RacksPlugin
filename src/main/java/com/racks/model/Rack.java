package com.racks.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One placed rack: the plugin's replacement for an entry of the data pack's
 * {@code pk:racks database.blocks.racks} list (see {@code blocks/rack/data/_structure}).
 *
 * <h2>Threading</h2>
 * The immutable half — id, variant, type, facing, position, owner, creation time — is set once at
 * construction. The mutable half — {@link #pose()} and the two item slots — is only ever written
 * from the thread that owns the rack's block: the main thread on Paper, the region thread on Folia.
 * Bukkit already delivers the place/interact/break events for a rack on exactly that thread, so the
 * hot paths need no locking; the fields are {@code volatile} purely so the database writer thread,
 * which only ever reads, cannot observe a torn or stale value.
 *
 * <h2>Item ownership</h2>
 * Stored stacks are never handed out or taken in by reference: {@link #setItem} copies what it is
 * given and {@link #item} returns a copy. That is what makes it impossible for a caller to hold a
 * live alias of a stack that is also on a rack — the shape most item-duplication bugs take.
 */
public final class Rack {

    private final int id;
    private final RackKey key;
    private final RackVariant variant;
    private final RackType type;
    private final RackFacing facing;
    private final @Nullable UUID owner;
    private final long createdAtGameTime;

    private volatile short pose;
    private volatile @Nullable ItemStack left;
    private volatile @Nullable ItemStack right;

    public Rack(int id, RackKey key, RackVariant variant, RackType type, RackFacing facing,
                @Nullable UUID owner, long createdAtGameTime, short pose,
                @Nullable ItemStack left, @Nullable ItemStack right) {
        this.id = id;
        this.key = key;
        this.variant = variant;
        this.type = type;
        this.facing = facing;
        this.owner = owner;
        this.createdAtGameTime = createdAtGameTime;
        this.pose = pose;
        this.left = copyOrNull(left);
        this.right = copyOrNull(right);
    }

    public int id() {
        return id;
    }

    public RackKey key() {
        return key;
    }

    public RackVariant variant() {
        return variant;
    }

    public RackType type() {
        return type;
    }

    public RackFacing facing() {
        return facing;
    }

    /**
     * UUID of whoever placed the rack. Recorded because the data pack recorded it; like the data
     * pack, nothing reads it to decide who may break or use a rack.
     */
    public @Nullable UUID owner() {
        return owner;
    }

    /** World game time the rack was placed at, for the {@code lootable-delay} grace period. */
    public long createdAtGameTime() {
        return createdAtGameTime;
    }

    public short pose() {
        return pose;
    }

    public void setPose(short pose) {
        this.pose = pose;
    }

    /** Advances to the next pose, wrapping at this rack type's pose count. Returns the new pose. */
    public short cyclePose() {
        short next = (short) (pose + 1);
        if (next >= type.poseCount()) {
            next = 0;
        }
        this.pose = next;
        return next;
    }

    /** A copy of the stack in {@code part}, or null when that slot is empty. */
    public @Nullable ItemStack item(RackPart part) {
        return copyOrNull(part == RackPart.LEFT ? left : right);
    }

    /** Stores a copy of {@code item} in {@code part}; an empty or null stack clears the slot. */
    public void setItem(RackPart part, @Nullable ItemStack item) {
        ItemStack stored = copyOrNull(item);
        if (part == RackPart.LEFT) {
            this.left = stored;
        } else {
            this.right = stored;
        }
    }

    public boolean isEmpty() {
        return left == null && right == null;
    }

    private static @Nullable ItemStack copyOrNull(@Nullable ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }
}
