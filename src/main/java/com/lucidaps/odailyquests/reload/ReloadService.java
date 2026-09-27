package com.lucidaps.odailyquests.reload;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.configuration.essentials.ReloadMessage;
import com.lucidaps.odailyquests.configuration.integrations.NexoEnabled;
import com.lucidaps.odailyquests.quests.categories.CategoriesLoader;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ReloadService {

    private final ODailyQuests plugin;
    private final CategoriesLoader categoriesLoader;

    /**
     * Constructor.
     *
     * @param plugin main class instance.
     */
    public ReloadService(ODailyQuests plugin) {
        this.plugin = plugin;
        this.categoriesLoader = plugin.getCategoriesLoader();
    }

    /**
     * Load all quests from connected players, to avoid errors on reload.
     */
    public void loadConnectedPlayerQuests() {
        loadConnectedPlayerQuests(true);
    }

    public void loadConnectedPlayerQuests(boolean sendStatusMessage) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!QuestsManager.isPlayerLoaded(player.getName())) {
                plugin.getDatabaseManager().loadQuestsForPlayer(player.getName(), sendStatusMessage);
            }
        }
    }

    /**
     * Save all quests from connected players, to avoid errors on reload.
     */
    public void saveConnectedPlayerQuests() {
        saveConnectedPlayerQuests(plugin.isServerStopping());
    }

    public void saveConnectedPlayerQuests(boolean forceSync) {
        final java.util.Set<String> loadedPlayers = QuestsManager.getLoadedPlayers();
        for (String playerName : loadedPlayers) {
            final Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                Debugger.write("Impossible to save progression for player " + playerName + " because the player is offline.");
                PluginLogger.warn("Impossible to save progression for player " + playerName + " because the player is offline.");
                continue;
            }

            plugin.getDatabaseManager().saveProgressionForPlayer(player.getName(), player.getUniqueId().toString(), forceSync);
            QuestsManager.removePlayer(playerName);
        }
    }

    /**
     * Execute all required actions when the command /qadmin reload is performed.
     */
    public void reload() {
        try {
            /* load files */
            plugin.getFilesManager().load();

            /* load configurations */
            ConfigFactory.registerConfigs(plugin.getFilesManager());

            /* load database */
            plugin.getDatabaseManager().load();

            /* load quests & interface */
            if (!NexoEnabled.isEnabled() || NexoEnabled.isLoaded()) {

                categoriesLoader.loadCategories();
                plugin.getInterfacesManager().initAllObjects();
            }

            saveConnectedPlayerQuests(true);

            final boolean sendStatusOnReload = ReloadMessage.shouldSendOnReload();
            TaskScheduler.runSyncLater(() -> loadConnectedPlayerQuests(sendStatusOnReload), 20L);
        } catch (Exception e) {
            PluginLogger.error("An error occurred while reloading the plugin. Please check the logs for details.");
            PluginLogger.error(e.getMessage());
        }
    }
}
