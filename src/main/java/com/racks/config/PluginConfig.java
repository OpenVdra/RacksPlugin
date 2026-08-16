package com.racks.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed, immutable snapshot of {@code config.yml}. Re-created on {@code /racks reload}; every
 * consumer reads through the plugin so a reload is picked up without stale references.
 */
public final class PluginConfig {

    private final String language;
    private final boolean autoDetectLanguage;
    private final String databaseFile;
    private final String tablePrefix;
    private final boolean ignoreWallRackSupport;
    private final int wallSupportCheckInterval;
    private final boolean worldGuardIntegration;
    private final boolean griefPreventionIntegration;
    private final boolean adoptDatapackRacks;
    private final boolean recipesEnabled;
    private final boolean updateChecker;

    public PluginConfig(FileConfiguration config) {
        this.language = config.getString("language", "en_US");
        this.autoDetectLanguage = config.getBoolean("language-auto-detect", true);
        this.databaseFile = config.getString("database.file", "racks.db");
        this.tablePrefix = sanitizePrefix(config.getString("database.table-prefix", ""));
        this.ignoreWallRackSupport = config.getBoolean("settings.ignore-wall-rack-support", false);
        this.wallSupportCheckInterval = Math.max(1, config.getInt("settings.wall-support-check-interval", 10));
        this.worldGuardIntegration = config.getBoolean("protection.worldguard", true);
        this.griefPreventionIntegration = config.getBoolean("protection.griefprevention", true);
        this.adoptDatapackRacks = config.getBoolean("adopt-datapack-racks", false);
        this.recipesEnabled = config.getBoolean("recipes-enabled", true);
        this.updateChecker = config.getBoolean("update-checker", true);
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

    public int getWallSupportCheckInterval() {
        return wallSupportCheckInterval;
    }

    public boolean isWorldGuardIntegrationEnabled() {
        return worldGuardIntegration;
    }

    public boolean isGriefPreventionIntegrationEnabled() {
        return griefPreventionIntegration;
    }

    public boolean isAdoptDatapackRacks() {
        return adoptDatapackRacks;
    }

    public boolean isRecipesEnabled() {
        return recipesEnabled;
    }

    /** Read once at enable; the check does not re-run on {@code /racks reload}. */
    public boolean isUpdateCheckerEnabled() {
        return updateChecker;
    }
}
