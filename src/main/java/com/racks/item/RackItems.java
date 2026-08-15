package com.racks.item;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.racks.lang.LanguageManager;
import com.racks.model.RackVariant;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Builds and recognises the rack item.
 *
 * <p>It is the same object the data pack handed out: a {@code player_head} carrying the variant's
 * skin texture, the {@code pk_racks:<variant>_rack} custom-model-data string (so an existing Racks
 * resource pack still applies), a yellow name and a matching grey lore line — and, as in the data
 * pack, with the {@code equippable} component removed so the head cannot be worn as a hat.
 *
 * <h2>Recognising an item</h2>
 * Two ways, in order:
 * <ol>
 *   <li>The plugin's own persistent-data key. Everything this class builds carries it.</li>
 *   <li>The {@code custom_model_data} string. This is what makes rack items already sitting in
 *       players' inventories from the <b>data pack</b> keep working after the switch — the plugin
 *       cannot read the data pack's {@code pk_data} NBT, but the model string says the same thing
 *       and has been on every version of the item.</li>
 * </ol>
 *
 * <h2>Localization</h2>
 * An item's name is baked into the stack rather than re-rendered per viewer, so the caller passes
 * the locale of whoever is about to receive it — the player running {@code /racks give}, the player
 * at the crafting table. Where a rack is created with no viewer at all (a rack dropped on the floor
 * when one is broken) the configured fallback locale is used.
 */
public final class RackItems {

    private final Plugin plugin;
    private final LanguageManager lang;
    private final NamespacedKey variantKey;

    public RackItems(Plugin plugin, LanguageManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.variantKey = new NamespacedKey(plugin, "variant");
    }

    // ------------------------------------------------------------------------------------------------
    // Building
    // ------------------------------------------------------------------------------------------------

    /** One rack of {@code variant}, named for {@code locale}. */
    public ItemStack create(RackVariant variant, Locale locale) {
        return create(variant, locale, 1);
    }

    /** {@code count} racks of {@code variant}, named for {@code locale}. */
    @SuppressWarnings("UnstableApiUsage")
    public ItemStack create(RackVariant variant, Locale locale, int count) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, Math.max(1, count));

        Component variantName = lang.render(locale, "item.variant." + variant.id());
        Component name = lang.renderRich(locale, "item.name", "variant", variantName)
                .decoration(TextDecoration.ITALIC, false);
        Component lore = lang.renderRich(locale, "item.lore", "variant", variantName)
                .decoration(TextDecoration.ITALIC, false);

        item.editMeta(meta -> {
            meta.itemName(name);
            meta.lore(List.of(lore));

            CustomModelDataComponent model = meta.getCustomModelDataComponent();
            model.setStrings(List.of(variant.modelString()));
            meta.setCustomModelDataComponent(model);

            meta.getPersistentDataContainer().set(variantKey, PersistentDataType.STRING, variant.id());
        });

        // The head's skin, as a profile carrying nothing but the texture — no name and no UUID, which
        // is how the data pack wrote it and what it has to stay. A profile UUID is what the client
        // caches a resolved skin under, so reusing one across the twelve woods would have racks
        // showing each other's textures; giving each a made-up one instead would stop identical racks
        // from stacking. Properties alone avoid both.
        item.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile()
                .addProperty(new ProfileProperty("textures", variant.texture()))
                .build());

        // The data pack's "!minecraft:equippable": a player head is wearable by default, and a rack
        // is furniture, not a hat. unsetData removes the component outright rather than overriding it.
        item.unsetData(DataComponentTypes.EQUIPPABLE);

        return item;
    }

    // ------------------------------------------------------------------------------------------------
    // Recognition
    // ------------------------------------------------------------------------------------------------

    /** The rack variant {@code item} represents, or null if it is not a rack item at all. */
    public @Nullable RackVariant variantOf(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) {
            return null;
        }
        var meta = item.getItemMeta();

        String stored = meta.getPersistentDataContainer().get(variantKey, PersistentDataType.STRING);
        RackVariant variant = RackVariant.byId(stored);
        if (variant != null) return variant;

        return variantFromModelString(item);
    }

    /**
     * True when {@code item} is a rack that the <b>data pack</b> produced: recognisable by its model
     * string but without the plugin's own key. Those get re-stamped in place on pickup so they end up
     * identical to a plugin-made rack, which is the same thing the data pack's own
     * {@code items/rack/update} did whenever its item format changed.
     */
    public boolean isLegacyItem(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) {
            return false;
        }
        boolean claimed = item.getItemMeta().getPersistentDataContainer().has(variantKey, PersistentDataType.STRING);
        return !claimed && variantFromModelString(item) != null;
    }

    private @Nullable RackVariant variantFromModelString(ItemStack item) {
        CustomModelDataComponent model = item.getItemMeta().getCustomModelDataComponent();
        for (String s : model.getStrings()) {
            RackVariant variant = RackVariant.byModelString(s);
            if (variant != null) return variant;
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------------
    // Handing items out
    // ------------------------------------------------------------------------------------------------

    /**
     * Puts {@code item} in {@code player}'s inventory, dropping whatever does not fit at their feet.
     *
     * @return how many items had to be dropped
     */
    public int giveOrDrop(Player player, ItemStack item) {
        int dropped = 0;
        for (ItemStack leftover : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
            dropped += leftover.getAmount();
        }
        return dropped;
    }

    /** Key the plugin stamps rack items with. Exposed so recipes can be built against it in tests. */
    public NamespacedKey variantKey() {
        return variantKey;
    }

    public Plugin plugin() {
        return plugin;
    }
}
