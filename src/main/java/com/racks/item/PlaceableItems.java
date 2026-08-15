package com.racks.item;

import com.racks.model.RackItemType;
import com.racks.model.RackType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves, once at startup, which items may go on a rack and how each is posed.
 *
 * <p>The data pack answered both questions per interaction with {@code if items entity @s ...
 * #minecraft:axes} chains and the {@code #pk_racks:ground_rack_placeable} /
 * {@code #pk_racks:wall_rack_placeable} item tags. Here the vanilla tags are expanded a single time
 * into {@link EnumSet}/{@link EnumMap} lookups, so the interaction path is a couple of array reads
 * with no allocation and no tag walking.
 *
 * <p>Vanilla tags are looked up by key rather than through the {@code Tag} constants, because the
 * set is version-dependent ({@code #minecraft:spears} only exists on recent releases) and a missing
 * tag should quietly contribute nothing instead of failing the plugin to start.
 */
public final class PlaceableItems {

    /**
     * Item-type classification in the exact order of {@code items/update/define_item_type}: tags
     * first, then the individual items. {@code putIfAbsent} preserves "first match wins" for any
     * material that appears in more than one of these.
     */
    private static final List<Classification> CLASSIFICATIONS = List.of(
            Classification.tag("axes", RackItemType.AXE),
            Classification.tag("hoes", RackItemType.HOE),
            Classification.tag("pickaxes", RackItemType.PICKAXE),
            Classification.tag("shovels", RackItemType.SHOVEL),
            Classification.tag("spears", RackItemType.SPEAR),
            Classification.tag("swords", RackItemType.SWORD),
            Classification.item("bow", RackItemType.BOW),
            Classification.item("carrot_on_a_stick", RackItemType.CARROT_ON_A_STICK),
            Classification.item("crossbow", RackItemType.CROSSBOW),
            Classification.item("fishing_rod", RackItemType.FISHING_ROD),
            Classification.item("shears", RackItemType.SHEARS),
            Classification.item("shield", RackItemType.SHIELD),
            Classification.item("spyglass", RackItemType.SPYGLASS),
            Classification.item("trident", RackItemType.TRIDENT),
            Classification.item("mace", RackItemType.MACE),
            Classification.item("warped_fungus_on_a_stick", RackItemType.WARPED_FUNGUS_ON_A_STICK));

    /** {@code #pk_racks:ground_rack_placeable}. */
    private static final List<String> GROUND_TAGS = List.of("axes", "hoes", "pickaxes", "shovels", "spears", "swords");
    private static final List<String> GROUND_ITEMS =
            List.of("carrot_on_a_stick", "fishing_rod", "mace", "warped_fungus_on_a_stick");

    /** {@code #pk_racks:wall_rack_placeable} — the ground list plus the six ranged/utility items. */
    private static final List<String> WALL_ITEMS = List.of("bow", "carrot_on_a_stick", "crossbow", "fishing_rod",
            "mace", "shears", "shield", "spyglass", "trident", "warped_fungus_on_a_stick");

    private final Map<Material, RackItemType> types = new EnumMap<>(Material.class);
    private final Set<Material> groundPlaceable = EnumSet.noneOf(Material.class);
    private final Set<Material> wallPlaceable = EnumSet.noneOf(Material.class);

    public PlaceableItems(Logger logger) {
        for (Classification c : CLASSIFICATIONS) {
            for (Material material : c.resolve(logger)) {
                types.putIfAbsent(material, c.type());
            }
        }
        for (String tag : GROUND_TAGS) {
            List<Material> resolved = itemTag(tag, logger);
            groundPlaceable.addAll(resolved);
            wallPlaceable.addAll(resolved);
        }
        for (String item : GROUND_ITEMS) {
            material(item, logger).ifPresent(groundPlaceable::add);
        }
        for (String item : WALL_ITEMS) {
            material(item, logger).ifPresent(wallPlaceable::add);
        }
    }

    /** How {@code item} should be posed. Never null; an unknown or empty item maps to {@code NONE}. */
    public RackItemType typeOf(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return RackItemType.NONE;
        return types.getOrDefault(item.getType(), RackItemType.NONE);
    }

    /**
     * Whether {@code item} may be put on a rack of this type. Mirrors the data pack's
     * {@code define_rack_new_item}: an empty hand is always allowed (it means "take"), anything
     * outside the whitelist means the whole interaction is dropped without any state change.
     */
    public boolean isPlaceable(RackType rackType, @Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) return true;
        Set<Material> allowed = rackType.isWall() ? wallPlaceable : groundPlaceable;
        return allowed.contains(item.getType());
    }

    private static List<Material> itemTag(String name, Logger logger) {
        Tag<Material> tag = org.bukkit.Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(name), Material.class);
        if (tag == null) {
            logger.debug("Vanilla item tag #minecraft:{} is not present on this server version — skipping", name);
            return List.of();
        }
        return new ArrayList<>(tag.getValues());
    }

    private static java.util.Optional<Material> material(String name, Logger logger) {
        Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(name));
        if (material == null) {
            logger.debug("Item minecraft:{} does not exist on this server version — skipping", name);
        }
        return java.util.Optional.ofNullable(material);
    }

    /** One line of {@code define_item_type}: either a vanilla item tag or a single item. */
    private record Classification(@Nullable String tag, @Nullable String item, RackItemType type) {

        static Classification tag(String tag, RackItemType type) {
            return new Classification(tag, null, type);
        }

        static Classification item(String item, RackItemType type) {
            return new Classification(null, item, type);
        }

        List<Material> resolve(Logger logger) {
            if (tag != null) return itemTag(tag, logger);
            return material(item, logger).map(List::of).orElse(List.of());
        }
    }
}
