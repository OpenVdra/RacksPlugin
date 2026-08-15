package com.racks.migration;

import com.racks.model.Rack;
import com.racks.model.RackFacing;
import com.racks.model.RackItemType;
import com.racks.model.RackKey;
import com.racks.model.RackPart;
import com.racks.model.RackType;
import com.racks.model.RackVariant;
import com.racks.item.PlaceableItems;
import com.racks.render.RackEntityKeys;
import com.racks.render.RackRenderer;
import com.racks.render.RackTransforms;
import com.racks.storage.RackRepository;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Imports racks the original data pack left behind.
 *
 * <p>A server switching from the data pack to this plugin has racks already standing in its world,
 * full of players' gear. The plugin cannot read where the data pack recorded them — that lived in
 * command storage, which has no API — but it does not need to: everything about a rack is written
 * into the entities that make it up. The wood is the block its frame displays, the direction is the
 * yaw those displays were turned to, the contents are simply what the item displays are holding, and
 * even the pose can be read back by matching the item display's transform against the same tables
 * the data pack applied.
 *
 * <p>So an adopted rack is rebuilt rather than re-labelled: the data pack's entities are read,
 * removed, and replaced with a plugin-owned rack carrying the same items in the same pose. Rebuilding
 * is what guarantees the result is structurally identical to a rack this plugin placed itself,
 * instead of inheriting whatever state a years-old rack happened to be in.
 *
 * <p>Two things cannot be recovered and are set to sensible values: the original owner (recorded by
 * the data pack but never used by it) becomes unset, and the creation time becomes 0, which means
 * "old enough to drop" — exactly how the data pack itself treated racks placed before its 3.0.0.
 */
public final class DatapackAdopter {

    private final RackRepository repository;
    private final RackRenderer renderer;
    private final RackEntityKeys keys;
    private final PlaceableItems placeable;
    private final Logger logger;

    public DatapackAdopter(RackRepository repository, RackRenderer renderer, RackEntityKeys keys,
                           PlaceableItems placeable, Logger logger) {
        this.repository = repository;
        this.renderer = renderer;
        this.keys = keys;
        this.placeable = placeable;
        this.logger = logger;
    }

    /**
     * Adopts the rack anchored at {@code controller}.
     *
     * <p>Must run on the thread that owns the block. {@code chunkEntities} is the entity list the
     * caller already has (from the chunk-load event), so grouping does not cost another world query.
     *
     * @return true if a rack was imported
     */
    public boolean adopt(Entity controller, List<Entity> chunkEntities, World world) {
        Block block = controller.getLocation().getBlock();
        RackKey key = RackKey.of(block);

        if (repository.contains(key)) {
            logger.warn("Found a leftover data pack rack at {} {},{},{} where a plugin rack already "
                            + "stands — leaving it alone. Remove it by hand if it is a duplicate.",
                    world.getName(), key.x(), key.y(), key.z());
            return false;
        }

        List<Entity> parts = partsOf(key, chunkEntities);
        RackType type = controller.getScoreboardTags().contains(RackType.WALL.tag())
                ? RackType.WALL
                : RackType.GROUND;

        RackVariant variant = readVariant(parts, type);
        if (variant == null) {
            logger.warn("Could not work out the wood of the data pack rack at {} {},{},{} — skipping it",
                    world.getName(), key.x(), key.y(), key.z());
            return false;
        }
        RackFacing facing = readFacing(parts);

        ItemStack left = readItem(parts, RackPart.LEFT);
        ItemStack right = type.isWall() ? null : readItem(parts, RackPart.RIGHT);
        short pose = readPose(parts, type, left, right);

        // Take the old rack apart only once everything worth keeping has been read off it.
        for (Entity part : parts) {
            part.remove();
        }

        Rack rack = new Rack(repository.nextId(), key, variant, type, facing,
                null, 0L, pose, left, right);
        if (!repository.add(rack)) {
            logger.warn("Could not register the adopted rack at {} {},{},{}",
                    world.getName(), key.x(), key.y(), key.z());
            return false;
        }

        block.setType(type.isWall() ? Material.AIR : Material.BARRIER, false);
        renderer.spawn(rack, world);
        return true;
    }

    // ------------------------------------------------------------------------------------------------
    // Reading a rack off its entities
    // ------------------------------------------------------------------------------------------------

    /**
     * The data pack's entities that belong to the rack occupying {@code key}.
     *
     * <p>Grouped by block position rather than by distance, which matters: a rack's furthest part
     * sits three quarters of a block from its centre, so two racks side by side are close enough that
     * a radius search would mix them up. Every part of a rack — including the interaction boxes,
     * which are sunk a thousandth of a block so their hitboxes resolve in the right order — lands
     * inside its own rack's block once that thousandth is added back.
     */
    private List<Entity> partsOf(RackKey key, List<Entity> chunkEntities) {
        List<Entity> parts = new ArrayList<>(12);
        for (Entity entity : chunkEntities) {
            if (!entity.getScoreboardTags().contains(RackEntityKeys.TAG_RACK)) continue;
            if (keys.rackIdOf(entity) != null) continue; // already plugin-owned

            var loc = entity.getLocation();
            int x = loc.getBlockX();
            int y = (int) Math.floor(loc.getY() + 0.01);
            int z = loc.getBlockZ();
            if (x == key.x() && y == key.y() && z == key.z()) {
                parts.add(entity);
            }
        }
        return parts;
    }

    /**
     * The wood, read off the frame. A ground rack's parts are all fences; a wall rack's first two are
     * buttons and its last two fences, so fences are tried first and buttons are the fallback.
     */
    private @Nullable RackVariant readVariant(List<Entity> parts, RackType type) {
        RackVariant fromButton = null;
        for (Entity entity : parts) {
            if (!(entity instanceof BlockDisplay display)) continue;
            BlockData data = display.getBlock();
            RackVariant fromFence = RackVariant.byFence(data.getMaterial());
            if (fromFence != null) {
                return fromFence;
            }
            if (fromButton == null) {
                fromButton = byButton(data.getMaterial());
            }
        }
        return fromButton;
    }

    private static @Nullable RackVariant byButton(Material material) {
        for (RackVariant variant : RackVariant.values()) {
            if (variant.button() == material) return variant;
        }
        return null;
    }

    /**
     * The direction, read off any display's yaw — which is exactly where the data pack put it, with
     * {@code tp @s ~ ~ ~ <y_rotation> 0}. Yaw comes back normalised to (−180, 180], so west reads as
     * −90 rather than the 270 that was written.
     */
    private RackFacing readFacing(List<Entity> parts) {
        for (Entity entity : parts) {
            if (!(entity instanceof BlockDisplay) && !(entity instanceof ItemDisplay)) continue;
            float yaw = entity.getLocation().getYaw();
            int normalized = Math.floorMod(Math.round(yaw), 360);
            return switch (normalized) {
                case 90 -> RackFacing.EAST;
                case 180 -> RackFacing.SOUTH;
                case 270 -> RackFacing.WEST;
                default -> RackFacing.NORTH;
            };
        }
        return RackFacing.NORTH;
    }

    /** What a slot is holding, straight off its item display. */
    private @Nullable ItemStack readItem(List<Entity> parts, RackPart part) {
        for (Entity entity : parts) {
            if (!(entity instanceof ItemDisplay display)) continue;
            if (!entity.getScoreboardTags().contains(part.tag())) continue;
            ItemStack item = display.getItemStack();
            return item == null || item.getType().isAir() ? null : item.clone();
        }
        return null;
    }

    /**
     * The pose, recovered by matching a held item's transform against the pose tables.
     *
     * <p>The pose was the one field kept only in the data pack's own storage. Rather than resetting
     * every adopted rack to its default arrangement — which players would see as their racks all
     * quietly rearranging themselves — it is read back out of the display that is wearing it. An
     * empty rack has no pose to recover and none that would be visible, so it starts at 0.
     */
    private short readPose(List<Entity> parts, RackType type,
                           @Nullable ItemStack left, @Nullable ItemStack right) {
        RackPart source = left != null ? RackPart.LEFT : (right != null ? RackPart.RIGHT : null);
        if (source == null) {
            return 0;
        }
        RackItemType itemType = placeable.typeOf(source == RackPart.LEFT ? left : right);
        for (Entity entity : parts) {
            if (!(entity instanceof ItemDisplay display)) continue;
            if (!entity.getScoreboardTags().contains(source.tag())) continue;
            return RackTransforms.inferPose(type, source, itemType, display.getTransformation());
        }
        return 0;
    }
}
