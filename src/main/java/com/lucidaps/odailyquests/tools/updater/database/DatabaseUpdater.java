package com.lucidaps.odailyquests.tools.updater.database;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.files.FilesManager;
import com.lucidaps.odailyquests.files.implementations.ProgressionFile;
import com.lucidaps.odailyquests.quests.player.progression.storage.DatabaseManager;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;

public abstract class DatabaseUpdater implements IDatabaseUpdater {

    protected final File configFile;
    protected final FileConfiguration config;
    protected final DatabaseManager databaseManager;
    protected final ProgressionFile progressionFile;

    protected DatabaseUpdater(ODailyQuests plugin) {
        final FilesManager filesManager = plugin.getFilesManager();
        this.configFile = filesManager.getConfigurationFile().getFile();
        this.config = filesManager.getConfigurationFile().getConfig();
        this.databaseManager = plugin.getDatabaseManager();
        this.progressionFile = filesManager.getProgressionFile();
    }

    protected void updateVersion(String version) {
        config.set("database_version", version);
        try {
            config.save(configFile);
        } catch (IOException e) {
            PluginLogger.error("Error while saving the configuration file.");
        }
    }
}

