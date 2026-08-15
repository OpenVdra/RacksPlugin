package com.racks.render;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * How a rack's entities say which rack they belong to.
 *
 * <p>Two labelling systems sit side by side, on purpose:
 * <ul>
 *   <li><b>Persistent data</b> ({@code racks:rack_id}, {@code racks:role}, {@code racks:slot}) is what
 *       the plugin actually reads. It is typed, cheap to test and cannot collide with anything else
 *       on the server.</li>
 *   <li><b>Scoreboard tags</b> are the data pack's originals ({@code pk.racks.block.rack.*}), applied
 *       unchanged. Nothing in the plugin needs them, but every command, selector and admin tool that
 *       server owners already had pointed at Racks entities keeps working, and they are what lets the
 *       adopter recognise racks the data pack left behind.</li>
 * </ul>
 */
public final class RackEntityKeys {

    /** Role of an entity within its rack. */
    public enum Role {
        /** The invisible marker at the block's centre; a rack's anchor and its proof of existence. */
        CONTROLLER,
        /** A block display making up the rack's frame. */
        BODY,
        /** A clickable hitbox. */
        INTERACTION,
        /** An item display showing what is on the rack. */
        ITEM;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static @Nullable Role byId(@Nullable String id) {
            if (id == null) return null;
            for (Role r : values()) {
                if (r.id().equals(id)) return r;
            }
            return null;
        }
    }

    // -- Data pack scoreboard tags, kept verbatim ----------------------------------------------------

    public static final String TAG_CUSTOM_BLOCK = "pk.custom_block";
    public static final String TAG_RACKS = "pk.racks";
    public static final String TAG_BLOCK = "pk.racks.block";
    public static final String TAG_RACK = "pk.racks.block.rack";
    public static final String TAG_CONTROLLER = "pk.racks.block.rack.controller";
    public static final String TAG_BODY = "pk.racks.block.rack.body";
    public static final String TAG_INTERACTION = "pk.racks.block.rack.interaction";
    public static final String TAG_ITEM = "pk.racks.block.rack.item";

    private final NamespacedKey rackId;
    private final NamespacedKey role;
    private final NamespacedKey slot;

    public RackEntityKeys(Plugin plugin) {
        this.rackId = new NamespacedKey(plugin, "rack_id");
        this.role = new NamespacedKey(plugin, "role");
        this.slot = new NamespacedKey(plugin, "slot");
    }

    public NamespacedKey rackId() {
        return rackId;
    }

    public NamespacedKey role() {
        return role;
    }

    public NamespacedKey slot() {
        return slot;
    }

    /** The rack this entity belongs to, or null if it is not part of a plugin-managed rack. */
    public @Nullable Integer rackIdOf(Entity entity) {
        return entity.getPersistentDataContainer().get(rackId, PersistentDataType.INTEGER);
    }

    public boolean belongsTo(Entity entity, int id) {
        Integer stored = rackIdOf(entity);
        return stored != null && stored == id;
    }

    public @Nullable Role roleOf(Entity entity) {
        return Role.byId(entity.getPersistentDataContainer().get(role, PersistentDataType.STRING));
    }

    public @Nullable String slotOf(Entity entity) {
        return entity.getPersistentDataContainer().get(slot, PersistentDataType.STRING);
    }

    /** Stamps the plugin's identifying data onto a freshly spawned entity. */
    public void stamp(Entity entity, int id, Role entityRole, @Nullable String slotId) {
        var pdc = entity.getPersistentDataContainer();
        pdc.set(rackId, PersistentDataType.INTEGER, id);
        pdc.set(role, PersistentDataType.STRING, entityRole.id());
        if (slotId != null) {
            pdc.set(slot, PersistentDataType.STRING, slotId);
        }
    }
}
