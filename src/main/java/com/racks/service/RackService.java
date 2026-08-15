package com.racks.service;

import com.racks.config.PluginConfig;
import com.racks.item.PlaceableItems;
import com.racks.item.RackItems;
import com.racks.lang.LanguageManager;
import com.racks.model.Rack;
import com.racks.model.RackFacing;
import com.racks.model.RackKey;
import com.racks.model.RackPart;
import com.racks.model.RackType;
import com.racks.model.RackVariant;
import com.racks.render.RackRenderer;
import com.racks.storage.RackRepository;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Everything a rack can do: get placed, hold an item, turn it, get broken.
 *
 * <p>This is the data pack's {@code blocks/rack/actions/*} tree, in one place. Each method
 * corresponds to one action function and keeps its decisions, its ordering and its early exits —
 * including the ones that look like details but are not, such as a swap being abandoned whole when
 * the player is holding something a rack will not take, or stored items dropping even for a creative
 * breaker while the rack itself does not.
 *
 * <h2>Threading</h2>
 * Every method must be called from the thread that owns the rack's block. Bukkit already delivers
 * the place, interact and damage events there, on Paper and on Folia alike, so no hand-off happens
 * here — and because one rack is therefore only ever touched by one thread, its item slots need no
 * lock and cannot interleave.
 */
public final class RackService {

    /**
     * Read through a supplier rather than held directly, so {@code /racks reload} can swap the whole
     * config object without re-wiring any of the services or listeners that point at this one.
     */
    private final Supplier<PluginConfig> config;
    private final RackRepository repository;
    private final RackRenderer renderer;
    private final RackItems items;
    private final PlaceableItems placeable;
    private final LanguageManager lang;

    public RackService(Supplier<PluginConfig> config, RackRepository repository, RackRenderer renderer,
                       RackItems items, PlaceableItems placeable, LanguageManager lang) {
        this.config = config;
        this.repository = repository;
        this.renderer = renderer;
        this.items = items;
        this.placeable = placeable;
        this.lang = lang;
    }

    // ------------------------------------------------------------------------------------------------
    // Placing
    // ------------------------------------------------------------------------------------------------

    /**
     * Creates a rack at {@code block}.
     *
     * <p>Follows {@code actions/create/from_placeholder} then {@code actions/place/_run}: build the
     * data, put the block down, then the entities. A ground rack stands on a barrier — invisible,
     * unbreakable in survival, and solid, which is what stops a player walking through their own
     * rack; a wall rack occupies air and hangs off the block behind it.
     *
     * @param facing for a wall rack, the direction the head was placed facing; for a ground rack this
     *               is ignored and the rack is turned to face whoever placed it
     * @return the new rack, or null if that block already holds one
     */
    public @Nullable Rack place(Block block, RackVariant variant, RackType type, RackFacing facing,
                                @Nullable Player placer) {
        RackKey key = RackKey.of(block);
        if (repository.contains(key)) {
            return null;
        }

        RackFacing resolved = type.isWall()
                ? facing
                // The data pack derived a ground rack's facing from the placer's yaw and then
                // inverted it, so a rack you put down is always turned towards you.
                : (placer != null ? RackFacing.fromPlayerYaw(placer.getLocation().getYaw()) : RackFacing.NORTH);

        World world = block.getWorld();
        Rack rack = new Rack(
                repository.nextId(), key, variant, type, resolved,
                placer != null ? placer.getUniqueId() : null,
                world.getGameTime(),
                (short) 0, null, null);

        if (!repository.add(rack)) {
            return null; // lost a race for the same block; the other placement stands
        }

        block.setType(type.isWall() ? Material.AIR : Material.BARRIER, false);
        renderer.spawn(rack, world);
        return rack;
    }

    // ------------------------------------------------------------------------------------------------
    // Swapping an item
    // ------------------------------------------------------------------------------------------------

    /**
     * Trades what the player is holding for what is on the rack, in one motion:
     * {@code actions/swap_item/_run}.
     *
     * <p>Three things make it a no-op rather than a partial change, all of them from the original:
     * the player holds something a rack of this type will not take; the rack is not in the database;
     * or both the hand and the slot are empty, so there is nothing to trade either way.
     *
     * @return true if anything changed
     */
    public boolean swapItem(Player player, Rack rack, RackPart part, World world) {
        ItemStack held = player.getInventory().getItemInMainHand();
        boolean handEmpty = held.getType().isAir();

        if (!handEmpty && !placeable.isPlaceable(rack.type(), held)) {
            return false;
        }

        ItemStack incoming = handEmpty ? null : held.clone();
        ItemStack outgoing = rack.item(part);
        if (incoming == null && outgoing == null) {
            return false;
        }

        // Commit the rack first and get its write moving, then hand the old item over. The two
        // stacks are copies taken above, so no reference is ever shared between the rack and the
        // player's inventory — the swap cannot leave a second copy of either item anywhere.
        rack.setItem(part, incoming);
        repository.save(rack);
        renderer.refreshItem(rack, part, world);

        player.getInventory().setItemInMainHand(outgoing);

        // Taking sounds like pulling something off a frame; putting or trading sounds like gearing
        // up. Same as the data pack, whose /playsound ran `at` the interaction entity and so was
        // already positioned on the rack.
        play(world, rack, incoming == null ? Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM : Sound.ITEM_ARMOR_EQUIP_GENERIC);
        return true;
    }

    // ------------------------------------------------------------------------------------------------
    // Changing pose
    // ------------------------------------------------------------------------------------------------

    /**
     * Cycles how the rack's items are angled: {@code actions/change_pose/_run}. Six arrangements on
     * the ground, four on a wall, wrapping round to the first.
     *
     * <p>An empty rack has nothing to turn, so a sneaking click on one does nothing at all — the same
     * early exit the data pack took.
     *
     * @return true if the pose changed
     */
    public boolean cyclePose(Rack rack, World world) {
        if (rack.isEmpty()) {
            return false;
        }
        rack.cyclePose();
        repository.save(rack);
        renderer.refreshPose(rack, world);
        play(world, rack, Sound.ENTITY_ITEM_FRAME_ROTATE_ITEM);
        return true;
    }

    // ------------------------------------------------------------------------------------------------
    // Breaking
    // ------------------------------------------------------------------------------------------------

    /**
     * Breaks a rack: {@code actions/break/from_controller}.
     *
     * <p>Order is the original's, and the order matters. The rack leaves the database first, so
     * nothing can interact with it while it is coming apart. Then the block goes, then whatever was
     * on the rack drops — <b>always</b>, creative or not, because those items belong to whoever put
     * them there. Only then does the rack itself decide whether to drop, and it declines for a
     * creative breaker or when it is younger than {@code lootable-delay}. Entities are removed last.
     *
     * @param breaker the player responsible, or null when the rack broke on its own — a wall rack
     *                whose support was taken away
     */
    public void breakRack(Rack rack, World world, @Nullable Player breaker) {
        repository.remove(rack);

        // A ground rack sits on a barrier that has to go with it; a wall rack was already air.
        if (!rack.type().isWall()) {
            Block block = rack.key().toLocation(world).getBlock();
            if (block.getType() == Material.BARRIER) {
                block.setType(Material.AIR, false);
            }
        }

        dropIfPresent(rack.item(RackPart.LEFT), world, rack);
        dropIfPresent(rack.item(RackPart.RIGHT), world, rack);

        if (shouldDropItself(rack, world, breaker)) {
            Locale locale = breaker != null ? breaker.locale() : lang.fallbackLocale();
            drop(world, rack, items.create(rack.variant(), locale));
        }

        renderer.despawn(rack, world);
        play(world, rack, Sound.ENTITY_ARMOR_STAND_BREAK);
    }

    /**
     * Whether the rack drops as an item: {@code actions/break/should_loot_itself}.
     *
     * <p>The age check exists for land-protection plugins. A protection plugin that undoes a
     * placement a moment later would otherwise let a player place and break a rack in the same breath
     * and walk away with two, so an operator can set {@code lootable-delay} to a handful of ticks and
     * have a rack that never really existed leave nothing behind.
     */
    private boolean shouldDropItself(Rack rack, World world, @Nullable Player breaker) {
        if (breaker != null && breaker.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        long delay = config.get().getLootableDelay();
        if (delay <= 0) {
            return true;
        }
        long age = world.getGameTime() - rack.createdAtGameTime();
        return age >= delay;
    }

    private void dropIfPresent(@Nullable ItemStack item, World world, Rack rack) {
        if (item != null) {
            drop(world, rack, item);
        }
    }

    /**
     * Drops an item from the rack's centre with the little scatter the data pack gave it
     * ({@code packages/dynamic_item/drop}): a small random horizontal nudge and a consistent upward
     * hop, so two items coming off one rack do not land on top of each other.
     *
     * <p>Pickup delay is zeroed to match {@code summon item}, which sets none — a player breaking a
     * rack picks it straight back up rather than waiting out Bukkit's default half-second.
     */
    private void drop(World world, Rack rack, ItemStack item) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector motion = new Vector(
                random.nextInt(-10, 11) * 0.01,
                random.nextInt(10, 15) * 0.01,
                random.nextInt(-10, 11) * 0.01);

        Location at = rack.key().center(world);
        world.dropItem(at, item, (Item spawned) -> {
            spawned.setVelocity(motion);
            spawned.setPickupDelay(0);
        });
    }

    private void play(World world, Rack rack, Sound sound) {
        world.playSound(rack.key().center(world), sound, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    // ------------------------------------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------------------------------------

    /** The rack occupying {@code block}, or null. */
    public @Nullable Rack rackAt(Block block) {
        return repository.get(RackKey.of(block));
    }

    public RackRepository repository() {
        return repository;
    }

    public RackRenderer renderer() {
        return renderer;
    }

    public RackItems items() {
        return items;
    }
}
