package com.lucidaps.odailyquests.quests.player.progression.storage.yaml;

import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.files.implementations.ProgressionFile;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.player.progression.ProgressionLoader;
import com.lucidaps.odailyquests.quests.player.progression.QuestLoaderUtils;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.tools.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoadProgressionYAML extends ProgressionLoader {

    private final ProgressionFile progressionFile;

    public LoadProgressionYAML(ProgressionFile progressionFile) {
        this.progressionFile = progressionFile;
    }

    public void loadPlayerQuests(String playerName, Map<String, PlayerQuests> ignored, boolean sendStatusMessage) {
        TaskScheduler.runSync(() -> loadSync(playerName, sendStatusMessage));
    }

    private void loadSync(String playerName, boolean sendStatusMessage) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            handlePlayerDisconnected(playerName);
            return;
        }

        final FileConfiguration config = progressionFile.getConfig();
        final ConfigurationSection playerSection = config.getConfigurationSection(player.getUniqueId().toString());
        if (playerSection == null) {
            drawAllPeriods(playerName, 0);
            return;
        }

        final int overallTotal = playerSection.contains("overallTotalAchievedQuests")
                ? playerSection.getInt("overallTotalAchievedQuests")
                : playerSection.getInt("totalAchievedQuests");
        final ConfigurationSection periodsSection = playerSection.getConfigurationSection("periods");
        final QuestPeriod legacyPeriod = QuestPeriods.getLegacyPeriod();

        for (QuestPeriod period : QuestPeriods.getEnabledPeriods()) {
            final ConfigurationSection periodSection = periodsSection == null
                    ? (period == legacyPeriod ? playerSection : null)
                    : periodsSection.getConfigurationSection(period.getConfigKey());

            if (periodSection == null) {
                QuestLoaderUtils.loadNewPlayerQuests(playerName, period, new HashMap<>(), 0, false);
                continue;
            }

            loadPeriod(player, period, periodSection);
        }

        QuestsManager.markPlayerLoaded(playerName, overallTotal);
        if (sendStatusMessage) {
            final QuestPeriod first = QuestPeriods.getEnabledPeriods().getFirst();
            final PlayerQuests quests = QuestsManager.getPlayerQuests(playerName, first);
            if (quests != null) sendQuestStatusMessage(player, quests.getAchievedQuests(), quests);
        }
    }

    private void loadPeriod(Player player, QuestPeriod period, ConfigurationSection section) {
        final String playerName = player.getName();
        final long timestamp = section.getLong("timestamp", System.currentTimeMillis());
        final int achieved = section.getInt("achievedQuests");
        final int periodTotal = section.getInt("totalAchievedQuests");
        final int rerolls = section.contains("recentRerolls")
                ? section.getInt("recentRerolls")
                : section.getInt("recentRolls");
        final Map<String, Integer> categoryTotals = loadCategoryTotals(section);

        if (QuestLoaderUtils.checkTimestamp(period, timestamp)) {
            QuestLoaderUtils.loadNewPlayerQuests(playerName, period, categoryTotals, periodTotal, false);
            return;
        }

        final LinkedHashMap<AbstractQuest, Progression> quests = loadQuests(playerName, period, section);
        if (quests == null || quests.isEmpty()) {
            QuestLoaderUtils.loadNewPlayerQuests(playerName, period, categoryTotals, periodTotal, false);
            return;
        }

        final PlayerQuests playerQuests = new PlayerQuests(period, timestamp, quests);
        playerQuests.setAchievedQuests(achieved);
        playerQuests.setTotalAchievedQuests(periodTotal);
        playerQuests.setRecentRerolls(rerolls);
        playerQuests.setTotalAchievedQuestsByCategory(categoryTotals);
        QuestsManager.registerPlayerPeriod(playerName, period, playerQuests);
    }

    private Map<String, Integer> loadCategoryTotals(ConfigurationSection section) {
        final Map<String, Integer> totals = new HashMap<>();
        final ConfigurationSection stats = section.getConfigurationSection("totalAchievedQuestsByCategory");
        if (stats != null) {
            for (String category : stats.getKeys(false)) totals.put(category, stats.getInt(category));
        }
        return totals;
    }

    private LinkedHashMap<AbstractQuest, Progression> loadQuests(String playerName, QuestPeriod period, ConfigurationSection section) {
        final ConfigurationSection stored = section.getConfigurationSection("quests");
        if (stored == null) return null;

        final LinkedHashMap<AbstractQuest, Progression> quests = new LinkedHashMap<>();
        for (String key : stored.getKeys(false)) {
            final int questIndex = stored.getInt(key + ".index");
            final String category = stored.getString(key + ".category");
            final int requiredAmount = stored.getInt(key + ".requiredAmount");
            final int selectedRequired = stored.getInt(key + ".selectedRequired", -1);
            if (requiredAmount == 0) return null;

            final AbstractQuest quest = QuestLoaderUtils.findQuest(period, playerName, category, questIndex, Integer.parseInt(key));
            if (quest == null || isSelectedRequiredInvalid(quest, selectedRequired, playerName)) return null;
            if (!quest.isRandomRequiredAmount() && requiredAmount != Integer.parseInt(quest.getRequiredAmountRaw())) return null;

            final double rewardAmount = stored.isSet(key + ".rewardAmount")
                    ? stored.getDouble(key + ".rewardAmount")
                    : quest.getReward().resolveRewardAmount();
            final Progression progression = new Progression(
                    requiredAmount,
                    rewardAmount,
                    stored.getInt(key + ".progression"),
                    stored.getBoolean(key + ".isAchieved")
            );
            if (selectedRequired != -1) progression.setSelectedRequiredIndex(selectedRequired);
            quests.put(quest, progression);
        }
        return quests;
    }

    private void drawAllPeriods(String playerName, int overallTotal) {
        for (QuestPeriod period : QuestPeriods.getEnabledPeriods()) {
            QuestLoaderUtils.loadNewPlayerQuests(playerName, period, new HashMap<>(), 0, false);
        }
        QuestsManager.markPlayerLoaded(playerName, overallTotal);
    }
}
