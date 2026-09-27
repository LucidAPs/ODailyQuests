package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.enums.QuestPeriod;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/** Resolves the single quest period used before independent periods were introduced. */
public final class LegacyPeriodResolver {

    private LegacyPeriodResolver() {
    }

    public static QuestPeriod resolve(ConfigurationSection config) {
        final QuestPeriod stored = QuestPeriod.fromString(
                config.getString("quest_periods.legacy_period")
        ).orElse(null);
        if (stored != null) return stored;

        if (config.contains("temporality_mode")) {
            return switch (config.getInt("temporality_mode", 1)) {
                case 2 -> QuestPeriod.WEEKLY;
                case 3 -> QuestPeriod.MONTHLY;
                default -> QuestPeriod.DAILY;
            };
        }

        final String interval = config.getString("renew_interval", "1d");
        if (interval == null) return QuestPeriod.DAILY;

        return switch (interval.replace(" ", "").toLowerCase(Locale.ROOT)) {
            case "7d" -> QuestPeriod.WEEKLY;
            case "30d", "31d" -> QuestPeriod.MONTHLY;
            default -> QuestPeriod.DAILY;
        };
    }
}
