package com.lucidaps.odailyquests.files.implementations;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.lucidaps.odailyquests.tools.PluginLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class QuestsFiles {

    private static final Map<QuestPeriod, Map<String, FileConfiguration>> configurations = new java.util.EnumMap<>(QuestPeriod.class);

    private final ODailyQuests plugin;

    public QuestsFiles(ODailyQuests plugin) {
        this.plugin = plugin;
    }

    public static FileConfiguration getQuestsConfigurationByCategory(String category) {
        return getQuestsConfigurationByCategory(QuestPeriod.DAILY, category);
    }

    public static FileConfiguration getQuestsConfigurationByCategory(QuestPeriod period, String category) {
        final FileConfiguration configuration = configurations.getOrDefault(period, Map.of()).get(category);
        if (configuration == null) {
            PluginLogger.error("Impossible to find the " + period.getDisplayName() + " configuration file for category " + category + ".");
            PluginLogger.error("Please check that the file exists and is correctly referenced in the configuration file (quests_per_category section).");
            PluginLogger.error("If the problem persists, please inform the developer.");
            return null;
        }

        return configuration;
    }

    /**
     * Init quests files.
     */
    public void load() {
        configurations.clear();

        for (QuestPeriod period : QuestPeriod.values()) {
            configurations.put(period, new HashMap<>());
            loadPeriod(period);
        }
    }

    private void loadPeriod(QuestPeriod period) {
        final String relativeFolder = switch (period) {
            case DAILY -> "quests";
            case WEEKLY -> "quests/weekly";
            case MONTHLY -> "quests/monthly";
        };
        final File questsFolder = new File(plugin.getDataFolder(), relativeFolder);

        if (!questsFolder.exists() || questsFolder.listFiles() == null || questsFolder.listFiles().length == 0) {
            questsFolder.mkdirs();
            if (period == QuestPeriod.DAILY) createDefaultQuestFiles();
            else createPeriodQuestFiles(period, questsFolder);
        }

        final File[] questFiles = questsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (questFiles == null) {
            PluginLogger.error("An error occurred while loading quests files.");
            PluginLogger.error("Please inform the developer.");
            return;
        }

        for (File file : questFiles) {
            final String category = file.getName().replace(".yml", "");

            final FileConfiguration config = new YamlConfiguration();
            try {
                config.load(file);
                configurations.get(period).put(category, config);
                PluginLogger.fine(period.getDisplayName() + " " + category + " quests file successfully loaded.");
            } catch (InvalidConfigurationException | IOException e) {
                PluginLogger.error("An error occurred while loading the " + category + " quests file.");
                PluginLogger.error("Please inform the developer.");
                PluginLogger.error(e.getMessage());
            }
        }
    }

    private void createDefaultQuestFiles() {
        final String[] defaultFiles = {"examples.yml", "easy.yml", "medium.yml", "hard.yml"};

        for (String fileName : defaultFiles) {
            plugin.saveResource("quests/" + fileName, false);
            PluginLogger.info(fileName + " created as default.");
        }
    }

    private void createPeriodQuestFiles(QuestPeriod period, File questsFolder) {
        final String[] defaultFiles = {"easy.yml", "medium.yml", "hard.yml"};
        for (String fileName : defaultFiles) {
            final File destination = new File(questsFolder, fileName);
            try (InputStream source = plugin.getResource("quests/" + fileName)) {
                if (source == null) continue;
                Files.copy(source, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                PluginLogger.info(period.getDisplayName() + " " + fileName + " example created.");
            } catch (IOException exception) {
                PluginLogger.error("Could not create " + period.getDisplayName() + " example file " + fileName + ".");
                PluginLogger.error(exception.getMessage());
            }
        }
    }
}
