package com.lucidaps.odailyquests.tools;

import com.lucidaps.odailyquests.quests.player.QuestsManager;

import com.lucidaps.odailyquests.configuration.essentials.RenewInterval;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.configuration.essentials.TimestampMode;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;

import java.time.*;

public class TimeRemain {

    private TimeRemain() {
    }

    /**
     * Get the time remaining before the next quests draw (same schedule as TimerTask).
     *
     * @param playerName player to consider.
     * @return the time remaining as a formatted String.
     */
    public static String timeRemain(String playerName) {
        return timeRemain(playerName, QuestPeriods.getDefaultPeriod());
    }

    public static String timeRemain(String playerName, QuestPeriod period) {
        final PlayerQuests playerQuests = QuestsManager.getPlayerQuests(playerName, period);
        final long timestamp = playerQuests == null ? System.currentTimeMillis() : playerQuests.getTimestamp();
        return formatTimeRemain(QuestPeriods.millisUntilRenewal(period, timestamp));
    }

    private static String formatTimeRemain(long rest) {
        final String d = RenewInterval.getDayInitial();
        final String h = RenewInterval.getHourInitial();
        final String m = RenewInterval.getMinuteInitial();

        final int days = (int) (rest / (1000L * 60 * 60 * 24));
        final int hours = (int) ((rest / (1000L * 60 * 60)) % 24);
        final int minutes = (int) ((rest / (1000L * 60)) % 60);

        if (days != 0) return String.format("%d%s%d%s%d%s", days, d, hours, h, minutes, m);
        if (hours != 0) return String.format("%d%s%d%s", hours, h, minutes, m);
        if (minutes != 0) return String.format("%d%s", minutes, m);
        return RenewInterval.getFewSeconds();
    }
}
