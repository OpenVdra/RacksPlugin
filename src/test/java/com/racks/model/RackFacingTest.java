package com.racks.model;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The yaw-to-facing buckets from {@code data/create/set_facing}, boundaries included.
 *
 * <p>Worth pinning down because the data pack read the player's yaw into a scoreboard score, which
 * truncates toward zero rather than rounding — so −134.9 belongs to the −134..−45 bucket, not the one
 * below it. Getting that wrong turns a rack the wrong way only for players standing at certain
 * angles, which is exactly the kind of thing nobody would think to test by hand.
 */
class RackFacingTest {

    @Test
    void aRackFacesThePlayerWhoPlacedIt() {
        // A player looking south (yaw 0) gets a rack facing north, back at them.
        assertEquals(RackFacing.NORTH, RackFacing.fromPlayerYaw(0f));
        assertEquals(RackFacing.EAST, RackFacing.fromPlayerYaw(90f));
        assertEquals(RackFacing.SOUTH, RackFacing.fromPlayerYaw(180f));
        assertEquals(RackFacing.WEST, RackFacing.fromPlayerYaw(-90f));
    }

    @Test
    void bucketBoundariesMatchTheDataPack() {
        assertEquals(RackFacing.SOUTH, RackFacing.fromPlayerYaw(-135f));
        assertEquals(RackFacing.WEST, RackFacing.fromPlayerYaw(-134f));
        assertEquals(RackFacing.WEST, RackFacing.fromPlayerYaw(-45f));
        assertEquals(RackFacing.NORTH, RackFacing.fromPlayerYaw(-44f));
        assertEquals(RackFacing.NORTH, RackFacing.fromPlayerYaw(44f));
        assertEquals(RackFacing.EAST, RackFacing.fromPlayerYaw(45f));
        assertEquals(RackFacing.EAST, RackFacing.fromPlayerYaw(134f));
        assertEquals(RackFacing.SOUTH, RackFacing.fromPlayerYaw(135f));
    }

    @Test
    void fractionsAreTruncatedTowardZeroLikeAScoreboardScore() {
        // -134.9 truncates to -134, which is west. Rounding would have made it south.
        assertEquals(RackFacing.WEST, RackFacing.fromPlayerYaw(-134.9f));
        // 44.9 truncates to 44, which is north. Rounding would have made it east.
        assertEquals(RackFacing.NORTH, RackFacing.fromPlayerYaw(44.9f));
    }

    @Test
    void supportIsTheBlockBehindAWallRack() {
        assertEquals(BlockFace.SOUTH, RackFacing.NORTH.supportFace());
        assertEquals(BlockFace.WEST, RackFacing.EAST.supportFace());
        assertEquals(BlockFace.NORTH, RackFacing.SOUTH.supportFace());
        assertEquals(BlockFace.EAST, RackFacing.WEST.supportFace());
    }

    @Test
    void idsRoundTrip() {
        for (RackFacing facing : RackFacing.values()) {
            assertEquals(facing, RackFacing.byId(facing.id()));
            assertEquals(facing, RackFacing.of(facing.blockFace()));
        }
    }
}
