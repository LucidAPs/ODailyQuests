package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the independent Daily, Weekly and Monthly quest-period settings.
 * Legacy top-level settings remain the fallback for Daily so existing servers
 * can upgrade without rewriting their configuration first.
 */
public final class QuestPeriods implements IConfigurable {

    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("h:mma"),
            DateTimeFormatter.ofPattern("ha"),
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("H")
    );

    public record Settings(
            boolean enabled,
            int timestampMode,
            LocalTime renewTime,
            ZoneId zoneId,
            DayOfWeek dayOfWeek,
            int dayOfMonth,
            int rerollMaximum,
            Map<String, QuestAmountSetting> questAmounts
    ) {
    }

    private final ConfigurationFile configurationFile;
    private final EnumMap<QuestPeriod, Settings> settings = new EnumMap<>(QuestPeriod.class);
    private QuestPeriod legacyPeriod = QuestPeriod.DAILY;

    public QuestPeriods(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Override
    public void load() {
        settings.clear();
        final FileConfiguration config = configurationFile.getConfig();
        legacyPeriod = LegacyPeriodResolver.resolve(config);
        final ConfigurationSection periods = config.getConfigurationSection("quest_periods");

        for (QuestPeriod period : QuestPeriod.values()) {
            final ConfigurationSection section = periods == null
                    ? null
                    : periods.getConfigurationSection(period.getConfigKey());
            settings.put(period, loadPeriod(config, section, period));
        }

        if (getEnabledPeriods().isEmpty()) {
            PluginLogger.error("At least one quest period must be enabled. Enabling Daily as a fallback.");
            final Settings daily = settings.get(QuestPeriod.DAILY);
            settings.put(QuestPeriod.DAILY, new Settings(
                    true,
                    daily.timestampMode(),
                    daily.renewTime(),
                    daily.zoneId(),
                    daily.dayOfWeek(),
                    daily.dayOfMonth(),
                    daily.rerollMaximum(),
                    daily.questAmounts()
            ));
        }
    }

    private Settings loadPeriod(FileConfiguration config, ConfigurationSection section, QuestPeriod period) {
        final boolean legacyDaily = period == QuestPeriod.DAILY && section == null;
        final boolean enabled = section != null
                ? section.getBoolean("enabled", period == QuestPeriod.DAILY)
                : period == QuestPeriod.DAILY;

        final ConfigurationSection renewal = section == null ? null : section.getConfigurationSection("renewal");
        final int timestampMode = renewal != null
                ? renewal.getInt("timestamp_mode", config.getInt("timestamp_mode", 1))
                : config.getInt("timestamp_mode", 1);
        final String timeRaw = renewal != null
                ? renewal.getString("time", "00:00")
                : config.getString("renew_time", "00:00");
        final String zoneRaw = renewal != null
                ? renewal.getString("time_zone", "SystemDefault")
                : config.getString("renew_time_zone", "SystemDefault");

        final DayOfWeek dayOfWeek = parseDayOfWeek(
                renewal == null ? "MONDAY" : renewal.getString("day_of_week", "MONDAY"),
                period
        );
        final int configuredDay = renewal == null ? 1 : renewal.getInt("day_of_month", 1);
        final int dayOfMonth = Math.max(1, Math.min(configuredDay, 31));

        final int rerollMaximum = section != null
                ? section.getInt("reroll_maximum", config.getInt("reroll_maximum", -1))
                : config.getInt("reroll_maximum", -1);

        final ConfigurationSection amountsSection = section == null
                ? config.getConfigurationSection("quests_per_category")
                : section.getConfigurationSection("quests_per_category");

        final Map<String, QuestAmountSetting> amounts = new LinkedHashMap<>();
        if (amountsSection != null) {
            for (String category : amountsSection.getKeys(false)) {
                final Object raw = amountsSection.get(category);
                if (raw == null) continue;
                try {
                    amounts.put(category, QuestAmountSetting.from(category, raw));
                } catch (IllegalArgumentException exception) {
                    PluginLogger.error("Invalid quest amount for " + period.getDisplayName() + " category '" + category + "'.");
                }
            }
        } else if (enabled || legacyDaily) {
            PluginLogger.error("No quests_per_category section found for " + period.getDisplayName() + " quests.");
        }

        return new Settings(
                enabled,
                timestampMode == 2 ? 2 : 1,
                parseTime(timeRaw),
                parseZone(zoneRaw),
                dayOfWeek,
                dayOfMonth,
                rerollMaximum,
                Map.copyOf(amounts)
        );
    }

    private LocalTime parseTime(String raw) {
        final String value = raw == null ? "00:00" : raw.trim().toUpperCase();
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        PluginLogger.warn("Invalid quest-period renewal time '" + raw + "'. Using 00:00.");
        return LocalTime.MIDNIGHT;
    }

    private ZoneId parseZone(String raw) {
        if (raw == null || raw.equalsIgnoreCase("SystemDefault")) return ZoneId.systemDefault();
        try {
            return ZoneId.of(raw);
        } catch (DateTimeException exception) {
            PluginLogger.warn("Invalid quest-period time zone '" + raw + "'. Using the system default.");
            return ZoneId.systemDefault();
        }
    }

    private DayOfWeek parseDayOfWeek(String raw, QuestPeriod period) {
        try {
            return DayOfWeek.valueOf(raw == null ? "MONDAY" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            PluginLogger.warn("Invalid day_of_week for " + period.getDisplayName() + ". Using MONDAY.");
            return DayOfWeek.MONDAY;
        }
    }

    public static Settings get(QuestPeriod period) {
        return getInstance().settings.get(period);
    }

    public static boolean isEnabled(QuestPeriod period) {
        final Settings value = get(period);
        return value != null && value.enabled();
    }

    public static List<QuestPeriod> getEnabledPeriods() {
        return getInstance().settings.entrySet().stream()
                .filter(entry -> entry.getValue().enabled())
                .map(Map.Entry::getKey)
                .toList();
    }

    public static QuestPeriod getDefaultPeriod() {
        return isEnabled(QuestPeriod.DAILY) ? QuestPeriod.DAILY : getEnabledPeriods().getFirst();
    }

    /** Period that owned the server's quest data before multi-period support. */
    public static QuestPeriod getLegacyPeriod() {
        return getInstance().legacyPeriod;
    }

    public static Map<String, QuestAmountSetting> getQuestAmounts(QuestPeriod period) {
        final Settings value = get(period);
        return value == null ? Map.of() : value.questAmounts();
    }

    public static Map<String, Integer> resolveQuestAmounts(QuestPeriod period, Player player) {
        final Map<String, Integer> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, QuestAmountSetting> entry : getQuestAmounts(period).entrySet()) {
            resolved.put(entry.getKey(), entry.getValue().resolve(player));
        }
        return resolved;
    }

    public static int getTotalQuestAmount(QuestPeriod period, Player player) {
        return resolveQuestAmounts(period, player).values().stream().mapToInt(Integer::intValue).sum();
    }

    public static ZonedDateTime nextGlobalRenewal(QuestPeriod period, ZonedDateTime now) {
        final Settings value = get(period);
        final ZonedDateTime localNow = now.withZoneSameInstant(value.zoneId());

        return switch (period) {
            case DAILY -> {
                ZonedDateTime candidate = localNow.toLocalDate().atTime(value.renewTime()).atZone(value.zoneId());
                if (!candidate.isAfter(localNow)) candidate = candidate.plusDays(1);
                yield candidate;
            }
            case WEEKLY -> {
                LocalDate date = localNow.toLocalDate().with(TemporalAdjusters.nextOrSame(value.dayOfWeek()));
                ZonedDateTime candidate = date.atTime(value.renewTime()).atZone(value.zoneId());
                if (!candidate.isAfter(localNow)) candidate = candidate.plusWeeks(1);
                yield candidate;
            }
            case MONTHLY -> {
                YearMonth month = YearMonth.from(localNow);
                int day = Math.min(value.dayOfMonth(), month.lengthOfMonth());
                ZonedDateTime candidate = month.atDay(day).atTime(value.renewTime()).atZone(value.zoneId());
                if (!candidate.isAfter(localNow)) {
                    month = month.plusMonths(1);
                    day = Math.min(value.dayOfMonth(), month.lengthOfMonth());
                    candidate = month.atDay(day).atTime(value.renewTime()).atZone(value.zoneId());
                }
                yield candidate;
            }
        };
    }

    public static boolean shouldRenew(QuestPeriod period, long timestamp) {
        final Settings value = get(period);
        if (value == null || !value.enabled()) return false;

        final ZonedDateTime last = Instant.ofEpochMilli(timestamp).atZone(value.zoneId());
        final ZonedDateTime now = ZonedDateTime.now(value.zoneId());
        if (value.timestampMode() == 2) {
            final ZonedDateTime expiry = switch (period) {
                case DAILY -> last.plusDays(1);
                case WEEKLY -> last.plusWeeks(1);
                case MONTHLY -> last.plusMonths(1);
            };
            return !expiry.isAfter(now);
        }

        return !nextGlobalRenewal(period, last).isAfter(now);
    }

    public static long millisUntilRenewal(QuestPeriod period, long timestamp) {
        final Settings value = get(period);
        if (value == null) return 0L;
        final ZonedDateTime now = ZonedDateTime.now(value.zoneId());
        final ZonedDateTime next = value.timestampMode() == 2
                ? switch (period) {
                    case DAILY -> Instant.ofEpochMilli(timestamp).atZone(value.zoneId()).plusDays(1);
                    case WEEKLY -> Instant.ofEpochMilli(timestamp).atZone(value.zoneId()).plusWeeks(1);
                    case MONTHLY -> Instant.ofEpochMilli(timestamp).atZone(value.zoneId()).plusMonths(1);
                }
                : nextGlobalRenewal(period, now);
        return Math.max(0L, java.time.Duration.between(now, next).toMillis());
    }

    private static QuestPeriods getInstance() {
        return ConfigFactory.getConfig(QuestPeriods.class);
    }
}
