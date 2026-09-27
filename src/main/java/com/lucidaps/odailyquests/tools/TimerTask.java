package com.lucidaps.odailyquests.tools;

import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.quests.player.progression.QuestLoaderUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.*;
import java.util.Map;

public class TimerTask {

    private BukkitTask scheduledTask;
    private final QuestPeriod period;

    /**
     * Set a runnable to reload quests at midnight.
     *
     * @param start date and time to start the task.
     */
    public TimerTask(LocalDateTime start) {
        this(QuestPeriod.DAILY, start);
    }

    public TimerTask(QuestPeriod period, LocalDateTime start) {
        this.period = period;
        scheduleNextExecution(start);
    }

    private void scheduleNextExecution(LocalDateTime start) {
        final QuestPeriods.Settings settings = QuestPeriods.get(period);
        if (settings == null || !settings.enabled() || settings.timestampMode() != 1) {
            return;
        }

        final ZonedDateTime now = start.atZone(ZoneId.systemDefault()).withZoneSameInstant(settings.zoneId());
        final ZonedDateTime next = QuestPeriods.nextGlobalRenewal(period, now);

        long initialDelayMillis = Duration.between(
                ZonedDateTime.now(ZoneId.systemDefault()),
                next.withZoneSameInstant(ZoneId.systemDefault())
        ).toMillis();

        scheduledTask = TaskScheduler.scheduleSyncLater(this::executeAndReschedule, TaskScheduler.ticksFromMillis(initialDelayMillis));
    }

    private void executeAndReschedule() {
        PluginLogger.info("The " + period.getDisplayName() + " player quests are being reloaded.");
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            final String msg = QuestsMessages.NEW_DAY.toString();
            if (msg != null) player.sendMessage(msg.replace("%period%", period.getDisplayName()));

            final PlayerQuests playerQuests = QuestsManager.getPlayerQuests(player.getName(), period);
            if (playerQuests == null) {
                PluginLogger.warn("Skipping quest renewal for " + player.getName() + " because their quests are not loaded.");
                continue;
            }

            final int totalAchievedQuests = playerQuests.getTotalAchievedQuests();
            final Map<String, Integer> totalAchievedQuestsByCategory = playerQuests.getTotalAchievedQuestsByCategory();
            QuestLoaderUtils.loadNewPlayerQuests(player.getName(), period, totalAchievedQuestsByCategory, totalAchievedQuests);
        }

        scheduleNextExecution(LocalDateTime.now());
    }

    public void reload() {
       cancel();
        scheduleNextExecution(LocalDateTime.now());
    }

    private void cancel() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    public void stop() {
        cancel();
    }
}
