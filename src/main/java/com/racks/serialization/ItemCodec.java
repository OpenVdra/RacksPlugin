package com.racks.serialization;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Turns the single {@link ItemStack} a rack slot holds into the bytes stored in the database, and
 * back.
 *
 * <p>The body is Paper's own {@link ItemStack#serializeAsBytes()}. That matters for a rack, whose
 * whole job is to hold somebody's real gear: it is the same NBT path the server uses for its own
 * saves, so enchantments, durability, custom components, trims and any third-party plugin data all
 * survive a round trip untouched — and Paper runs the data fixers on read, so a rack holding a
 * netherite sword keeps holding it across a Minecraft upgrade.
 *
 * <p>Storage format is {@code [1-byte version tag] + [body]}. The tag costs one byte per item and
 * buys the ability to change the body format later without orphaning rows already written: an
 * unknown tag is reported rather than guessed at.
 *
 * <p>An empty slot is not encoded at all — it is stored as SQL {@code NULL}, so the common case of a
 * half-full rack writes nothing.
 */
public final class ItemCodec {

    /** Current write format: version tag + {@link ItemStack#serializeAsBytes()}. */
    private static final byte FORMAT_VERSION = 0x01;

    /**
     * Encodes one slot. Returns null for an empty slot, which the storage layer writes as SQL NULL.
     *
     * <p>Must be called from the thread that owns the item (main/region thread); the resulting byte
     * array is what gets handed to the database writer, so no Bukkit object ever crosses threads.
     */
    public byte @Nullable [] encode(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
            return null;
        }
        byte[] body = item.serializeAsBytes();
        byte[] result = new byte[1 + body.length];
        result[0] = FORMAT_VERSION;
        System.arraycopy(body, 0, result, 1, body.length);
        return result;
    }

    /**
     * Decodes one slot. Null in, null out — an empty slot round-trips as empty.
     *
     * @throws CodecException if the data is malformed or carries an unknown format version. The
     *                        caller must not substitute an empty slot on failure: that would quietly
     *                        delete somebody's item. Skip the rack and leave the row alone instead.
     */
    public @Nullable ItemStack decode(byte @Nullable [] data) throws CodecException {
        if (data == null || data.length == 0) {
            return null;
        }
        if (data.length < 2) {
            throw new CodecException("Stored item data is too short (length=" + data.length + ")");
        }
        byte version = data[0];
        if (version != FORMAT_VERSION) {
            throw new CodecException("Unknown item format version 0x" + String.format("%02X", version)
                    + " — the plugin may need updating before this data can be read");
        }
        try {
            ItemStack item = ItemStack.deserializeBytes(Arrays.copyOfRange(data, 1, data.length));
            return item == null || item.getType().isAir() ? null : item;
        } catch (Exception e) {
            throw new CodecException("Failed to deserialize a stored rack item", e);
        }
    }
}
