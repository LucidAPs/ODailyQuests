package com.lucidaps.odailyquests.tools;

import com.lucidaps.odailyquests.enums.QuestsMessages;
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

    /**
     * Set a runnable to reload quests at midnight.
     *
     * @param start date and time to start the task.
     */
    public TimerTask(LocalDateTime start) {
        scheduleNextExecution(start);
    }

    private void scheduleNextExecution(LocalDateTime start) {
        final RenewSchedule.Settings s = RenewSchedule.settings();
        if (!RenewSchedule.isValid(s)) {
            PluginLogger.error("Invalid renew schedule. Task not scheduled.");
            return;
        }

        final ZonedDateTime now = start.atZone(ZoneId.systemDefault()).withZoneSameInstant(s.zone());
        final ZonedDateTime next = RenewSchedule.nextExecutionAtOrAfter(now, s);

        long initialDelayMillis = Duration.between(
                ZonedDateTime.now(ZoneId.systemDefault()),
                next.withZoneSameInstant(ZoneId.systemDefault())
        ).toMillis();

        scheduledTask = TaskScheduler.scheduleSyncLater(this::executeAndReschedule, TaskScheduler.ticksFromMillis(initialDelayMillis));
    }

    private void executeAndReschedule() {
        PluginLogger.info("It's a new day. The player quests are being reloaded.");
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            final String msg = QuestsMessages.NEW_DAY.toString();
            if (msg != null) player.sendMessage(msg);

            final PlayerQuests playerQuests = QuestsManager.getActiveQuests().get(player.getName());
            if (playerQuests == null) {
                PluginLogger.warn("Skipping quest renewal for " + player.getName() + " because their quests are not loaded.");
                continue;
            }

            final int totalAchievedQuests = playerQuests.getTotalAchievedQuests();
            final Map<String, Integer> totalAchievedQuestsByCategory = playerQuests.getTotalAchievedQuestsByCategory();
            QuestLoaderUtils.loadNewPlayerQuests(player.getName(), QuestsManager.getActiveQuests(), totalAchievedQuestsByCategory, totalAchievedQuests);
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
