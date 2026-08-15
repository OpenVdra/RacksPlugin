package com.racks.render;

import com.racks.item.PlaceableItems;
import com.racks.model.Rack;
import com.racks.model.RackFacing;
import com.racks.model.RackItemType;
import com.racks.model.RackPart;
import com.racks.model.RackType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Marker;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a rack out of real entities, and keeps them in step with its stored state.
 *
 * <p>Everything a player sees or clicks is a genuine, persistent entity, positioned and transformed
 * exactly as the data pack positioned and transformed it: a {@link Marker} anchor, the block
 * displays of the frame, the {@link Interaction} hitboxes, and one {@link ItemDisplay} per slot.
 * Keeping them real (rather than sending them as per-player packets) is what makes a plugin rack
 * indistinguishable from a data pack one — it survives restarts inside the chunk, and every other
 * plugin, command and admin tool on the server can see it.
 *
 * <h2>Threading</h2>
 * Every method here spawns, reads or removes entities and therefore must run on the thread that owns
 * the rack's block: the main thread on Paper, the owning region thread on Folia. The events that
 * drive them are already delivered there.
 */
public final class RackRenderer {

    /**
     * Search radius when collecting a rack's entities, matching the data pack's {@code distance=..2}.
     * Generous enough to cover every part (the furthest sits well under a block from the centre) and
     * tight enough that two adjacent racks never see each other's — and the rack id is checked on top
     * regardless.
     */
    private static final double SEARCH_RADIUS = 2.0;

    /** {@code interpolation_duration: 5} — the glide the data pack gave every item display. */
    private static final int INTERPOLATION_TICKS = 5;

    private final RackEntityKeys keys;
    private final PlaceableItems placeable;

    public RackRenderer(RackEntityKeys keys, PlaceableItems placeable) {
        this.keys = keys;
        this.placeable = placeable;
    }

    // ------------------------------------------------------------------------------------------------
    // Spawning
    // ------------------------------------------------------------------------------------------------

    /**
     * Creates every entity of {@code rack}, in the data pack's order: controller, interactions, body,
     * items.
     */
    public void spawn(Rack rack, World world) {
        spawnController(rack, world);
        spawnInteractions(rack, world);
        spawnBody(rack, world);
        spawnItemDisplays(rack, world);
    }

    private void spawnController(Rack rack, World world) {
        world.spawn(rack.key().center(world), Marker.class, marker -> {
            prepare(marker, rack, RackEntityKeys.Role.CONTROLLER, null);
            marker.addScoreboardTag(RackEntityKeys.TAG_CONTROLLER);
        });
    }

    private void spawnBody(Rack rack, World world) {
        Location base = rack.key().center(world);
        base.setYaw(rack.facing().yaw());
        base.setPitch(0f);

        for (RackTransforms.BodyPart part : RackTransforms.body(rack.type())) {
            world.spawn(base, BlockDisplay.class, display -> {
                prepare(display, rack, RackEntityKeys.Role.BODY, null);
                display.addScoreboardTag(RackEntityKeys.TAG_BODY);
                display.addScoreboardTag(RackEntityKeys.TAG_BODY + "." + part.id());
                display.setBlock((part.button() ? rack.variant().button() : rack.variant().fence()).createBlockData());
                display.setTransformation(part.transformation());
            });
        }
    }

    private void spawnItemDisplays(Rack rack, World world) {
        Location base = rack.key().center(world);
        base.setYaw(rack.facing().yaw());
        base.setPitch(0f);

        // A wall rack has a single slot; a ground rack has both. Same split the data pack made
        // between its wall and ground item creators.
        List<RackPart> parts = rack.type().isWall()
                ? List.of(RackPart.LEFT)
                : List.of(RackPart.LEFT, RackPart.RIGHT);

        for (RackPart part : parts) {
            world.spawn(base, ItemDisplay.class, display -> {
                prepare(display, rack, RackEntityKeys.Role.ITEM, part.name().toLowerCase(java.util.Locale.ROOT));
                display.addScoreboardTag(RackEntityKeys.TAG_ITEM);
                display.addScoreboardTag(part.tag());
                display.setInterpolationDuration(INTERPOLATION_TICKS);
                applyItem(display, rack, part);
            });
        }
    }

    private void spawnInteractions(Rack rack, World world) {
        Location base = rack.key().floorCenter(world);
        for (InteractionSpec spec : interactionSpecs(rack.type())) {
            Location at = base.clone().add(spec.offsetX(rack.facing()), spec.dy(), spec.offsetZ(rack.facing()));
            world.spawn(at, Interaction.class, interaction -> {
                prepare(interaction, rack, RackEntityKeys.Role.INTERACTION,
                        spec.part().name().toLowerCase(java.util.Locale.ROOT));
                interaction.addScoreboardTag(RackEntityKeys.TAG_INTERACTION);
                interaction.addScoreboardTag(RackEntityKeys.TAG_INTERACTION + "." + spec.id());
                interaction.addScoreboardTag(spec.part().tag());
                interaction.setInteractionWidth(spec.width());
                interaction.setInteractionHeight(spec.height());
                interaction.setResponsive(true);
            });
        }
    }

    /** Tags, persistent data and the flags every rack entity shares. */
    private void prepare(Entity entity, Rack rack, RackEntityKeys.Role role, @Nullable String slot) {
        keys.stamp(entity, rack.id(), role, slot);
        entity.addScoreboardTag(RackEntityKeys.TAG_CUSTOM_BLOCK);
        entity.addScoreboardTag(RackEntityKeys.TAG_RACKS);
        entity.addScoreboardTag(RackEntityKeys.TAG_BLOCK);
        entity.addScoreboardTag(RackEntityKeys.TAG_RACK);
        entity.addScoreboardTag(rack.type().tag());
        entity.setPersistent(true);
    }

    // ------------------------------------------------------------------------------------------------
    // Updating
    // ------------------------------------------------------------------------------------------------

    /**
     * Re-applies one slot's item and pose to its display. Used after a swap, where only the slot the
     * player touched has changed.
     */
    public void refreshItem(Rack rack, RackPart part, World world) {
        ItemDisplay display = findItemDisplay(rack, part, world);
        if (display != null) {
            applyItem(display, rack, part);
        }
    }

    /**
     * Re-applies both slots after a pose change, restarting the interpolation so the items visibly
     * turn rather than snap — the data pack's {@code start_interpolation: -1}, which means "begin
     * from this tick".
     */
    public void refreshPose(Rack rack, World world) {
        for (Entity entity : entitiesOf(rack, world)) {
            if (entity instanceof ItemDisplay display && keys.roleOf(entity) == RackEntityKeys.Role.ITEM) {
                RackPart part = partOf(entity);
                if (part == null) continue;
                display.setInterpolationDelay(-1);
                applyItem(display, rack, part);
            }
        }
    }

    /**
     * Sets what a display holds and how it is posed.
     *
     * <p>Mirrors {@code items/update/_run}: clear the item, reset the display mode, then look up the
     * transform for (rack type, slot, item type) at the current pose. When there is no table — an
     * empty slot, or an item the data pack never wrote a pose for — the transform is deliberately
     * left as it was, exactly as the data pack's dispatch to a missing function left it. Nothing is
     * shown either way, since the display holds no item.
     */
    private void applyItem(ItemDisplay display, Rack rack, RackPart part) {
        ItemStack item = rack.item(part);
        display.setItemStack(item);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);

        RackItemType itemType = placeable.typeOf(item);
        RackTransforms.ItemTable table = RackTransforms.item(rack.type(), part, itemType);
        if (table == null) {
            return;
        }
        display.setItemDisplayTransform(table.mode());
        display.setTransformation(table.transformation(rack.pose()));
    }

    // ------------------------------------------------------------------------------------------------
    // Lookups and removal
    // ------------------------------------------------------------------------------------------------

    /** Every entity belonging to {@code rack} that is currently loaded. */
    public List<Entity> entitiesOf(Rack rack, World world) {
        Location center = rack.key().center(world);
        List<Entity> found = new ArrayList<>(12);
        for (Entity entity : world.getNearbyEntities(center, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS,
                e -> keys.belongsTo(e, rack.id()))) {
            found.add(entity);
        }
        return found;
    }

    /** The item display for one slot, or null when the chunk's entities are not loaded. */
    public @Nullable ItemDisplay findItemDisplay(Rack rack, RackPart part, World world) {
        for (Entity entity : entitiesOf(rack, world)) {
            if (entity instanceof ItemDisplay display && part == partOf(entity)) {
                return display;
            }
        }
        return null;
    }

    /** True when the rack's controller marker is present — i.e. the rack really exists in the world. */
    public boolean hasController(Rack rack, World world) {
        for (Entity entity : entitiesOf(rack, world)) {
            if (keys.roleOf(entity) == RackEntityKeys.Role.CONTROLLER) {
                return true;
            }
        }
        return false;
    }

    /** Removes every entity of {@code rack}. */
    public void despawn(Rack rack, World world) {
        for (Entity entity : entitiesOf(rack, world)) {
            entity.remove();
        }
    }

    private @Nullable RackPart partOf(Entity entity) {
        String slot = keys.slotOf(entity);
        if (slot == null) return null;
        return slot.equals("right") ? RackPart.RIGHT : RackPart.LEFT;
    }

    // ------------------------------------------------------------------------------------------------
    // Interaction hitboxes
    // ------------------------------------------------------------------------------------------------

    /**
     * One clickable box, from {@code entities/interactions/create/<type>/initialize_<n>}.
     *
     * <p>The offsets are stored per facing because the data pack wrote them that way: an interaction
     * entity has no rotation of its own, so where its box sits has to be worked out from the rack's
     * facing rather than inherited from it. Index order is north, east, south, west.
     */
    private record InteractionSpec(int id, RackPart part, float width, float height, double dy,
                                   double[] dx, double[] dz) {

        double offsetX(RackFacing facing) {
            return dx[facing.ordinal()];
        }

        double offsetZ(RackFacing facing) {
            return dz[facing.ordinal()];
        }
    }

    private static final InteractionSpec[] GROUND_INTERACTIONS = {
            // 1 — the whole block, taking the left slot. Sunk a thousandth of a block so it loses ties
            // with the two right-hand boxes stacked in front of it.
            new InteractionSpec(1, RackPart.LEFT, 1.001f, 1.002f, -0.001,
                    new double[]{0, 0, 0, 0}, new double[]{0, 0, 0, 0}),
            // 2 and 3 — two half-width boxes covering the right slot, one on each side of the rack.
            new InteractionSpec(2, RackPart.RIGHT, 0.5005f, 1.004f, -0.002,
                    new double[]{-0.2505, 0.2505, 0.2505, -0.2505},
                    new double[]{-0.2505, -0.2505, 0.2505, 0.2505}),
            new InteractionSpec(3, RackPart.RIGHT, 0.5005f, 1.004f, -0.002,
                    new double[]{-0.2505, -0.2505, 0.2505, 0.2505},
                    new double[]{0.2505, -0.2505, -0.2505, 0.2505}),
    };

    private static final InteractionSpec[] WALL_INTERACTIONS = {
            // Both boxes drive the single left slot; they sit side by side along the wall.
            new InteractionSpec(1, RackPart.LEFT, 0.5f, 0.5f, 0.075,
                    new double[]{0.25, -0.375, -0.25, 0.375},
                    new double[]{0.375, 0.25, -0.375, -0.25}),
            new InteractionSpec(2, RackPart.LEFT, 0.5f, 0.5f, 0.075,
                    new double[]{-0.25, -0.375, 0.25, 0.375},
                    new double[]{0.375, -0.25, -0.375, 0.25}),
    };

    private static InteractionSpec[] interactionSpecs(RackType type) {
        return type.isWall() ? WALL_INTERACTIONS : GROUND_INTERACTIONS;
    }
}
