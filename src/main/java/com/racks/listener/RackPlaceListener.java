package com.racks.listener;

import com.racks.item.RackItems;
import com.racks.model.RackFacing;
import com.racks.model.RackKey;
import com.racks.model.RackType;
import com.racks.model.RackVariant;
import com.racks.scheduler.Scheduler;
import com.racks.service.RackService;
import com.racks.storage.RackRepository;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Turns a placed rack head into an actual rack.
 *
 * <p>Like the data pack, the head is allowed to be placed normally and is converted a tick later
 * rather than being intercepted. That is not incidental: letting the server place the block means
 * the server also takes the item out of the player's hand, under its own rules for creative mode,
 * off-hand placement and every protection plugin that ran before this listener — so there is no
 * hand-rolled item accounting anywhere in the placement path, and therefore nothing to get wrong.
 *
 * <p>The one tick between the head appearing and the rack replacing it is the same flicker the data
 * pack had, for the same reason: its advancement reward could not run any sooner either.
 */
public final class RackPlaceListener implements Listener {

    private final RackService service;
    private final RackItems items;
    private final RackRepository repository;
    private final Scheduler scheduler;

    public RackPlaceListener(RackService service, RackItems items, RackRepository repository, Scheduler scheduler) {
        this.service = service;
        this.items = items;
        this.repository = repository;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack used = event.getItemInHand();
        RackVariant variant = items.variantOf(used);
        if (variant == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        // Read the placed head now, while it is still in the world: whether it went on a wall, and
        // which way it points. A tick from now the block may be anything.
        boolean wall = block.getType() == Material.PLAYER_WALL_HEAD;
        RackFacing headFacing = wall && block.getBlockData() instanceof Directional directional
                ? RackFacing.of(directional.getFacing())
                : RackFacing.NORTH;

        Location location = block.getLocation();
        RackType type = RackType.of(wall);
        EquipmentSlot hand = event.getHand();

        scheduler.runAtLocation(location, () -> convert(location, player, variant, type, headFacing, hand));
    }

    /**
     * Replaces the placeholder head with a real rack, on the thread that owns the block.
     *
     * <p>Re-checks everything it was told a tick ago, because a tick is long enough for the head to
     * have been broken, replaced or blown up in the meantime.
     */
    private void convert(Location location, Player player, RackVariant variant, RackType type,
                         RackFacing headFacing, EquipmentSlot hand) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Block block = location.getBlock();
        Material material = block.getType();
        if (material != Material.PLAYER_HEAD && material != Material.PLAYER_WALL_HEAD) {
            return; // the head is gone; nothing to convert
        }

        // A wall rack leaves its block as air, so a second head can be placed right inside one. The
        // data pack refused that and handed the item back; so does this.
        if (repository.contains(RackKey.of(block))) {
            block.setType(Material.AIR, false);
            refund(player, variant, hand);
            return;
        }

        if (!player.hasPermission("racks.use")) {
            block.setType(Material.AIR, false);
            refund(player, variant, hand);
            return;
        }

        service.place(block, variant, type, headFacing, player);
    }

    /**
     * Gives back the head that was consumed placing it. Skipped in creative, where nothing was taken
     * — the same {@code return 1} the data pack's cancel path took for a creative player.
     */
    private void refund(Player player, RackVariant variant, EquipmentSlot hand) {
        if (!player.isOnline() || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack refund = items.create(variant, player.locale());
        ItemStack inHand = player.getInventory().getItem(hand);

        // Put it back in the hand it came from when that hand is now empty; otherwise fall back to
        // the inventory (and the floor), so a refund can never overwrite something else.
        if (inHand == null || inHand.getType().isAir()) {
            player.getInventory().setItem(hand, refund);
        } else {
            items.giveOrDrop(player, refund);
        }
    }
}
