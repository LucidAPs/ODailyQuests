package com.lucidaps.odailyquests.files.implementations;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.files.APluginFile;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PlayerInterfaceFile extends APluginFile {

    public PlayerInterfaceFile(ODailyQuests plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        file = new File(plugin.getDataFolder(), "playerInterface.yml");

        if (!file.exists()) {
            plugin.saveResource("playerInterface.yml", false);
            PluginLogger.info("Player interface file created.");
        }

        config = new YamlConfiguration();

        try {
            config.load(file);
        } catch (Exception e) {
            PluginLogger.error("An error occurred while loading the player interface file.");
            PluginLogger.error(e.getMessage());
        }
        PluginLogger.fine("Player interface file successfully loaded.");
    }
}