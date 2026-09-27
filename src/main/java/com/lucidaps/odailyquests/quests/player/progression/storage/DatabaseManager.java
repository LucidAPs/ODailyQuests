package com.lucidaps.odailyquests.quests.player.progression.storage;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.essentials.Database;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.quests.player.progression.storage.sql.SQLManager;
import com.lucidaps.odailyquests.quests.player.progression.storage.sql.sqlite.SQLiteManager;
import com.lucidaps.odailyquests.quests.player.progression.storage.sql.mysql.MySQLManager;
import com.lucidaps.odailyquests.quests.player.progression.storage.yaml.YamlManager;
import com.lucidaps.odailyquests.tools.PluginLogger;

import java.util.Map;

public class DatabaseManager {

    private final ODailyQuests plugin;
    private final AlltimeLeaderboard alltimeLeaderboard;

    private SQLManager sqlManager;
    private YamlManager yamlManager;

    public DatabaseManager(ODailyQuests plugin) {
        this.plugin = plugin;
        this.alltimeLeaderboard = new AlltimeLeaderboard(this);
    }

    public void load() {
        close();
        this.sqlManager = null;
        this.yamlManager = null;
        this.alltimeLeaderboard.clear();

        switch (Database.getMode()) {
            case MYSQL -> this.sqlManager = new MySQLManager();
            case SQLITE -> this.sqlManager = new SQLiteManager();
            case YAML -> this.yamlManager = new YamlManager(plugin.getFilesManager().getProgressionFile());
        }

        this.alltimeLeaderboard.refresh();
    }

    public void close() {
        if (this.sqlManager != null) {
            this.sqlManager.close();
        }
    }

    public void loadQuestsForPlayer(String playerName) {
        loadQuestsForPlayer(playerName, true);
    }

    public void loadQuestsForPlayer(String playerName, boolean sendStatusMessage) {
        final Map<String, PlayerQuests> activeQuests = QuestsManager.getActiveQuests();
        switch (Database.getMode()) {
            case YAML -> yamlManager.getLoadProgressionYAML().loadPlayerQuests(playerName, activeQuests, sendStatusMessage);
            case MYSQL, SQLITE -> sqlManager.getLoadProgressionSQL().loadProgression(playerName, activeQuests, sendStatusMessage);
            default ->
                    PluginLogger.error("Impossible to load player quests : the selected storage mode is incorrect !");
        }
    }

    public void saveProgressionForPlayer(String playerName, String playerUuid, PlayerQuests playerQuests) {
        saveProgressionForPlayer(playerName, playerUuid, plugin.isServerStopping());
    }

    public void saveProgressionForPlayer(String playerName, String playerUuid, PlayerQuests playerQuests, boolean forceSync) {
        saveProgressionForPlayer(playerName, playerUuid, forceSync);
    }

    public void saveProgressionForPlayer(String playerName, String playerUuid) {
        saveProgressionForPlayer(playerName, playerUuid, plugin.isServerStopping());
    }

    public void saveProgressionForPlayer(String playerName, String playerUuid, boolean forceSync) {
        final PlayerQuestData data = PlayerQuestData.capture(playerName);
        switch (Database.getMode()) {
            case YAML ->
                    yamlManager.getSaveProgressionYAML().saveProgression(playerName, playerUuid, data, forceSync);
            case MYSQL, SQLITE ->
                    sqlManager.getSaveProgressionSQL().saveProgression(playerName, playerUuid, data, forceSync);
            default ->
                    PluginLogger.error("Impossible to save player quests : the selected storage mode is incorrect !");
        }
    }

    public SQLManager getSqlManager() {
        return sqlManager;
    }

    public YamlManager getYamlManager() {
        return yamlManager;
    }

    public AlltimeLeaderboard getAlltimeLeaderboard() {
        return alltimeLeaderboard;
    }
}
