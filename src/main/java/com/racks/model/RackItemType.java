package com.racks.model;

/**
 * How a held item is posed on a rack.
 *
 * <p>The data pack derived this in {@code items/update/define_item_type} and then dispatched to
 * {@code update/<ground|wall>/<left|right>/<type>}; the constants and their order here are that
 * function, one for one. Order matters: the first match wins, so an item that is in several vanilla
 * tags resolves the same way it did in the data pack.
 *
 * <p>{@link #NONE} covers an empty slot and any item with no transform table, which the data pack
 * expressed by dispatching to a function that does not exist — the display simply keeps whatever
 * transform it had, and since it holds no item there is nothing to see.
 */
public enum RackItemType {
    AXE,
    HOE,
    PICKAXE,
    SHOVEL,
    SPEAR,
    SWORD,
    BOW,
    CARROT_ON_A_STICK,
    CROSSBOW,
    FISHING_ROD,
    SHEARS,
    SHIELD,
    SPYGLASS,
    TRIDENT,
    MACE,
    WARPED_FUNGUS_ON_A_STICK,
    NONE
}
