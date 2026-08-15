package com.racks.model;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The twelve wood types a rack comes in.
 *
 * <p>Everything here is lifted verbatim from the data pack so a plugin rack is byte-for-byte the
 * same object a data pack rack was: the head texture in {@code items/rack/attributes/<variant>},
 * the {@code custom_model_data} string {@code pk_racks:<variant>_rack} used by the optional resource
 * pack, and the fence/button blocks the body is built from. The planks are the recipe ingredient.
 */
public enum RackVariant {

    ACACIA("acacia", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzk2NDU2MGU5MDJmYTY4Y2MwNTVjNWU1OTI2MWY2ODk1NmEyMDdlMmMxMWQ1NmMzYTYwYWFhNzhlYzVmNDNmIn19fQ=="),
    BAMBOO("bamboo", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmQ5MDFlOGMzYmFlOTJkNmM4MDg3MTJlYjk3MTk3ZjdjNjliMjA2MTZlMGUxMWY2YmJkZjNmNWRiZDA0ZjhlMyJ9fX0="),
    BIRCH("birch", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmY5Mjk4MWRmZjY3NmZkZGM5MDdhZTdjMDk2YzI5YWFiYjUwZjljOTFlZjUzMDRmNTNkZGQ5NWZjMzZkNjY3NyJ9fX0="),
    CHERRY("cherry", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzBlOTZlNjYwYmExYzg0YzhkMzIzMmYwMGI0Nzk3Mzk0ZjYyNWU3NGE3ZGNmNTE1MDdhOTZkMWMwYWY0YmMxNCJ9fX0="),
    CRIMSON("crimson", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTM5ZmQxYzM3YmZhN2UxNzU3NTk1YmYxZDk1YmE0MTgyZDVlOWM1OTY4NjFhYzY2YThlOWQzYWZjMWRiZGRmZiJ9fX0="),
    DARK_OAK("dark_oak", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTIyZDk4MGU4NTI4NjVjODIwZjcwYzI2OTg0NWY4NmIyNjZhNThlNDFiNGI5ZTY5MWU5YWY3NTdjODg5NDRmMCJ9fX0="),
    JUNGLE("jungle", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjI1ZGNkNjY3MDgwYWU5NWI4YjFlMTg5YjgwZWVlMWUwMzBiNGE0NmEyNzczYzExNTFjYThkYTMxNzc2M2MwNyJ9fX0="),
    MANGROVE("mangrove", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2NkZGM4ODgzNjAzNDJlYzc3YzI2ZDAxMzg4ODJmZWU3NTUxZmYzZDI0ZDE1MzMwZmE4YzMyODdjY2Q3YzM4NyJ9fX0="),
    OAK("oak", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGE5N2QyMzczODQyMTI5YzZkZjczOTgxNDFlZDQyMWIwNDFkYjY4NzRmODY5ZmRlMWM1NmZiZjk5NWQzMTY3NiJ9fX0="),
    PALE_OAK("pale_oak", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Q4OTRlYzQ1YjkzNDQ0N2IwYTQzZWZhODdlYzU1NWYxZTM2Y2MzNmU0MzY0NjQ5OTE3MzFlNTEzMjJiNzcwYmUifX19"),
    SPRUCE("spruce", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTViZWY4MzRkNmYzODc2Nzc5M2FhZjAxM2VlMTdlZDViZTA4ZDdjODMyMzAxMWVmNWIyZTVkNWYzZDkwNDU4OSJ9fX0="),
    WARPED("warped", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjVlMzVjMjBmMjUyNzczMzgwZDhjYTQ0NWQ0MjUxMWRlMmFlNDY2YTYyMWUyNjlhZWNlYzZjNzkwMzY2YmI0ZiJ9fX0=");

    private static final Map<String, RackVariant> BY_ID = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(RackVariant::id, Function.identity()));

    private final String id;
    private final String texture;
    private final String modelString;
    private final Material planks;
    private final Material fence;
    private final Material button;

    RackVariant(String id, String texture) {
        this.id = id;
        this.texture = texture;
        this.modelString = "pk_racks:" + id + "_rack";
        this.planks = Material.valueOf(id.toUpperCase(Locale.ROOT) + "_PLANKS");
        this.fence = Material.valueOf(id.toUpperCase(Locale.ROOT) + "_FENCE");
        this.button = Material.valueOf(id.toUpperCase(Locale.ROOT) + "_BUTTON");
    }

    /** The data pack's variant string, e.g. {@code dark_oak}. Also the database and language key. */
    public String id() {
        return id;
    }

    /** Base64 {@code textures} profile property of the rack's head item. */
    public String texture() {
        return texture;
    }

    /** {@code custom_model_data} string, kept so an existing Racks resource pack still applies. */
    public String modelString() {
        return modelString;
    }

    public Material planks() {
        return planks;
    }

    /** Block the body's fence parts display (ground: all six; wall: parts 3 and 4). */
    public Material fence() {
        return fence;
    }

    /** Block the wall body's parts 1 and 2 display. */
    public Material button() {
        return button;
    }

    public static @Nullable RackVariant byId(@Nullable String id) {
        return id == null ? null : BY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Parses a {@code custom_model_data} string such as {@code pk_racks:oak_rack}. This is how a rack
     * item left over from the data pack (which carries no plugin data) is still recognised.
     */
    public static @Nullable RackVariant byModelString(@Nullable String modelString) {
        if (modelString == null) return null;
        if (!modelString.startsWith("pk_racks:") || !modelString.endsWith("_rack")) return null;
        return byId(modelString.substring("pk_racks:".length(), modelString.length() - "_rack".length()));
    }

    /** Reverse of {@link #fence()} — used when adopting a data pack rack from its body entities. */
    public static @Nullable RackVariant byFence(Material fence) {
        for (RackVariant v : values()) {
            if (v.fence == fence) return v;
        }
        return null;
    }
}
