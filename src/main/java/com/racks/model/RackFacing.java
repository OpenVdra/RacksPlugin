package com.racks.model;

import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The four directions a rack can face, in the data pack's own order — the ordinal is the value it
 * stored in the {@code pk.custom_block.facing} scoreboard objective, and {@link #yaw()} is the
 * rotation it applied to every body and item display.
 */
public enum RackFacing {

    NORTH(0f, BlockFace.NORTH),
    EAST(90f, BlockFace.EAST),
    SOUTH(180f, BlockFace.SOUTH),
    WEST(270f, BlockFace.WEST);

    private final float yaw;
    private final BlockFace blockFace;

    RackFacing(float yaw, BlockFace blockFace) {
        this.yaw = yaw;
        this.blockFace = blockFace;
    }

    /** Entity yaw the data pack teleported the displays to. */
    public float yaw() {
        return yaw;
    }

    public BlockFace blockFace() {
        return blockFace;
    }

    /**
     * The block a wall rack hangs on: directly behind it. This is the direction
     * {@code blocks/rack/wall_has_support} checked, expressed there as a per-facing coordinate
     * offset (facing north → check +Z, east → −X, south → −Z, west → +X).
     */
    public BlockFace supportFace() {
        return blockFace.getOppositeFace();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @Nullable RackFacing byId(@Nullable String id) {
        if (id == null) return null;
        for (RackFacing f : values()) {
            if (f.id().equals(id.toLowerCase(Locale.ROOT))) return f;
        }
        return null;
    }

    public static RackFacing of(BlockFace face) {
        for (RackFacing f : values()) {
            if (f.blockFace == face) return f;
        }
        return NORTH;
    }

    /**
     * The facing a rack gets from the player who placed it: the opposite of where they are looking,
     * so a freshly placed rack always faces its owner.
     *
     * <p>Reproduces {@code data/create/set_facing} exactly, including its integer truncation — the
     * data pack read the yaw into a scoreboard score, which drops the fraction toward zero, so a yaw
     * of −134.9 lands in the −134..−45 bucket rather than the one below it.
     */
    public static RackFacing fromPlayerYaw(float rawYaw) {
        int yaw = (int) rawYaw;
        if (yaw <= -135) return SOUTH;
        if (yaw <= -45) return WEST;
        if (yaw <= 44) return NORTH;
        if (yaw <= 134) return EAST;
        return SOUTH;
    }
}
