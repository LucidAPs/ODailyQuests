package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class QuestsPerCategory implements IConfigurable {

    private final ConfigurationFile configurationFile;
    private final Map<String, QuestAmountSetting> questsAmounts = new LinkedHashMap<>();

    public QuestsPerCategory(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Override
    public void load() {
        final FileConfiguration config = configurationFile.getConfig();
        questsAmounts.clear();

        ConfigurationSection section = config.getConfigurationSection("quests_per_category");
        if (section == null) {
            section = config.getConfigurationSection("quest_periods.daily.quests_per_category");
        }
        if (section == null) {
            PluginLogger.warn("No legacy/Daily quests_per_category section found; legacy category APIs will be empty.");
            return;
        }

        for (String category : section.getKeys(false)) {
            final Object rawValue = section.get(category);
            if (rawValue == null) {
                PluginLogger.error("Invalid quests_per_category entry for '" + category + "': value is missing.");
                Bukkit.getPluginManager().disablePlugin(ODailyQuests.INSTANCE);
                return;
            }

            try {
                final QuestAmountSetting setting = QuestAmountSetting.from(category, rawValue);
                questsAmounts.put(category, setting);
            } catch (IllegalArgumentException exception) {
                PluginLogger.error("Impossible to load quests. Disabling plugin.");
                Bukkit.getPluginManager().disablePlugin(ODailyQuests.INSTANCE);
                return;
            }
        }
    }

    public static int getAmountForCategory(String name) {
        final QuestAmountSetting setting = getInstance().questsAmounts.get(name);
        if (setting == null) return -1;
        final Integer amount = setting.getStaticAmount();
        return amount == null ? -1 : amount;
    }

    private static QuestsPerCategory getInstance() {
        return ConfigFactory.getConfig(QuestsPerCategory.class);
    }

    public static int getTotalQuestsAmount(Player player) {
        int total = 0;
        for (QuestAmountSetting setting : getInstance().questsAmounts.values()) {
            total += setting.resolve(player);
        }
        return total;
    }

    public static Map<String, QuestAmountSetting> getAllSettings() {
        return getInstance().questsAmounts;
    }

    public static Map<String, Integer> resolveAllFor(Player player) {
        final Map<String, Integer> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, QuestAmountSetting> entry : getInstance().questsAmounts.entrySet()) {
            resolved.put(entry.getKey(), entry.getValue().resolve(player));
        }
        return resolved;
    }

    public static int resolveAmountForCategory(String name, Player player) {
        final QuestAmountSetting setting = getInstance().questsAmounts.get(name);
        if (setting == null) {
            return 0;
        }
        return setting.resolve(player);
    }
}
