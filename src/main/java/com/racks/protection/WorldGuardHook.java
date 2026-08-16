package com.racks.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Queries WorldGuard region flags: {@code block-break} for breaking a rack, {@code interact} for
 * swapping its item or turning it — the same two flags WorldGuard already applies to a real block's
 * break and its right-click.
 *
 * <p>{@link RegionQuery} does not consider a player's region-bypass permission on its own — that is
 * WorldGuard's own documented behaviour — so {@code worldguard.region.bypass} is checked here first.
 */
final class WorldGuardHook implements ProtectionHook {

    @Override
    public String name() {
        return "WorldGuard";
    }

    @Override
    public boolean canBreak(Player player, Location location) {
        return canPass(player, location, Flags.BLOCK_BREAK);
    }

    @Override
    public boolean canInteract(Player player, Location location) {
        return canPass(player, location, Flags.INTERACT);
    }

    private static boolean canPass(Player player, Location location, StateFlag flag) {
        if (bypasses(player)) {
            return true;
        }
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        return query.testBuild(BukkitAdapter.adapt(location), localPlayer, flag);
    }

    private static boolean bypasses(Player player) {
        return player.hasPermission("worldguard.region.bypass." + player.getWorld().getName())
                || player.hasPermission("worldguard.region.bypass");
    }
}
