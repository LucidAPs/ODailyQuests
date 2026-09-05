package com.lucidaps.odailyquests.files.implementations;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.files.APluginFile;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigurationFile extends APluginFile {

    public ConfigurationFile(ODailyQuests plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        file = new File(plugin.getDataFolder(), "config.yml");

        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
            PluginLogger.info("Configuration file created.");
        }

        config = new YamlConfiguration();

        try {
            config.load(file);
        } catch (Exception e) {
            PluginLogger.error("An error occurred while loading the configuration file.");
            PluginLogger.error(e.getMessage());
        }
        PluginLogger.fine("Configuration file successfully loaded.");
    }
}