package com.racks;

import com.racks.behavior.WallSupportService;
import com.racks.command.RacksCommand;
import com.racks.config.PluginConfig;
import com.racks.item.PlaceableItems;
import com.racks.item.RackItems;
import com.racks.item.RackRecipes;
import com.racks.lang.LanguageManager;
import com.racks.listener.RackBlockListener;
import com.racks.listener.RackChunkListener;
import com.racks.listener.RackInteractListener;
import com.racks.listener.RackItemUpgradeListener;
import com.racks.listener.RackPlaceListener;
import com.racks.migration.DatapackAdopter;
import com.racks.render.RackEntityKeys;
import com.racks.render.RackRenderer;
import com.racks.scheduler.Scheduler;
import com.racks.serialization.ItemCodec;
import com.racks.service.RackService;
import com.racks.storage.RackRepository;
import com.racks.storage.SqliteRackStorage;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

/**
 * A Paper and Folia port of KawaMood's <b>Racks</b> data pack (CC BY-NC-SA 4.0).
 *
 * <p>The behaviour is the data pack's, kept deliberately intact — the same item, the same crafting
 * recipe, racks built out of the same entities at the same offsets, the same poses, the same rules
 * about what may go on a rack and what happens when one is broken. What changes is everything
 * underneath: placed racks live in SQLite instead of command storage, with each held item stored
 * through Paper's own {@code ItemStack} serializer, so a rack can hold anything a player can carry
 * and keep it across a Minecraft version upgrade.
 *
 * <h2>How the pieces fit</h2>
 * <pre>
 *   listeners ──▶ RackService ──▶ RackRepository ──▶ SqliteRackStorage
 *                      │                                (writer thread)
 *                      └──────▶ RackRenderer  (entities in the world)
 * </pre>
 * Listeners receive events on the thread that owns the rack's block, and that thread is the only one
 * that ever touches a rack — so the service needs no locking, and the database writer only ever sees
 * finished, immutable snapshots.
 */
public final class RacksPlugin extends JavaPlugin {

    private volatile PluginConfig config;
    private LanguageManager lang;
    private Scheduler scheduler;

    private SqliteRackStorage storage;
    private RackRepository repository;
    private RackItems rackItems;
    private RackRecipes recipes;
    private RackService service;
    private WallSupportService wallSupport;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        config = new PluginConfig(getConfig());
        scheduler = new Scheduler(this);

        lang = new LanguageManager(this, config);
        GlobalTranslator.translator().addSource(lang.translator());

        PlaceableItems placeable = new PlaceableItems(getSLF4JLogger());
        rackItems = new RackItems(this, lang);
        RackEntityKeys keys = new RackEntityKeys(this);
        RackRenderer renderer = new RackRenderer(keys, placeable);
        ItemCodec codec = new ItemCodec();

        if (!openStorage(codec)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        service = new RackService(this::pluginConfig, repository, renderer, rackItems, placeable, lang);

        wallSupport = new WallSupportService(this::pluginConfig, scheduler, repository, service);
        repository.setWallRackListener(wallSupport);
        wallSupport.start();

        DatapackAdopter adopter = new DatapackAdopter(repository, renderer, keys, placeable, getSLF4JLogger());

        registerListeners(keys, renderer, adopter);
        registerCommands();

        recipes = new RackRecipes(this, rackItems, lang);
        // Registered whether or not recipes are on: the listener does nothing while none exist, and
        // registering it up front means a reload can turn recipes on without re-wiring anything.
        getServer().getPluginManager().registerEvents(recipes, this);
        if (config.isRecipesEnabled()) {
            recipes.register();
        }

        getSLF4JLogger().info("Racks enabled with {} rack(s) loaded{}",
                repository.size(), scheduler.isFolia() ? " (Folia)" : "");
    }

    @Override
    public void onDisable() {
        if (wallSupport != null) {
            wallSupport.shutdown();
        }
        if (recipes != null) {
            recipes.unregister();
        }
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        // Drains every queued write before the server finishes shutting down, so the last interaction
        // before a restart is on disk like any other.
        if (repository != null) {
            repository.close();
        } else if (storage != null) {
            storage.close();
        }
        if (lang != null) {
            GlobalTranslator.translator().removeSource(lang.translator());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Wiring
    // ------------------------------------------------------------------------------------------------

    /** @return false if the database could not be opened, which is fatal — better off than half-on. */
    private boolean openStorage(ItemCodec codec) {
        Path dataFolder = getDataFolder().toPath();
        try {
            java.nio.file.Files.createDirectories(dataFolder);
            storage = new SqliteRackStorage(dataFolder, config.getDatabaseFile(), config.getTablePrefix());
            repository = new RackRepository(storage, codec, getSLF4JLogger());
            repository.loadAll();
            return true;
        } catch (Exception e) {
            getSLF4JLogger().error("Could not open the Racks database — disabling the plugin so no rack "
                    + "is placed that cannot be saved", e);
            if (storage != null) {
                storage.close();
            }
            return false;
        }
    }

    private void registerListeners(RackEntityKeys keys, RackRenderer renderer, DatapackAdopter adopter) {
        var manager = getServer().getPluginManager();
        manager.registerEvents(new RackPlaceListener(service, rackItems, repository, scheduler), this);
        manager.registerEvents(new RackInteractListener(service, repository, keys), this);
        manager.registerEvents(new RackBlockListener(service), this);
        manager.registerEvents(new RackItemUpgradeListener(rackItems), this);
        manager.registerEvents(new RackChunkListener(this::pluginConfig, repository, renderer, keys,
                adopter, scheduler, getSLF4JLogger()), this);
    }

    private void registerCommands() {
        RacksCommand command = new RacksCommand(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.build(),
                        "Racks admin commands", RacksCommand.aliases()));
    }

    // ------------------------------------------------------------------------------------------------
    // Reload
    // ------------------------------------------------------------------------------------------------

    /**
     * Re-reads {@code config.yml} and the language files.
     *
     * <p>Only the configuration is rebuilt: placed racks, their entities and the open database stay
     * exactly as they are, because nothing a reload can change affects them. Services read the config
     * through {@link #pluginConfig()} rather than holding it, so swapping the object is enough — no
     * listener is re-registered and no rack is touched.
     */
    public void reloadPlugin() {
        reloadConfig();
        config = new PluginConfig(getConfig());
        lang.reload(config);

        wallSupport.restart();

        recipes.unregister();
        if (config.isRecipesEnabled()) {
            recipes.register();
        }
    }

    /**
     * Flips the wall-support setting, writes it back to {@code config.yml} and restarts the sweep —
     * the plugin's version of the data pack's {@code settings/ignore_wall_rack_support} functions,
     * which kept the same flag in a scoreboard value. Both survive a restart; a file is simply the
     * natural place for it here, and it means the setting can be edited without the server running.
     */
    public void setIgnoreWallRackSupport(boolean value) {
        config.setIgnoreWallRackSupport(value);
        getConfig().set("settings.ignore-wall-rack-support", value);
        saveConfig();
        wallSupport.refresh();
    }

    // ------------------------------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------------------------------

    public PluginConfig pluginConfig() {
        return config;
    }

    public LanguageManager languageManager() {
        return lang;
    }

    public RackItems rackItems() {
        return rackItems;
    }

    public RackService rackService() {
        return service;
    }

    public RackRepository repository() {
        return repository;
    }

    public Scheduler scheduler() {
        return scheduler;
    }
}
