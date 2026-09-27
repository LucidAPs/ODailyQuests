package com.lucidaps.odailyquests;

import com.jeff_media.customblockdata.CustomBlockData;
import com.lucidaps.odailyquests.commands.admin.AdminCommandRegistry;
import com.lucidaps.odailyquests.commands.admin.handlers.*;
import com.lucidaps.odailyquests.commands.player.PlayerCommandRegistry;
import com.lucidaps.odailyquests.files.FilesManager;
import com.lucidaps.odailyquests.quests.QuestTypeRegistry;
import com.lucidaps.odailyquests.commands.admin.convert.ConvertCommand;
import com.lucidaps.odailyquests.quests.types.global.*;
import com.lucidaps.odailyquests.quests.types.item.*;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.TimerTask;
import com.lucidaps.odailyquests.commands.admin.handlers.*;
import com.lucidaps.odailyquests.commands.player.handlers.MeCommand;
import com.lucidaps.odailyquests.commands.player.handlers.PRerollCommand;
import com.lucidaps.odailyquests.commands.player.handlers.PShowCommand;
import com.lucidaps.odailyquests.events.restart.RestartHandler;
import com.lucidaps.odailyquests.externs.IntegrationsManager;
import com.lucidaps.odailyquests.commands.admin.AdminCommands;
import com.lucidaps.odailyquests.commands.player.PlayerCommands;
import com.lucidaps.odailyquests.reload.ReloadService;
import com.lucidaps.odailyquests.commands.admin.AdminCompleter;
import com.lucidaps.odailyquests.commands.player.PlayerCompleter;
import com.lucidaps.odailyquests.commands.interfaces.InterfacesManager;
import com.lucidaps.odailyquests.commands.interfaces.InventoryClickListener;
import com.lucidaps.odailyquests.configuration.essentials.TimestampMode;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.events.EventsManager;
import com.lucidaps.odailyquests.files.*;
import com.lucidaps.odailyquests.quests.types.custom.vote.VotifierPlusQuest;
import com.lucidaps.odailyquests.quests.categories.CategoriesLoader;
import com.lucidaps.odailyquests.quests.player.progression.storage.DatabaseManager;
import com.lucidaps.odailyquests.quests.types.entity.BreedQuest;
import com.lucidaps.odailyquests.quests.types.entity.KillQuest;
import com.lucidaps.odailyquests.quests.types.entity.ShearQuest;
import com.lucidaps.odailyquests.quests.types.entity.TameQuest;
import com.lucidaps.odailyquests.quests.types.global.*;
import com.lucidaps.odailyquests.quests.types.inventory.GetQuest;
import com.lucidaps.odailyquests.quests.types.inventory.LocationQuest;
import com.lucidaps.odailyquests.quests.types.inventory.PlaceholderQuest;
import com.lucidaps.odailyquests.quests.types.item.*;
import com.lucidaps.odailyquests.tools.*;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.tools.updater.config.ConfigUpdateManager;
import com.lucidaps.odailyquests.tools.updater.database.DatabaseUpdateManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;

/**
 * Main class of the ODailyQuests plugin.
 */
public final class ODailyQuests extends JavaPlugin {

    public static ODailyQuests INSTANCE;
    private final QuestTypeRegistry questTypeRegistry = new QuestTypeRegistry();
    private final PlayerCommandRegistry playerCommandRegistry = new PlayerCommandRegistry();
    private final AdminCommandRegistry adminCommandRegistry = new AdminCommandRegistry();

    /**
     * Getting instance of files classes.
     */
    private InterfacesManager interfacesManager;
    private FilesManager filesManager;
    public TimerTask timerTask;
    private final java.util.EnumMap<QuestPeriod, TimerTask> timerTasks = new java.util.EnumMap<>(QuestPeriod.class);
    private ReloadService reloadService;
    private CategoriesLoader categoriesLoader;
    private DatabaseManager databaseManager;
    private RestartHandler restartHandler;

    boolean isServerStopping;

    /**
     * Plugin load logic.
     */
    @Override
    public void onLoad() {
        INSTANCE = this;
    }

    /**
     * Plugin startup logic.
     */
    @Override
    public void onEnable() {
        PluginLogger.info("Plugin is starting...");
        isServerStopping = false;

        /* Load files */
        this.filesManager = new FilesManager(this);
        this.filesManager.load();

        /* Check for updates */
        new ConfigUpdateManager(this).runUpdates();

        /* Init categories loader */
        this.categoriesLoader = new CategoriesLoader(questTypeRegistry);

        /* Load class instances */
        this.interfacesManager = new InterfacesManager(this);
        this.databaseManager = new DatabaseManager(this);

        /* Load dependencies */
        new IntegrationsManager(this).loadAllDependencies();

        /* Hook CustomBlockData */
        CustomBlockData.registerListener(this);

        /* Register all quest types */
        registerQuestTypes();

        /* Load all config elements */
        this.reloadService = new ReloadService(this);
        reloadService.reload();

        // stop loading if errors where detected on reload
        if (!this.isEnabled()) {
            return;
        }

        /* Apply database migrations if necessary */
        new DatabaseUpdateManager(this).runUpdates();

        /* Load listeners */
        new EventsManager(this).registerListeners();

        /* Register all subcommands */
        registerSubCommands();

        /* Load main commands */
        getCommand("dquests").setExecutor(new PlayerCommands(playerCommandRegistry));
        getCommand("dqadmin").setExecutor(new AdminCommands(this, adminCommandRegistry));

        /* Load Tab Completers */
        getCommand("dquests").setTabCompleter(new PlayerCompleter(playerCommandRegistry));
        getCommand("dqadmin").setTabCompleter(new AdminCompleter(adminCommandRegistry));

        /* Register plugin events */
        getServer().getPluginManager().registerEvents(new InventoryClickListener(interfacesManager.getQuestsInterfaces()), this);
        getServer().getPluginManager().registerEvents(new QuestsManager(this), this);

        /* Register server restart related events */
        restartHandler = new RestartHandler(this);
        restartHandler.registerSubClasses();

        /* Avoid errors on reload */
        if (!Bukkit.getServer().getOnlinePlayers().isEmpty()) {
            reloadService.loadConnectedPlayerQuests();

            PluginLogger.warn("It seems that you have reloaded the server.");
            PluginLogger.warn("Think that this can cause problems, especially in the data backup.");
            PluginLogger.warn("You should restart the server instead.");
        }

        /* Init delayed task to draw new quests */
        reloadPeriodTimers();

        PluginLogger.info("Plugin is started!");
    }

    /**
     * Register all available quest types.
     */
    private void registerQuestTypes() {
        /* entity quests */
        questTypeRegistry.register("KILL", KillQuest.class);
        questTypeRegistry.register("BREED", BreedQuest.class);
        questTypeRegistry.register("SHEAR", ShearQuest.class);
        questTypeRegistry.register("TAME", TameQuest.class);
        questTypeRegistry.register("FIREBALL_REFLECT", FireballReflectQuest.class);

        /* item quests */
        questTypeRegistry.register("BREAK", BreakQuest.class);
        questTypeRegistry.register("PLACE", PlaceQuest.class);
        questTypeRegistry.register("CRAFT", CraftQuest.class);
        questTypeRegistry.register("PICKUP", PickupQuest.class);
        questTypeRegistry.register("LAUNCH", LaunchQuest.class);
        questTypeRegistry.register("CONSUME", ConsumeQuest.class);
        questTypeRegistry.register("COOK", CookQuest.class);
        questTypeRegistry.register("ENCHANT", EnchantQuest.class);
        questTypeRegistry.register("FISH", FishQuest.class);
        questTypeRegistry.register("FARMING", FarmingQuest.class);

        /* inventory quests */
        questTypeRegistry.register("GET", GetQuest.class);
        questTypeRegistry.register("LOCATION", LocationQuest.class);
        questTypeRegistry.register("VILLAGER_TRADE", VillagerQuest.class);
        questTypeRegistry.register("PLACEHOLDER", PlaceholderQuest.class);
        questTypeRegistry.register("CARVE", CarveQuest.class);

        /* global quests */
        questTypeRegistry.register("MILKING", MilkingQuest.class);
        questTypeRegistry.register("EXP_POINTS", ExpPointsQuest.class);
        questTypeRegistry.register("EXP_LEVELS", ExpLevelQuest.class);
        questTypeRegistry.register("PLAYER_DEATH", PlayerDeathQuest.class);
        questTypeRegistry.register("FIREBALL_REFLECT", FireballReflectQuest.class);

        /* other plugins */
        questTypeRegistry.register("VOTIFIER_PLUS", VotifierPlusQuest.class);
    }

    /**
     * Register all available subcommands.
     */
    private void registerSubCommands() {
        playerCommandRegistry.registerCommand(new PShowCommand(interfacesManager.getQuestsInterfaces()));
        playerCommandRegistry.registerCommand(new MeCommand(interfacesManager.getPlayerQuestsInterface()));
        playerCommandRegistry.registerCommand(new PRerollCommand());

        adminCommandRegistry.registerCommand(new AddCommand());
        adminCommandRegistry.registerCommand(new RemoveCommand());
        adminCommandRegistry.registerCommand(new ResetCommand());
        adminCommandRegistry.registerCommand(new ARerollCommand());
        adminCommandRegistry.registerCommand(new CompleteCommand());
        adminCommandRegistry.registerCommand(new CustomCompleteCommand());
        adminCommandRegistry.registerCommand(new ConvertCommand());
        adminCommandRegistry.registerCommand(new AShowCommand(interfacesManager.getPlayerQuestsInterface()));
        adminCommandRegistry.registerCommand(new OpenCommand(interfacesManager.getPlayerQuestsInterface()));
        adminCommandRegistry.registerCommand(new SetCommand());
    }

    /**
     * Plugin shutdown logic.
     */
    @Override
    public void onDisable() {
        if (restartHandler != null) {
            restartHandler.setServerStopping();
        } else {
            setServerStopping(true);
        }

        if (timerTask != null) {
            timerTask.stop();
            timerTask = null;
        }
        timerTasks.values().forEach(TimerTask::stop);
        timerTasks.clear();

        /* Avoid errors on reload */
        if (reloadService != null) {
            reloadService.saveConnectedPlayerQuests();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }
        PluginLogger.info("Plugin is shutting down...");
    }

    /**
     * Check if the server is stopping.
     *
     * @return true if the server is stopping.
     */
    public boolean isServerStopping() {
        return this.isServerStopping;
    }

    /**
     * Set if the server is stopping.
     *
     * @param isServerStopping true if the server is stopping.
     */
    public void setServerStopping(boolean isServerStopping) {
        this.isServerStopping = isServerStopping;
    }

    /**
     * Get ReloadService instance.
     *
     * @return ReloadService instance.
     */
    public ReloadService getReloadService() {
        return reloadService;
    }

    /**
     * Get FilesManager instance.
     *
     * @return FilesManager instance.
     */
    public FilesManager getFilesManager() {
        return filesManager;
    }

    /**
     * Get InterfacesManager instance.
     *
     * @return InterfacesManager instance.
     */
    public InterfacesManager getInterfacesManager() {
        return interfacesManager;
    }

    /**
     * Get QuestsLoader instance.
     *
     * @return QuestsLoader instance.
     */
    public CategoriesLoader getCategoriesLoader() {
        return categoriesLoader;
    }

    /**
     * Get DatabaseManager instance.
     *
     * @return DatabaseManager instance.
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void reloadPeriodTimers() {
        timerTasks.values().forEach(TimerTask::stop);
        timerTasks.clear();

        for (QuestPeriod period : QuestPeriods.getEnabledPeriods()) {
            if (QuestPeriods.get(period).timestampMode() == 1) {
                timerTasks.put(period, new TimerTask(period, LocalDateTime.now()));
            }
        }
        timerTask = timerTasks.get(QuestPeriod.DAILY);
    }

}
