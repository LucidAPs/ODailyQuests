package com.lucidaps.odailyquests.commands.admin.convert;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.quests.player.progression.storage.sql.SQLManager;
import com.lucidaps.odailyquests.quests.player.progression.storage.sql.sqlite.SQLiteManager;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.TaskScheduler;
import org.bukkit.configuration.file.FileConfiguration;

public class YAMLtoSQLiteConverter extends SQLConverter {

    public boolean convert() {

        try {
            TaskScheduler.runAsync(() -> {

                final FileConfiguration progressionFile = ODailyQuests.INSTANCE.getFilesManager().getProgressionFile().getConfig();
                final SQLManager sqlManager = new SQLiteManager();

                convertData(progressionFile, sqlManager);
            });
        } catch (Exception e) {
            PluginLogger.error("An error occurred while converting YAML to SQLite.");
            PluginLogger.error(e.getMessage());
            return false;
        }

        return true;
    }
}
