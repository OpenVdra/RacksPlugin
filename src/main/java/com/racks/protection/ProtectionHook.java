package com.racks.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * One land-protection plugin's opinion on whether a player may act on a rack at a location.
 *
 * <p>A rack has no vanilla block-break or block-place event of its own once placed — it is a handful
 * of display and interaction entities — so a protection plugin's ordinary block flags never see it.
 * Each check here maps the closest vanilla equivalent onto that plugin's own vocabulary for "this
 * player is trusted here": breaking a rack is treated the same as breaking a block, swapping its item
 * or turning it is treated the same as using a container, since neither destroys anything.
 *
 * <p>Implementations are only ever constructed once the corresponding plugin is confirmed enabled —
 * see {@link ProtectionHooks#detect} — so none of the classes an implementation references are
 * touched, or even loaded, when that plugin is absent.
 */
interface ProtectionHook {

    /** The plugin's name, for logging which hooks are active. */
    String name();

    /** Whether {@code player} may break the rack standing at {@code location}. */
    boolean canBreak(Player player, Location location);

    /** Whether {@code player} may swap an item on, or change the pose of, the rack at {@code location}. */
    boolean canInteract(Player player, Location location);
}
