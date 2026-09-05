package com.lucidaps.odailyquests.tools;

import com.lucidaps.odailyquests.configuration.essentials.RenewInterval;
import com.lucidaps.odailyquests.configuration.essentials.RenewTime;
import com.lucidaps.odailyquests.configuration.essentials.TimestampMode;

import java.time.*;

/**
 * Utility class responsible for computing quest renewal schedules.
 *
 * <p>It centralizes all time-based calculations related to renewals
 * (next execution, elapsed intervals, offline catch-up logic),
 * ensuring consistent behavior across the plugin.
 *
 * <p>The schedule is defined by:
 * <ul>
 *   <li>a daily anchor time ({@link LocalTime})</li>
 *   <li>a renewal interval ({@link Duration})</li>
 *   <li>a time zone ({@link ZoneId})</li>
 *   <li>a timestamp mode (raw int, handled elsewhere)</li>
 * </ul>
 *
 * <p>This class is stateless and cannot be instantiated.
 */
public final class RenewSchedule {

    private RenewSchedule() {
        // Utility class
    }

    /**
     * Immutable container holding all renewal schedule parameters.
     *
     * @param renewTime the anchor time of day for renewals
     * @param interval  the renewal interval duration
     * @param zone      the time zone used for all calculations
     * @param mode      the timestamp mode applied to the schedule
     */
    public record Settings(LocalTime renewTime, Duration interval, ZoneId zone, int mode) {
    }


    /**
     * Builds a {@link Settings} instance from the current plugin configuration.
     *
     * @return the configured renewal schedule settings
     */
    public static Settings settings() {
        return new Settings(
                RenewTime.getRenewTime(),
                RenewInterval.getRenewInterval(),
                RenewTime.getZoneId(),
                TimestampMode.getTimestampMode()
        );
    }

    /**
     * Validates that the given settings are usable for scheduling calculations.
     *
     * @param s the schedule settings to validate
     * @return {@code true} if the settings are valid, {@code false} otherwise
     */
    public static boolean isValid(Settings s) {
        return s.interval() != null && !s.interval().isZero() && !s.interval().isNegative()
                && s.renewTime() != null
                && s.zone() != null;
    }

    /**
     * Computes the next scheduled execution time greater than or equal to {@code now}.
     * <p>
     * The calculation uses an anchor defined as "today at renewTime",
     * then applies {@code k * interval} to reach the first execution ≥ now.
     * </p>
     *
     * <p>
     * This method is the single source of truth for renewal timing
     * and is used by both timer tasks and remaining-time displays.
     * </p>
     *
     * @param now the current date-time
     * @param s   the schedule settings
     * @return the next scheduled execution time
     */
    public static ZonedDateTime nextExecutionAtOrAfter(ZonedDateTime now, Settings s) {
        ZonedDateTime anchor = now.with(s.renewTime());

        if (anchor.isAfter(now) || anchor.isEqual(now)) {
            return anchor;
        }

        long intervalMillis = s.interval().toMillis();
        long elapsedMillis = Duration.between(anchor, now).toMillis();

        long k = (elapsedMillis / intervalMillis) + 1;
        return anchor.plus(s.interval().multipliedBy(k));
    }

    /**
     * Determines whether at least one scheduled execution occurred
     * after {@code lastRenew} and before or at {@code now}.
     * <p>
     * This method is intended for offline catch-up logic
     * (e.g. when a player reconnects after being offline).
     * </p>
     *
     * @param lastRenew the last recorded renewal time
     * @param now       the current date-time
     * @param s         the schedule settings
     * @return {@code true} if a renewal should occur, {@code false} otherwise
     */
    public static boolean shouldRenewSince(ZonedDateTime lastRenew, ZonedDateTime now, Settings s) {
        // Next tick strictly after lastRenew
        ZonedDateTime nextAfterLast = nextExecutionAtOrAfter(lastRenew, s);
        if (nextAfterLast.isEqual(lastRenew)) {
            nextAfterLast = nextAfterLast.plus(s.interval());
        }
        return !nextAfterLast.isAfter(now); // nextAfterLast <= now
    }


    /**
     * Returns the number of milliseconds remaining until the next scheduled execution.
     *
     * @param now the current date-time
     * @param s   the schedule settings
     * @return the remaining time in milliseconds (never negative)
     */
    public static long millisUntilNext(ZonedDateTime now, Settings s) {
        ZonedDateTime next = nextExecutionAtOrAfter(now, s);
        return Math.max(0L, Duration.between(now, next).toMillis());
    }
}
