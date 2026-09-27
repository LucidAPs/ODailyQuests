package com.lucidaps.odailyquests.commands.admin.convert;

import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.player.progression.QuestLoaderUtils;
import com.lucidaps.odailyquests.quests.player.progression.storage.sql.SQLManager;
import com.lucidaps.odailyquests.quests.player.progression.storage.PlayerQuestData;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public abstract class SQLConverter {

    /**
     * Converts a YAML progression file to SQL rows.
     * <p>
     * For each player UUID found in the YAML configuration, this method reads:
     * <ul>
     *   <li>timestamp and achievement counters</li>
     *   <li>all quest progressions</li>
     * </ul>
     * Then it builds a {@link PlayerQuests} instance and persists it through the provided {@link SQLManager}.
     *
     * @param progressionFile the YAML progression configuration
     * @param sqlManager      the SQL manager used to save progressions
     */
    protected void convertData(FileConfiguration progressionFile, SQLManager sqlManager) {
        for (String playerUuid : progressionFile.getKeys(false)) {
            final ConfigurationSection playerSection = getRequiredSection(
                    progressionFile, playerUuid, "SQLConverter - Missing player section for " + playerUuid
            );
            if (playerSection == null) return;

            final int overallTotal = playerSection.contains("overallTotalAchievedQuests")
                    ? playerSection.getInt("overallTotalAchievedQuests")
                    : playerSection.getInt("totalAchievedQuests");
            final EnumMap<QuestPeriod, PlayerQuests> periods = new EnumMap<>(QuestPeriod.class);
            final ConfigurationSection periodSections = playerSection.getConfigurationSection("periods");

            if (periodSections == null) {
                final QuestPeriod legacyPeriod = QuestPeriods.getLegacyPeriod();
                final PlayerQuests legacyQuests = loadPeriod(playerUuid, legacyPeriod, playerSection);
                if (legacyQuests == null) return;
                periods.put(legacyPeriod, legacyQuests);
            } else {
                for (QuestPeriod period : QuestPeriods.getEnabledPeriods()) {
                    final ConfigurationSection periodSection = periodSections.getConfigurationSection(period.getConfigKey());
                    if (periodSection == null) continue;
                    final PlayerQuests quests = loadPeriod(playerUuid, period, periodSection);
                    if (quests == null) return;
                    periods.put(period, quests);
                }
            }

            sqlManager.getSaveProgressionSQL().saveProgression(
                    playerUuid, playerUuid, new PlayerQuestData(overallTotal, periods), true
            );
        }
    }

    private PlayerQuests loadPeriod(String playerUuid, QuestPeriod period, ConfigurationSection section) {
        final ConfigurationSection questsSection = getRequiredSection(
                section, ".quests", "SQLConverter - Missing " + period.getDisplayName() + " quests section for " + playerUuid
        );
        if (questsSection == null) return null;

        final LinkedHashMap<AbstractQuest, Progression> quests = loadQuests(playerUuid, period, questsSection);
        if (quests == null) return null;

        final PlayerQuests playerQuests = buildPlayerQuests(
                period,
                section.getLong(".timestamp"),
                section.getInt(".achievedQuests"),
                section.getInt(".totalAchievedQuests"),
                quests
        );
        playerQuests.setRecentRerolls(section.contains("recentRerolls")
                ? section.getInt("recentRerolls")
                : section.getInt("recentRolls"));
        final ConfigurationSection totals = section.getConfigurationSection("totalAchievedQuestsByCategory");
        if (totals != null) {
            final Map<String, Integer> values = new HashMap<>();
            for (String category : totals.getKeys(false)) values.put(category, totals.getInt(category));
            playerQuests.setTotalAchievedQuestsByCategory(values);
        }
        return playerQuests;
    }

    /**
     * Loads all quest progressions for a player.
     *
     * @param playerUuid    the player UUID (as string)
     * @param questsSection the configuration section containing quests data
     * @return a map of quest -> progression, or null if a critical error occurred
     */
    private LinkedHashMap<AbstractQuest, Progression> loadQuests(String playerUuid, QuestPeriod period, ConfigurationSection questsSection) {
        final LinkedHashMap<AbstractQuest, Progression> quests = new LinkedHashMap<>();

        for (String questKey : questsSection.getKeys(false)) {
            final ConfigurationSection progressionSection = getRequiredSection(
                    questsSection, questKey, "SQLConverter - Missing progression section for " + playerUuid + " / " + questKey
            );
            if (progressionSection == null) return null;

            final Progression progression = loadProgression(progressionSection);

            final int questIndex = progressionSection.getInt(".index");
            final String categoryName = progressionSection.getString(".category");

            final AbstractQuest quest = QuestLoaderUtils.findQuest(
                    period,
                    playerUuid,
                    categoryName,
                    questIndex,
                    Integer.parseInt(questKey)
            );

            if (quest == null) {
                error("SQLConverter - Quest not found for " + playerUuid + " (category=" + categoryName
                        + ", index=" + questIndex + ", key=" + questKey + ")");
                return null;
            }

            final int selectedRequired = progressionSection.getInt(".selectedRequired", -1);
            if (selectedRequired >= 0) progression.setSelectedRequiredIndex(selectedRequired);

            quests.put(quest, progression);
        }

        return quests;
    }

    /**
     * Reads a single quest progression from its configuration section.
     *
     * @param progressionSection the configuration section of the progression
     * @return the built {@link Progression}, or null if a critical error occurred
     */
    private Progression loadProgression(ConfigurationSection progressionSection) {
        final int advancement = progressionSection.getInt(".progression");
        final int requiredAmount = progressionSection.getInt(".requiredAmount");
        final boolean isAchieved = progressionSection.getBoolean(".isAchieved");

        final Double rewardAmount = progressionSection.isSet(".rewardAmount")
                ? progressionSection.getDouble(".rewardAmount")
                : null;

        final double resolvedRewardAmount = rewardAmount != null ? rewardAmount : 0.0D;

        return new Progression(requiredAmount, resolvedRewardAmount, advancement, isAchieved);
    }

    /**
     * Builds a {@link PlayerQuests} object from loaded values.
     *
     * @param timestamp           stored timestamp
     * @param achievedQuests      current achieved quests count
     * @param totalAchievedQuests lifetime achieved quests count
     * @param quests              loaded quest progressions
     * @return the created {@link PlayerQuests}
     */
    private PlayerQuests buildPlayerQuests(
            QuestPeriod period,
            long timestamp,
            int achievedQuests,
            int totalAchievedQuests,
            LinkedHashMap<AbstractQuest, Progression> quests
    ) {
        // Resolve missing reward amounts from quest definitions (when needed).
        for (Map.Entry<AbstractQuest, Progression> entry : quests.entrySet()) {
            final AbstractQuest quest = entry.getKey();
            final Progression prog = entry.getValue();

            if (prog.getRewardAmount() == 0.0D) {
                prog.setRewardAmount(quest.getReward().resolveRewardAmount());
            }
        }

        final PlayerQuests playerQuests = new PlayerQuests(period, timestamp, quests);
        playerQuests.setAchievedQuests(achievedQuests);
        playerQuests.setTotalAchievedQuests(totalAchievedQuests);
        return playerQuests;
    }

    /**
     * Returns a nested configuration section or logs a critical error if missing.
     *
     * @param parent       the parent configuration section
     * @param path         the child path
     * @param errorMessage message to log if the section is missing
     * @return the child section, or null if missing
     */
    private ConfigurationSection getRequiredSection(ConfigurationSection parent, String path, String errorMessage) {
        final ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            error(errorMessage);
        }
        return section;
    }

    /**
     * Logs an error message for conversion failures.
     *
     * @param message the error message
     */
    private void error(String message) {
        PluginLogger.error("An error occurred while converting YAML to SQL.");
        PluginLogger.error("If the error persists, please report it to the developer.");
        PluginLogger.error(message);
    }
}
