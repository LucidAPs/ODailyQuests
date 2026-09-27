package com.lucidaps.odailyquests.enums;

import java.util.Locale;
import java.util.Optional;

/**
 * The fixed renewal periods supported by ODailyQuests.
 */
public enum QuestPeriod {
    DAILY,
    WEEKLY,
    MONTHLY;

    public String getConfigKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getDisplayName() {
        final String key = getConfigKey();
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    public static Optional<QuestPeriod> fromString(String value) {
        if (value == null || value.isBlank()) return Optional.empty();

        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
