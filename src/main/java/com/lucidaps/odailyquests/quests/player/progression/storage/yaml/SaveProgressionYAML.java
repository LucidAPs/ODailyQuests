package com.lucidaps.odailyquests.quests.player.progression.storage.yaml;

import com.lucidaps.odailyquests.configuration.essentials.Logs;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.files.implementations.ProgressionFile;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.player.progression.storage.PlayerQuestData;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.TaskScheduler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.util.Map;

public class SaveProgressionYAML {

    private final ProgressionFile progressionFile;

    public SaveProgressionYAML(ProgressionFile progressionFile) {
        this.progressionFile = progressionFile;
    }

    public void saveProgression(String playerName, String playerUuid, PlayerQuestData data, boolean forceSync) {
        if (data == null) {
            PluginLogger.warn("Impossible to save progression for player " + playerName + " because their quest data is null.");
            return;
        }

        if (forceSync) updateFile(playerName, playerUuid, data);
        else TaskScheduler.runAsync(() -> updateFile(playerName, playerUuid, data));
    }

    private void updateFile(String playerName, String playerUuid, PlayerQuestData data) {
        final FileConfiguration config = progressionFile.getConfig();
        config.set(playerUuid + ".overallTotalAchievedQuests", data.overallLifetimeTotal());
        // Retained for the existing all-time leaderboard and downgrade-friendly backups.
        config.set(playerUuid + ".totalAchievedQuests", data.overallLifetimeTotal());

        for (Map.Entry<QuestPeriod, PlayerQuests> periodEntry : data.periods().entrySet()) {
            savePeriod(config, playerUuid, periodEntry.getKey(), periodEntry.getValue());
        }

        if (Logs.isEnabled()) PluginLogger.info(playerName + "'s data saved.");

        try {
            config.save(progressionFile.getFile());
        } catch (IOException exception) {
            PluginLogger.error("An error happened while saving the progression file.");
            PluginLogger.error(exception.getMessage());
        }
    }

    private void savePeriod(FileConfiguration config, String playerUuid, QuestPeriod period, PlayerQuests playerQuests) {
        final String base = playerUuid + ".periods." + period.getConfigKey();
        config.set(base, null);
        config.set(base + ".timestamp", playerQuests.getTimestamp());
        config.set(base + ".achievedQuests", playerQuests.getAchievedQuests());
        config.set(base + ".totalAchievedQuests", playerQuests.getTotalAchievedQuests());
        config.set(base + ".recentRerolls", playerQuests.getRecentlyRolled());

        int slot = 1;
        for (Map.Entry<AbstractQuest, Progression> entry : playerQuests.getQuests().entrySet()) {
            final AbstractQuest quest = entry.getKey();
            final Progression progression = entry.getValue();
            final ConfigurationSection questSection = config.createSection(base + ".quests." + slot++);
            questSection.set("index", quest.getQuestIndex());
            questSection.set("category", quest.getCategoryName());
            questSection.set("progression", progression.getAdvancement());
            questSection.set("requiredAmount", progression.getRequiredAmount());
            questSection.set("rewardAmount", progression.getRewardAmount());
            questSection.set("selectedRequired", progression.getSelectedRequiredIndex());
            questSection.set("isAchieved", progression.isAchieved());
        }

        final ConfigurationSection stats = config.createSection(base + ".totalAchievedQuestsByCategory");
        for (Map.Entry<String, Integer> entry : playerQuests.getTotalAchievedQuestsByCategory().entrySet()) {
            stats.set(entry.getKey(), entry.getValue());
        }
    }
}
