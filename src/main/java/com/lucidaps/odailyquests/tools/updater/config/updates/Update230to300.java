package com.lucidaps.odailyquests.tools.updater.config.updates;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.updater.config.ConfigUpdater;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class Update230to300 extends ConfigUpdater {

    private static final String TEMPORALITY_MODE = "temporality_mode";
    private static final String RENEW_INTERVAL = "renew_interval";

    public Update230to300(ODailyQuests plugin) {
        super(plugin);
    }

    @Override
    public void apply(ODailyQuests plugin, String version) {

        if (config.getString("storage_mode").equalsIgnoreCase("h2")) {
            config.set("storage_mode", "SQLite");

            try {
                config.save(configFile);
                PluginLogger.warn("For stability reasons, the storage mode has been changed from H2 to SQLite.");
                PluginLogger.warn("The migration will be done automatically. If you encounter any issues, please contact support.");
            } catch (IOException e) {
                PluginLogger.error("Error while saving the configuration file.");
                PluginLogger.error(e.getMessage());
            }
        }

        setDefaultConfigItem("safety_mode", true, config, configFile, false);
        setDefaultConfigItem("join_message_delay", 1.0, config, configFile, false);
        setDefaultConfigItem("use_nexo", false, config, configFile, false);
        setDefaultConfigItem("renew_time", "00:00", config, configFile, false);
        setDefaultConfigItem("player_data_load_delay", 0.5, config, configFile, false);
        setDefaultConfigItem("complete_only_on_click", false, config, configFile, false);

        setDefaultConfigItem("toast.enabled", false, config, configFile, false);
        setDefaultConfigItem("toast.frame", "CHALLENGE", config, configFile, false);
        setDefaultConfigItem("toast.icon", "BOOK", config, configFile, false);
        setDefaultConfigItem("toast.text", "&b&lQuest completed!\\n%questName%", config, configFile, false);

        // as prefix is now used, set it empty for servers that already customized their messages
        setDefaultConfigItem("prefix", "", config, configFile, true);

        replaceTemporalityMode();
        replaceQuestsAmount();
        replaceInterfaceNames();
        replaceNPCNames();
        cleanupCategoryRewards();

        updateVersion(version);
    }

    private void replaceTemporalityMode() {
        final int currentMode = config.getInt(TEMPORALITY_MODE);
        switch (currentMode) {
            case 2 -> setDefaultConfigItem(RENEW_INTERVAL, "7d", config, configFile, false);
            case 3 -> setDefaultConfigItem(RENEW_INTERVAL, "30d", config, configFile, false);
            default -> setDefaultConfigItem(RENEW_INTERVAL, "1d", config, configFile, false);
        }

        removeConfigItem(TEMPORALITY_MODE, config, configFile);
        parameterReplaced(TEMPORALITY_MODE, RENEW_INTERVAL);
    }

    private void replaceQuestsAmount() {
        final int currentMode = config.getInt("quests_mode");

        if (currentMode == 1) {
            final int globalAmount = config.getInt("global_quests_amount");
            setDefaultConfigItem("quests_per_category." + categoryKey("global"), globalAmount, config, configFile, false);
        } else {
            final int easyAmount = config.getInt("easy_quests_amount");
            final int mediumAmount = config.getInt("medium_quests_amount");
            final int hardAmount = config.getInt("hard_quests_amount");

            if (easyAmount > 0) setDefaultConfigItem("quests_per_category." + categoryKey("easy"), easyAmount, config, configFile, false);
            if (mediumAmount > 0) setDefaultConfigItem("quests_per_category." + categoryKey("medium"), mediumAmount, config, configFile, false);
            if (hardAmount > 0) setDefaultConfigItem("quests_per_category." + categoryKey("hard"), hardAmount, config, configFile, false);
        }

        removeConfigItem("quests_mode", config, configFile);
        removeConfigItem("global_quests_amount", config, configFile);
        removeConfigItem("easy_quests_amount", config, configFile);
        removeConfigItem("medium_quests_amount", config, configFile);
        removeConfigItem("hard_quests_amount", config, configFile);
    }

    private void replaceInterfaceNames() {
        final String[] oldInterfaceNames = {"global_quests", "easy_quests", "medium_quests", "hard_quests"};
        final String[] baseCategoryNames = {"global", "easy", "medium", "hard"};

        final ConfigurationSection section = config.getConfigurationSection("interfaces");
        if (section == null) {
            PluginLogger.error("Interfaces section is missing in the configuration file. Disabling.");
            return;
        }

        int i = 0;
        for (String interfaceName : oldInterfaceNames) {
            final String newInterfaceName = categoryKey(baseCategoryNames[i]);
            final String inventoryName = section.getString(interfaceName + ".inventory_name");
            final String emptyItem = section.getString(interfaceName + ".empty_item");

            setDefaultConfigItem("interfaces." + newInterfaceName + ".inventory_name", inventoryName, config, configFile, false);
            setDefaultConfigItem("interfaces." + newInterfaceName + ".empty_item", emptyItem, config, configFile, false);

            removeConfigItem("interfaces." + interfaceName, config, configFile);
            parameterReplaced("interfaces." + interfaceName, "interfaces." + newInterfaceName);

            i++;
        }
    }

    private void replaceNPCNames() {
        final String[] oldNPCNames = {"name_player", "name_global", "name_easy", "name_medium", "name_hard"};
        final String[] baseCategoryNames = {"player", "global", "easy", "medium", "hard"};

        int i = 0;
        for (String NPCName : oldNPCNames) {
            final String newNPCName = i == 0 ? "player" : categoryKey(baseCategoryNames[i]);
            final String name = config.getString("npcs." + NPCName);

            setDefaultConfigItem("npcs." + newNPCName, name, config, configFile, false);
            removeConfigItem("npcs." + NPCName, config, configFile);

            parameterReplaced(NPCName, newNPCName);

            i++;
        }
    }

    private void cleanupCategoryRewards() {
        final ConfigurationSection rewards = config.getConfigurationSection("categories_rewards");
        if (rewards == null) return;

        for (String baseName : new String[]{"global", "easy", "medium", "hard"}) {
            final String targetName = categoryKey(baseName);
            if (targetName.equals(baseName) || !rewards.isConfigurationSection(baseName)
                    || rewards.isConfigurationSection(targetName)) continue;

            final ConfigurationSection source = rewards.getConfigurationSection(baseName);
            if (source != null) {
                rewards.set(targetName, new LinkedHashMap<>(source.getValues(false)));
                rewards.set(baseName, null);
            }
        }

        int kept = 0;

        for (String cat : new HashSet<>(rewards.getKeys(false))) {
            final ConfigurationSection catSec = rewards.getConfigurationSection(cat);
            if (catSec == null) {
                rewards.set(cat, null);
                continue;
            }

            final boolean enabled = catSec.getBoolean("enabled", true);

            if (!enabled) {
                rewards.set(cat, null);
            } else {
                if (catSec.contains("enabled")) {
                    catSec.set("enabled", null);
                }
                kept++;
            }
        }

        if (kept == 0) {
            config.set("categories_rewards", new ArrayList<>());
            PluginLogger.warn("No active category rewards found. Set categories_rewards to an empty list [] to avoid load errors.");
        }
    }

    private String categoryKey(String baseName) {
        final File questsFolder = new File(ODailyQuests.INSTANCE.getDataFolder(), "quests");
        if (new File(questsFolder, baseName + "Quests.yml").isFile()) return baseName + "Quests";
        return baseName;
    }
}
