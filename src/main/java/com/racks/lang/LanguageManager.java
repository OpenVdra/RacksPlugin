package com.racks.lang;

import com.racks.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Loads {@code language/<locale>/messages.yml} and exposes every string as an Adventure
 * {@link Component}.
 *
 * <p><b>Per-viewer localization.</b> {@link #get} returns a locale-free
 * {@link Component#translatable(String) translatable Component}; the text is resolved by
 * {@link RacksTranslator} (registered on the {@code GlobalTranslator}) against the recipient
 * client's own locale when the message is actually sent. Both bundled locales ({@code en_US},
 * {@code vi_VN}) plus anything an operator drops under {@code language/} are loaded up front.
 *
 * <p><b>Item text is different.</b> An {@link org.bukkit.inventory.ItemStack} carries its name and
 * lore as baked-in data rather than as something re-rendered per viewer, so
 * {@link #render(Locale, String, String...)} exists for those: it resolves eagerly for a known
 * recipient (the player running {@code /racks give}, the player at the crafting table) and falls
 * back to the configured locale when a rack is created with no viewer — a rack dropped on the floor.
 *
 * <p>Each raw YAML value is normalized <b>once</b> at load into an equivalent MiniMessage string:
 * legacy {@code &} strings are converted, {@code {prefix}} is inlined and {@code {placeholder}}
 * tokens become {@code <placeholder>} argument tags. Because substitutions are passed as arguments
 * rather than spliced into the string, a value such as a player name can never inject formatting
 * into the surrounding message.
 */
public final class LanguageManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    /** {@code {placeholder}} → {@code <placeholder>}. Placeholder names are always {@code [A-Za-z_]}. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z_]+)}");

    private static final String NS = "racks.msg.";

    /**
     * Locales shipped inside the jar. Their files are extracted on first run so auto-detection works
     * before an operator touches anything. Operator-added folders are discovered from disk on top.
     */
    private static final List<String> BUNDLED_LOCALES = List.of("en_US", "vi_VN");

    private final JavaPlugin plugin;
    private final RacksTranslator translator = new RacksTranslator();

    private volatile PluginConfig config;
    private volatile Locale fallbackLocale = Locale.US;

    /**
     * Per-(locale, key) cache of eagerly rendered zero-argument Components. Item names/lore come
     * from here on every rack item built, which happens per crafted item and per adopted rack, so
     * caching keeps that off the MiniMessage parser. Cleared on every (re)load. Bounded by
     * (distinct viewer locales) × (static keys).
     */
    private final Map<Locale, Map<String, Component>> renderCache = new ConcurrentHashMap<>();

    public LanguageManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        load();
    }

    public void reload(PluginConfig config) {
        this.config = config;
        load();
    }

    /** The Adventure translator to register once on the {@code GlobalTranslator}. */
    public RacksTranslator translator() {
        return translator;
    }

    /** Locale used when there is no single viewer to render for (world drops, console). */
    public Locale fallbackLocale() {
        return fallbackLocale;
    }

    // ------------------------------------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------------------------------------

    private void load() {
        Map<String, Map<Locale, String>> table = new HashMap<>();
        List<Locale> availableLocales = new ArrayList<>();
        boolean configuredFound = false;
        String configured = config.getLanguage();

        for (String folder : discoverLocaleFolders()) {
            String path = "language/" + folder + "/messages.yml";

            // Extract only locales that actually ship in the jar; operator-added folders are used
            // verbatim from disk (getResource == null, so there would be nothing to save).
            if (plugin.getResource(path) != null) {
                saveDefault(path);
            }

            FileConfiguration messages = loadFile(path);
            Locale loc = toLocale(folder);
            availableLocales.add(loc);
            if (folder.equalsIgnoreCase(configured)) {
                configuredFound = true;
            }

            String prefixMm = toMiniMessage(messages.getString("prefix", ""));
            for (String key : messages.getKeys(true)) {
                if (!messages.isString(key)) continue;
                String normalized = normalize(messages.getString(key), prefixMm);
                table.computeIfAbsent(NS + key, k -> new HashMap<>()).put(loc, normalized);
            }
        }

        if (!configuredFound) {
            plugin.getSLF4JLogger().warn("Locale '{}' not found, falling back to en_US", configured);
        }

        fallbackLocale = toLocale(configuredFound ? configured : "en_US");
        translator.apply(table, availableLocales, fallbackLocale, config.isAutoDetectLanguage());

        // The table, fallback and auto-detect flag just changed, so every previously rendered
        // Component is potentially stale.
        renderCache.clear();
    }

    /** Bundled locales unioned with any {@code language/<name>/messages.yml} present on disk. */
    private List<String> discoverLocaleFolders() {
        LinkedHashSet<String> names = new LinkedHashSet<>(BUNDLED_LOCALES);
        File langDir = new File(plugin.getDataFolder(), "language");
        File[] subs = langDir.listFiles(File::isDirectory);
        if (subs != null) {
            for (File dir : subs) {
                if (new File(dir, "messages.yml").exists()) {
                    names.add(dir.getName());
                }
            }
        }
        return new ArrayList<>(names);
    }

    /**
     * Normalizes a raw YAML value into a MiniMessage string: convert legacy to MiniMessage, inline
     * the (already-normalized) prefix, then turn {@code {placeholder}} tokens into tags.
     */
    private String normalize(String raw, String prefixMm) {
        String mm = toMiniMessage(raw);
        if (mm.contains("{prefix}")) {
            mm = mm.replace("{prefix}", prefixMm);
        }
        return PLACEHOLDER.matcher(mm).replaceAll("<$1>");
    }

    /**
     * A {@code '<'} means the string is already MiniMessage; otherwise it is legacy {@code &} codes,
     * converted once here into an equivalent MiniMessage string.
     */
    private String toMiniMessage(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.contains("<") ? raw : MINI.serialize(LEGACY.deserialize(raw));
    }

    private static Locale toLocale(String folder) {
        Locale parsed = Translator.parseLocale(folder);
        return parsed != null ? parsed : Locale.US;
    }

    private void saveDefault(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
    }

    private FileConfiguration loadFile(String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        InputStream stream = plugin.getResource(relativePath);
        if (stream != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)));
        }
        return cfg;
    }

    // ------------------------------------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------------------------------------

    /**
     * A deferred message: optional {@code replacements} are name/value pairs bound to
     * {@code <name>} argument tags. Resolved per recipient when sent.
     */
    public Component get(String key, String... replacements) {
        if (replacements.length < 2) {
            return Component.translatable(NS + key);
        }
        List<ComponentLike> args = new ArrayList<>(replacements.length / 2);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            args.add(Argument.string(replacements[i], replacements[i + 1]));
        }
        return Component.translatable(NS + key, args.toArray(new ComponentLike[0]));
    }

    /** Like {@link #get} but binds one argument to a ready-made component (e.g. a rack's own name). */
    public Component getRich(String key, String name, ComponentLike value) {
        return Component.translatable(NS + key, Argument.component(name, value));
    }

    /**
     * A deferred message with an arbitrary mix of arguments, built with {@link #arg}. Needed whenever
     * a message carries both plain values and a nested translatable — a wood name inside a sentence,
     * say — since a plain-string argument could not localize itself.
     */
    public Component getArgs(String key, ComponentLike... args) {
        return Component.translatable(NS + key, args);
    }

    /** A plain-text argument for {@link #getArgs}. */
    public static ComponentLike arg(String name, String value) {
        return Argument.string(name, value);
    }

    /** A component argument for {@link #getArgs}; its own translatables resolve for the reader too. */
    public static ComponentLike arg(String name, ComponentLike value) {
        return Argument.component(name, value);
    }

    /**
     * Eagerly renders {@code key} for {@code locale}. Needed for surfaces the server does not run
     * through the {@code GlobalTranslator} on the way out — item names and lore above all.
     */
    public Component render(Locale locale, String key, String... replacements) {
        Locale target = config.isAutoDetectLanguage() ? locale : fallbackLocale;
        if (replacements.length < 2) {
            return renderCache
                    .computeIfAbsent(target, l -> new ConcurrentHashMap<>())
                    .computeIfAbsent(NS + key,
                            k -> GlobalTranslator.render(Component.translatable(k), target));
        }
        return GlobalTranslator.render(get(key, replacements), target);
    }

    /** {@link #render} with a component argument, for nesting one localized value inside another. */
    public Component renderRich(Locale locale, String key, String name, ComponentLike value) {
        Locale target = config.isAutoDetectLanguage() ? locale : fallbackLocale;
        return GlobalTranslator.render(getRich(key, name, value), target);
    }
}
