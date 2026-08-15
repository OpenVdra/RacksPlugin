package com.racks.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed, immutable snapshot of {@code config.yml}. Re-created on {@code /racks reload}; every
 * consumer reads through the plugin so a reload is picked up without stale references.
 *
 * <p>The one mutable value is {@link #isIgnoreWallRackSupport()}, because the data pack exposed it
 * as a runtime toggle ({@code /function pk_racks:settings/...}) and this port keeps that: the
 * command writes it back to disk and flips the live value.
 */
public final class PluginConfig {

    private final String language;
    private final boolean autoDetectLanguage;
    private final String databaseFile;
    private final String tablePrefix;
    private final int wallSupportCheckInterval;
    private final long lootableDelay;
    private final boolean adoptDatapackRacks;
    private final boolean recipesEnabled;

    private volatile boolean ignoreWallRackSupport;

    public PluginConfig(FileConfiguration config) {
        this.language = config.getString("language", "en_US");
        this.autoDetectLanguage = config.getBoolean("language-auto-detect", true);
        this.databaseFile = config.getString("database.file", "racks.db");
        this.tablePrefix = sanitizePrefix(config.getString("database.table-prefix", ""));
        this.ignoreWallRackSupport = config.getBoolean("settings.ignore-wall-rack-support", false);
        this.wallSupportCheckInterval = Math.max(1, config.getInt("settings.wall-support-check-interval", 10));
        this.lootableDelay = Math.max(0, config.getLong("settings.lootable-delay", 0));
        this.adoptDatapackRacks = config.getBoolean("adopt-datapack-racks", true);
        this.recipesEnabled = config.getBoolean("recipes-enabled", true);
    }

    /**
     * A table prefix is interpolated straight into DDL/DML (it cannot be a bind parameter), so
     * anything that is not a plain identifier is dropped rather than trusted.
     */
    private static String sanitizePrefix(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.matches("[A-Za-z0-9_]+") ? raw : "";
    }

    public String getLanguage() {
        return language;
    }

    public boolean isAutoDetectLanguage() {
        return autoDetectLanguage;
    }

    public String getDatabaseFile() {
        return databaseFile;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public boolean isIgnoreWallRackSupport() {
        return ignoreWallRackSupport;
    }

    public void setIgnoreWallRackSupport(boolean value) {
        this.ignoreWallRackSupport = value;
    }

    public int getWallSupportCheckInterval() {
        return wallSupportCheckInterval;
    }

    public long getLootableDelay() {
        return lootableDelay;
    }

    public boolean isAdoptDatapackRacks() {
        return adoptDatapackRacks;
    }

    public boolean isRecipesEnabled() {
        return recipesEnabled;
    }
}
