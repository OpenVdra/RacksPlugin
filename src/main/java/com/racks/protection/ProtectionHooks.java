package com.racks.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Every {@link ProtectionHook} whose plugin is installed, combined: a player needs every
 * <b>enabled</b> hook's approval, so one denying region or claim is enough to deny the action.
 *
 * <p>Built once, in {@link #detect}, at plugin enable — which protection plugins are on the server
 * cannot change without a restart, so there is nothing to re-detect on {@code /racks reload}. Whether
 * an installed hook actually runs is a live, per-plugin setting instead ({@code protection.worldguard}
 * / {@code protection.griefprevention} in {@code PluginConfig}), so {@code enabled} is passed fresh on
 * every call rather than baked in here.
 */
public final class ProtectionHooks {

    private static final List<ProtectionHook> NONE = List.of();

    private final List<ProtectionHook> hooks;

    private ProtectionHooks(List<ProtectionHook> hooks) {
        this.hooks = hooks;
    }

    /**
     * Probes for WorldGuard and GriefPrevention and wraps whichever is actually enabled. Safe to call
     * even when neither is installed — {@link WorldGuardHook} and {@link GriefPreventionHook} are only
     * ever constructed, and therefore only ever loaded by the JVM, once this method has already
     * confirmed the matching plugin is present.
     */
    public static ProtectionHooks detect(PluginManager plugins, Logger logger) {
        List<ProtectionHook> found = new ArrayList<>(2);
        if (plugins.isPluginEnabled("WorldGuard")) {
            found.add(new WorldGuardHook());
        }
        if (plugins.isPluginEnabled("GriefPrevention")) {
            found.add(new GriefPreventionHook());
        }
        if (found.isEmpty()) {
            return new ProtectionHooks(NONE);
        }
        logger.info("Protection integration: found {}.", found.stream()
                .map(ProtectionHook::name).toList());
        return new ProtectionHooks(found);
    }

    /**
     * Whether every hook {@code enabled} accepts agrees {@code player} may break the rack at
     * {@code location}. {@code enabled} is tested against each hook's {@link ProtectionHook#name()}.
     */
    public boolean canBreak(Player player, Location location, Predicate<String> enabled) {
        for (ProtectionHook hook : hooks) {
            if (enabled.test(hook.name()) && !hook.canBreak(player, location)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether every hook {@code enabled} accepts agrees {@code player} may swap an item on, or change
     * the pose of, the rack at {@code location}. {@code enabled} is tested against each hook's
     * {@link ProtectionHook#name()}.
     */
    public boolean canInteract(Player player, Location location, Predicate<String> enabled) {
        for (ProtectionHook hook : hooks) {
            if (enabled.test(hook.name()) && !hook.canInteract(player, location)) {
                return false;
            }
        }
        return true;
    }
}
