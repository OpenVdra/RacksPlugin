package com.racks.listener;

import com.racks.item.RackItems;
import com.racks.model.RackVariant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Brings rack items made by the data pack up to the plugin's own form.
 *
 * <p>Purely cosmetic, and deliberately so — a data pack rack item already works everywhere in this
 * plugin, because it is recognised by the {@code custom_model_data} string that has been on every
 * version of it. What re-stamping adds is the plugin's own identifying data and, more visibly, a
 * name in the player's language instead of the data pack's fixed English.
 *
 * <p>The data pack did the same thing, from an {@code inventory_changed} advancement that re-fired
 * on every inventory change. Here it is once per join: the items being upgraded were minted before
 * the plugin existed, so a player either has them when they log in or receives a current one later.
 * The stack size is carried across untouched, so nothing is created or lost.
 */
public final class RackItemUpgradeListener implements Listener {

    private final RackItems items;

    public RackItemUpgradeListener(RackItems items) {
        this.items = items;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack[] contents = player.getInventory().getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (!items.isLegacyItem(stack)) {
                continue;
            }
            RackVariant variant = items.variantOf(stack);
            if (variant == null) {
                continue;
            }
            player.getInventory().setItem(slot, items.create(variant, player.locale(), stack.getAmount()));
        }
    }
}
