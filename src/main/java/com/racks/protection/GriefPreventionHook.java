package com.racks.protection;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Queries GriefPrevention claims via {@link Claim#checkPermission}: {@link ClaimPermission#Build} for
 * breaking a rack (the same trust a block break asks for), {@link ClaimPermission#Inventory} for
 * swapping its item or turning it — the same trust a chest asks for, since neither action destroys
 * the rack. {@code Claim}'s older per-action {@code allowBreak}/{@code allowContainers} methods are
 * deprecated in favour of this one; there is no Bukkit event to pass for either action, so {@code
 * null} is passed where the API accepts one.
 *
 * <p>{@code checkPermission} already accounts for the claim's owner, trusted players, admin claims and
 * a player's own {@code /ignoreclaims} toggle — there is nothing extra to bypass here, unlike
 * WorldGuard.
 */
final class GriefPreventionHook implements ProtectionHook {

    @Override
    public String name() {
        return "GriefPrevention";
    }

    @Override
    public boolean canBreak(Player player, Location location) {
        return isGranted(player, location, ClaimPermission.Build);
    }

    @Override
    public boolean canInteract(Player player, Location location) {
        return isGranted(player, location, ClaimPermission.Inventory);
    }

    private static boolean isGranted(Player player, Location location, ClaimPermission permission) {
        Claim claim = claimAt(location);
        return claim == null || claim.checkPermission(player, permission, null) == null;
    }

    /** Null outside any claim, where every action is allowed. */
    private static Claim claimAt(Location location) {
        return GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
    }
}
