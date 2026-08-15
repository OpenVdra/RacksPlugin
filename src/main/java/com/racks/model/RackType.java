package com.racks.model;

/**
 * Whether a rack sits on the floor or hangs on a wall. The two have different bodies, different
 * interaction hitboxes, a different number of item slots and a different pose count — everything
 * the data pack branched on with {@code temp.rack{wall:1b}}.
 */
public enum RackType {

    /** Six fence displays, three interactions, two item slots, poses 0–5. */
    GROUND(6),

    /** Two button + two fence displays, two interactions, one item slot, poses 0–3. */
    WALL(4);

    private final int poseCount;

    RackType(int poseCount) {
        this.poseCount = poseCount;
    }

    /** Number of distinct poses; cycling past the last one wraps to 0. */
    public int poseCount() {
        return poseCount;
    }

    public boolean isWall() {
        return this == WALL;
    }

    /** Scoreboard tag the data pack put on this type's entities. */
    public String tag() {
        return "pk.racks.block.rack.type." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static RackType of(boolean wall) {
        return wall ? WALL : GROUND;
    }
}
