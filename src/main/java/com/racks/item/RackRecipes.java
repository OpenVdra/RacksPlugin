package com.racks.item;

import com.racks.lang.LanguageManager;
import com.racks.model.RackVariant;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * The twelve crafting recipes: three planks over two sticks, one rack per craft, exactly the shape
 * the data pack used.
 *
 * <p>A registered recipe carries one fixed result, so the recipe itself is built in the server's
 * configured language. Since a name is baked into an item rather than resolved per viewer, the result
 * is re-made in the crafter's own language while they stand at the table — which is what lets a
 * Vietnamese player and an English player at the same server craft the same rack and each read it in
 * their own language.
 */
public final class RackRecipes implements Listener {

    private final Plugin plugin;
    private final RackItems items;
    private final LanguageManager lang;
    private final List<NamespacedKey> registered = new ArrayList<>(RackVariant.values().length);

    public RackRecipes(Plugin plugin, RackItems items, LanguageManager lang) {
        this.plugin = plugin;
        this.items = items;
        this.lang = lang;
    }

    public void register() {
        for (RackVariant variant : RackVariant.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "rack_" + variant.id());
            ShapedRecipe recipe = new ShapedRecipe(key, items.create(variant, lang.fallbackLocale()));
            recipe.shape("PPP", "S S");
            recipe.setIngredient('P', variant.planks());
            recipe.setIngredient('S', Material.STICK);

            if (Bukkit.addRecipe(recipe)) {
                registered.add(key);
            }
        }
    }

    /** Removes the recipes again, so a reload or a disable does not leave stale ones behind. */
    public void unregister() {
        for (NamespacedKey key : registered) {
            Bukkit.removeRecipe(key);
        }
        registered.clear();
    }

    /** Re-makes the result in the crafting player's language. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof Keyed keyed) || !registered.contains(keyed.getKey())) {
            return;
        }
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        if (result == null) {
            return;
        }
        RackVariant variant = items.variantOf(result);
        if (variant == null) {
            return;
        }
        inventory.setResult(items.create(variant, player.locale(), result.getAmount()));
    }
}
