package com.lucidaps.odailyquests.tools.updater.config.updates;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.updater.config.ConfigUpdater;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Adds the independent-period section to legacy configs without enabling new periods. */
public final class QuestPeriodsBootstrap extends ConfigUpdater {

    public QuestPeriodsBootstrap(ODailyQuests plugin) {
        super(plugin);
    }

    public void applyIfMissing() {
        if (config.isConfigurationSection("quest_periods")) return;

        final Map<String, Object> amounts = new LinkedHashMap<>();
        final ConfigurationSection legacyAmounts = config.getConfigurationSection("quests_per_category");
        if (legacyAmounts != null) amounts.putAll(legacyAmounts.getValues(false));

        config.set("quest_periods.daily.enabled", true);
        config.set("quest_periods.daily.renewal.timestamp_mode", config.getInt("timestamp_mode", 1));
        config.set("quest_periods.daily.renewal.time", config.getString("renew_time", "00:00"));
        config.set("quest_periods.daily.renewal.time_zone", config.getString("renew_time_zone", "SystemDefault"));
        config.set("quest_periods.daily.quests_per_category", amounts);
        config.set("quest_periods.daily.reroll_maximum", config.getInt("reroll_maximum", -1));

        addDisabledPeriod("weekly", amounts);
        config.set("quest_periods.weekly.renewal.day_of_week", "MONDAY");
        addDisabledPeriod("monthly", amounts);
        config.set("quest_periods.monthly.renewal.day_of_month", 1);

        try {
            config.save(configFile);
            PluginLogger.info("Added independent Daily, Weekly and Monthly settings to config.yml. Weekly and Monthly remain disabled until explicitly enabled.");
        } catch (IOException exception) {
            PluginLogger.error("Could not save the independent quest-period settings: " + exception.getMessage());
        }
    }

    private void addDisabledPeriod(String key, Map<String, Object> amounts) {
        final String base = "quest_periods." + key;
        config.set(base + ".enabled", false);
        config.set(base + ".renewal.timestamp_mode", 1);
        config.set(base + ".renewal.time", "00:00");
        config.set(base + ".renewal.time_zone", "SystemDefault");
        config.set(base + ".quests_per_category", new LinkedHashMap<>(amounts));
        config.set(base + ".reroll_maximum", config.getInt("reroll_maximum", -1));
        config.set(base + ".global_reward.enabled", false);
        config.set(base + ".categories_rewards", new LinkedHashMap<>());
    }

    @Override
    public void apply(ODailyQuests plugin, String version) {
        applyIfMissing();
    }
}
