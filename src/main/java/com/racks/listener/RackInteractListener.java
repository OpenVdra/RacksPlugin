package com.racks.listener;

import com.racks.config.PluginConfig;
import com.racks.model.Rack;
import com.racks.model.RackPart;
import com.racks.protection.ProtectionHooks;
import com.racks.render.RackEntityKeys;
import com.racks.service.RackService;
import com.racks.storage.RackRepository;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The three things a player can do to a rack, from the two events that reach its hitboxes.
 *
 * <p>Right-click swaps the item; sneak and right-click turns it; left-click breaks it. That is the
 * data pack's {@code player_interacted_with_entity} and {@code player_hurt_entity} advancement pair,
 * mapped onto the events Bukkit already delivers — on the thread that owns the rack, which is what
 * lets {@link RackService} work without locks.
 *
 * <p>Placing a rack is a real {@code BlockPlaceEvent}, so WorldGuard/GriefPrevention already protect
 * it on their own. None of the three actions here are — a rack's hitboxes are {@link Interaction}
 * entities, invisible to a protection plugin's ordinary block flags — so {@link ProtectionHooks} is
 * asked explicitly, right after the {@code racks.use} check and before either service call.
 */
public final class RackInteractListener implements Listener {

    private final RackService service;
    private final RackRepository repository;
    private final RackEntityKeys keys;
    private final Supplier<PluginConfig> config;
    private final Supplier<ProtectionHooks> protection;

    public RackInteractListener(RackService service, RackRepository repository, RackEntityKeys keys,
                                Supplier<PluginConfig> config, Supplier<ProtectionHooks> protection) {
        this.service = service;
        this.repository = repository;
        this.keys = keys;
        this.config = config;
        this.protection = protection;
    }

    // ------------------------------------------------------------------------------------------------
    // Right-click: swap an item, or turn the rack when sneaking
    // ------------------------------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEntityEvent event) {
        // A right-click on an entity arrives twice, as PlayerInteractAtEntityEvent (which extends
        // this one) and then as the plain event. Ignoring the subclass leaves exactly one handling,
        // rather than swapping the same item twice.
        if (event instanceof PlayerInteractAtEntityEvent || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Rack rack = rackOf(event.getRightClicked());
        if (rack == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("racks.use") || !canInteract(player, rack)) {
            return;
        }
        if (player.isSneaking()) {
            service.cyclePose(rack, player.getWorld());
        } else {
            RackPart part = partOf(event.getRightClicked());
            service.swapItem(player, rack, part, player.getWorld());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Left-click: break the rack
    // ------------------------------------------------------------------------------------------------

    /**
     * The attack path. Paper raises this for every attack, including on entities that take no damage
     * — which an {@link Interaction} is — so it is the reliable half of the pair.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAttack(PrePlayerAttackEntityEvent event) {
        Rack rack = rackOf(event.getAttacked());
        if (rack == null) {
            return;
        }
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("racks.use") || !canBreak(player, rack)) {
            return;
        }
        service.breakRack(rack, player.getWorld(), player);
    }

    /**
     * Backstop for anything that damages a rack's hitbox without going through the attack event. It
     * is safe for both to fire: the first one to run takes the rack out of the index, so the second
     * finds nothing and does nothing.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Rack rack = rackOf(event.getEntity());
        if (rack == null) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getDamager() instanceof Player player) || !player.hasPermission("racks.use")
                || !canBreak(player, rack)) {
            return;
        }
        service.breakRack(rack, player.getWorld(), player);
    }

    // ------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------

    private boolean canBreak(Player player, Rack rack) {
        return protection.get().canBreak(player, rack.key().toLocation(player.getWorld()), this::isHookEnabled);
    }

    /** @see #canBreak */
    private boolean canInteract(Player player, Rack rack) {
        return protection.get().canInteract(player, rack.key().toLocation(player.getWorld()), this::isHookEnabled);
    }

    /**
     * {@code protection.worldguard}/{@code protection.griefprevention}: each protection plugin's hook
     * can be turned off without touching the other, independently of whether it is installed at all.
     */
    private boolean isHookEnabled(String hookName) {
        PluginConfig cfg = config.get();
        return switch (hookName) {
            case "WorldGuard" -> cfg.isWorldGuardIntegrationEnabled();
            case "GriefPrevention" -> cfg.isGriefPreventionIntegrationEnabled();
            default -> true;
        };
    }

    /** The rack an interaction hitbox belongs to, or null when the entity is not one of ours. */
    private @Nullable Rack rackOf(Entity entity) {
        if (!(entity instanceof Interaction)) {
            return null;
        }
        Integer id = keys.rackIdOf(entity);
        return id == null ? null : repository.byId(id);
    }

    /**
     * Which slot a hitbox drives. A ground rack's full-block box takes the left slot and its two
     * half-width boxes take the right; a wall rack's two boxes both take its single slot.
     */
    private RackPart partOf(Entity entity) {
        return "right".equals(keys.slotOf(entity)) ? RackPart.RIGHT : RackPart.LEFT;
    }
}
