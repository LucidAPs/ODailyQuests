package com.lucidaps.odailyquests.tools;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern P = Pattern.compile("(\\d+)([dhm])");
    private DurationParser() {
    }

    public static Duration parseDuration(String interval) {
        final Matcher matcher = P.matcher(interval);
        Duration duration = Duration.ZERO;

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            duration = switch (unit) {
                case "d" -> duration.plus(value, ChronoUnit.DAYS);
                case "h" -> duration.plus(value, ChronoUnit.HOURS);
                case "m" -> duration.plus(value, ChronoUnit.MINUTES);
                default -> duration;
            };
        }

        return duration;
    }
}
