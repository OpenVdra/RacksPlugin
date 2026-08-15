package com.racks.model;

/**
 * A rack's two item slots. A ground rack shows both; a wall rack only ever uses {@link #LEFT},
 * exactly as in the data pack (its wall item-display creator summons a single left display).
 */
public enum RackPart {
    LEFT,
    RIGHT;

    /** Index into the stored two-element item array — {@code items[0]} / {@code items[1]}. */
    public int index() {
        return ordinal();
    }

    /** Scoreboard tag the data pack put on the matching entities. */
    public String tag() {
        return "pk.racks.block.rack.part." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
