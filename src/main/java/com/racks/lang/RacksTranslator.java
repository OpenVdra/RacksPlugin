package com.racks.lang;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adventure {@link net.kyori.adventure.translation.Translator} backing Racks' per-viewer
 * localization. It is registered once on the {@link net.kyori.adventure.translation.GlobalTranslator}
 * (see {@code RacksPlugin}), so every Component the plugin sends through chat or the action bar is
 * rendered against the recipient client's own locale at send time — which is why
 * {@link LanguageManager#get} can return a locale-free {@code Component.translatable} and never
 * thread a {@link Locale} through call sites.
 *
 * <p>It holds normalized MiniMessage strings keyed by translation key then {@link Locale} (see
 * {@link LanguageManager} for how the raw YAML is normalized). Keys are namespaced
 * ({@code racks.msg.*}) so they cannot collide with vanilla ones.
 *
 * <p>Lookup fallback for a requested locale: exact match → same language, any region → the
 * configured fallback ({@code language:} in config.yml) → {@code en_US}, which always ships in the
 * jar. With {@code language-auto-detect} off the requested locale is ignored entirely and everybody
 * gets the configured fallback, reproducing the single-language data pack.
 */
public final class RacksTranslator extends MiniMessageTranslator {

    private static final Key NAME = Key.key("racks", "lang");

    /** en_US is shipped in the jar and is the last-resort locale. */
    static final Locale ALWAYS_BUNDLED = Locale.US;

    // key -> (locale -> already-normalized MiniMessage string). Swapped atomically on (re)load.
    private volatile Map<String, Map<Locale, String>> table = Map.of();
    private volatile List<Locale> available = List.of();
    private volatile Locale configFallback = Locale.US;
    private volatile boolean autoDetect = true;

    public RacksTranslator() {
        super(MiniMessage.miniMessage());
    }

    @Override
    public Key name() {
        return NAME;
    }

    /**
     * Atomically swaps in a freshly-loaded table. Called on startup and on {@code /racks reload};
     * the translator stays registered on the GlobalTranslator across reloads, only its contents
     * change.
     */
    void apply(Map<String, Map<Locale, String>> table, List<Locale> available,
               Locale configFallback, boolean autoDetect) {
        this.available = List.copyOf(available);
        this.configFallback = configFallback;
        this.autoDetect = autoDetect;
        this.table = table; // published last so readers that see it also see the fields above
    }

    /** True when {@code key} resolves to something for at least one locale. */
    boolean contains(String key) {
        return table.containsKey(key);
    }

    @Override
    protected @Nullable String getMiniMessageString(String key, Locale locale) {
        Map<Locale, String> perLocale = table.get(key);
        if (perLocale == null) {
            return null; // unknown key: leave the raw key visible as a missing-translation signal
        }
        Locale wanted = autoDetect ? locale : configFallback;

        String exact = perLocale.get(wanted);
        if (exact != null) return exact;

        Locale sameLang = sameLanguage(wanted);
        if (sameLang != null) {
            String s = perLocale.get(sameLang);
            if (s != null) return s;
        }
        String fallback = perLocale.get(configFallback);
        if (fallback != null) return fallback;

        return perLocale.get(ALWAYS_BUNDLED);
    }

    /** The first loaded locale sharing {@code wanted}'s language (e.g. {@code vi} matches {@code vi_VN}). */
    private @Nullable Locale sameLanguage(Locale wanted) {
        String lang = wanted.getLanguage();
        if (lang.isEmpty()) return null;
        for (Locale l : available) {
            if (l.getLanguage().equals(lang)) return l;
        }
        return null;
    }
}
