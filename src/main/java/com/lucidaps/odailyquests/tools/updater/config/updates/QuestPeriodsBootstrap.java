package com.lucidaps.odailyquests.tools.updater.config.updates;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.essentials.LegacyPeriodResolver;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.updater.config.ConfigUpdater;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Adds independent periods while preserving the server's former single period. */
public final class QuestPeriodsBootstrap extends ConfigUpdater {

    public QuestPeriodsBootstrap(ODailyQuests plugin) {
        super(plugin);
    }

    public void applyIfMissing() {
        if (config.isConfigurationSection("quest_periods")) {
            addLegacyPeriodMarkerIfMissing();
            return;
        }

        final QuestPeriod legacyPeriod = LegacyPeriodResolver.resolve(config);
        final Map<String, Object> amounts = new LinkedHashMap<>();
        final ConfigurationSection legacyAmounts = config.getConfigurationSection("quests_per_category");
        if (legacyAmounts != null) amounts.putAll(legacyAmounts.getValues(false));

        config.set("quest_periods.legacy_period", legacyPeriod.getConfigKey());
        for (QuestPeriod period : QuestPeriod.values()) {
            addPeriod(period, period == legacyPeriod, amounts);
        }

        try {
            config.save(configFile);
            PluginLogger.info("Added independent Daily, Weekly and Monthly settings to config.yml. Preserved "
                    + legacyPeriod.getDisplayName() + " as the previously active quest period; the other periods remain disabled.");
        } catch (IOException exception) {
            PluginLogger.error("Could not save the independent quest-period settings: " + exception.getMessage());
        }
    }

    private void addLegacyPeriodMarkerIfMissing() {
        if (config.contains("quest_periods.legacy_period")) return;

        final QuestPeriod legacyPeriod = LegacyPeriodResolver.resolve(config);
        config.set("quest_periods.legacy_period", legacyPeriod.getConfigKey());
        try {
            config.save(configFile);
            PluginLogger.info("Recorded " + legacyPeriod.getDisplayName()
                    + " as the pre-multi-period compatibility source.");
        } catch (IOException exception) {
            PluginLogger.error("Could not save the legacy quest-period marker: " + exception.getMessage());
        }
    }

    private void addPeriod(QuestPeriod period, boolean enabled, Map<String, Object> amounts) {
        final String base = "quest_periods." + period.getConfigKey();
        config.set(base + ".enabled", enabled);
        config.set(base + ".renewal.timestamp_mode", config.getInt("timestamp_mode", 1));
        config.set(base + ".renewal.time", config.getString("renew_time", "00:00"));
        config.set(base + ".renewal.time_zone", config.getString("renew_time_zone", "SystemDefault"));
        if (period == QuestPeriod.WEEKLY) config.set(base + ".renewal.day_of_week", "MONDAY");
        if (period == QuestPeriod.MONTHLY) config.set(base + ".renewal.day_of_month", 1);
        config.set(base + ".quests_per_category", new LinkedHashMap<>(amounts));
        config.set(base + ".reroll_maximum", config.getInt("reroll_maximum", -1));

        if (enabled) {
            copySection("global_reward", base + ".global_reward");
            copySection("categories_rewards", base + ".categories_rewards");
        } else {
            config.set(base + ".global_reward.enabled", false);
            config.set(base + ".categories_rewards", new LinkedHashMap<>());
        }
    }

    private void copySection(String sourcePath, String destinationPath) {
        final ConfigurationSection source = config.getConfigurationSection(sourcePath);
        if (source == null) {
            config.set(destinationPath, new LinkedHashMap<>());
            return;
        }
        config.set(destinationPath, toMap(source));
    }

    private Map<String, Object> toMap(ConfigurationSection section) {
        final Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            final ConfigurationSection child = section.getConfigurationSection(key);
            values.put(key, child == null ? section.get(key) : toMap(child));
        }
        return values;
    }

    @Override
    public void apply(ODailyQuests plugin, String version) {
        applyIfMissing();
    }
}
