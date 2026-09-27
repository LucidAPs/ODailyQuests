package com.lucidaps.odailyquests.files.implementations;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.essentials.LegacyPeriodResolver;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.lucidaps.odailyquests.tools.PluginLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuestsFiles {

    private enum NamingStyle {
        LEGACY,
        MODERN
    }

    private record StandardQuestFile(String resourceName, String modernName, String legacyName) {
        String destinationName(NamingStyle style) {
            return style == NamingStyle.LEGACY ? legacyName : modernName;
        }
    }

    private static final List<StandardQuestFile> STANDARD_QUEST_FILES = List.of(
            new StandardQuestFile("easy.yml", "easy.yml", "easyQuests.yml"),
            new StandardQuestFile("medium.yml", "medium.yml", "mediumQuests.yml"),
            new StandardQuestFile("hard.yml", "hard.yml", "hardQuests.yml"),
            new StandardQuestFile("global.yml", "global.yml", "globalQuests.yml")
    );

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

        final File dailyFolder = getPeriodFolder(QuestPeriod.DAILY);
        if (!dailyFolder.exists()) dailyFolder.mkdirs();
        if (!hasYamlFiles(dailyFolder)) createDefaultQuestFiles(dailyFolder);

        final NamingStyle namingStyle = detectNamingStyle(dailyFolder);
        final FileConfiguration mainConfig = plugin.getFilesManager().getConfigurationFile().getConfig();
        final boolean legacySinglePeriod = !mainConfig.isConfigurationSection("quest_periods");
        final QuestPeriod legacyPeriod = legacySinglePeriod
                ? LegacyPeriodResolver.resolve(mainConfig)
                : null;

        for (QuestPeriod period : QuestPeriod.values()) {
            configurations.put(period, new HashMap<>());
            final File questsFolder = getPeriodFolder(period);
            if (!questsFolder.exists()) questsFolder.mkdirs();

            if (legacySinglePeriod && period != QuestPeriod.DAILY && period == legacyPeriod) {
                copyLegacyPeriodFiles(dailyFolder, questsFolder, namingStyle, mainConfig);
            }
            ensureStandardQuestFiles(period, questsFolder, namingStyle);
            loadPeriod(period, questsFolder);
        }
    }

    private File getPeriodFolder(QuestPeriod period) {
        final String relativeFolder = switch (period) {
            case DAILY -> "quests";
            case WEEKLY -> "quests/weekly";
            case MONTHLY -> "quests/monthly";
        };
        return new File(plugin.getDataFolder(), relativeFolder);
    }

    private void loadPeriod(QuestPeriod period, File questsFolder) {
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

    private boolean hasYamlFiles(File folder) {
        final File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        return files != null && files.length > 0;
    }

    private void createDefaultQuestFiles(File dailyFolder) {
        plugin.saveResource("quests/examples.yml", false);
        PluginLogger.info("examples.yml created as default.");
        ensureStandardQuestFiles(QuestPeriod.DAILY, dailyFolder, NamingStyle.MODERN);
    }

    private NamingStyle detectNamingStyle(File dailyFolder) {
        final FileConfiguration mainConfig = plugin.getFilesManager().getConfigurationFile().getConfig();
        final ConfigurationSection configuredAmounts = mainConfig.getConfigurationSection("quest_periods.daily.quests_per_category") != null
                ? mainConfig.getConfigurationSection("quest_periods.daily.quests_per_category")
                : mainConfig.getConfigurationSection("quests_per_category");

        if (configuredAmounts != null) {
            for (String category : configuredAmounts.getKeys(false)) {
                if (isLegacyStandardCategory(category)) return NamingStyle.LEGACY;
            }
        }

        for (StandardQuestFile standard : STANDARD_QUEST_FILES) {
            if (new File(dailyFolder, standard.legacyName()).isFile()) return NamingStyle.LEGACY;
        }
        return NamingStyle.MODERN;
    }

    private boolean isLegacyStandardCategory(String category) {
        for (StandardQuestFile standard : STANDARD_QUEST_FILES) {
            final String legacyCategory = standard.legacyName().substring(0, standard.legacyName().length() - 4);
            if (legacyCategory.equalsIgnoreCase(category)) return true;
        }
        return false;
    }

    private void ensureStandardQuestFiles(QuestPeriod period, File questsFolder, NamingStyle style) {
        for (StandardQuestFile standard : STANDARD_QUEST_FILES) {
            final String destinationName = standard.destinationName(style);
            final File destination = new File(questsFolder, destinationName);
            if (destination.exists()) continue;

            try (InputStream source = plugin.getResource("quests/" + standard.resourceName())) {
                if (source == null) {
                    PluginLogger.error("Bundled quest template quests/" + standard.resourceName() + " is missing.");
                    continue;
                }
                Files.copy(source, destination.toPath());
                PluginLogger.info(period.getDisplayName() + " " + destinationName + " example created.");
            } catch (IOException exception) {
                PluginLogger.error("Could not create " + period.getDisplayName() + " example file " + destinationName + ".");
                PluginLogger.error(exception.getMessage());
            }
        }
    }

    private void copyLegacyPeriodFiles(File dailyFolder,
                                       File periodFolder,
                                       NamingStyle style,
                                       FileConfiguration mainConfig) {
        final Set<String> fileNames = new LinkedHashSet<>();
        for (StandardQuestFile standard : STANDARD_QUEST_FILES) {
            fileNames.add(standard.destinationName(style));
        }

        final ConfigurationSection configuredAmounts = mainConfig.getConfigurationSection("quests_per_category");
        if (configuredAmounts != null) {
            for (String category : configuredAmounts.getKeys(false)) fileNames.add(category + ".yml");
        }

        for (String fileName : fileNames) {
            final File source = new File(dailyFolder, fileName);
            final File destination = new File(periodFolder, fileName);
            if (!source.isFile() || destination.exists()) continue;

            try {
                Files.copy(source.toPath(), destination.toPath());
                PluginLogger.info("Copied legacy " + fileName + " into the preserved quest period.");
            } catch (IOException exception) {
                PluginLogger.error("Could not preserve legacy quest file " + fileName + ": " + exception.getMessage());
            }
        }
    }
}
