package com.lucidaps.odailyquests.quests.player.progression;

import com.lucidaps.odailyquests.configuration.essentials.*;
import com.lucidaps.odailyquests.configuration.essentials.*;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.quests.categories.CategoriesLoader;
import com.lucidaps.odailyquests.quests.categories.Category;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.tools.RenewSchedule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.entity.Player;

import java.time.*;
import java.util.*;

public class QuestLoaderUtils {

    private QuestLoaderUtils() {
    }

    /**
     * Check if it is time to redraw quests for a player.
     *
     * @param timestamp player timestamp.
     * @return true if it's time to redraw quests.
     */
    public static boolean checkTimestamp(long timestamp) {
        return checkTimestamp(QuestPeriod.DAILY, timestamp);
    }

    public static boolean checkTimestamp(QuestPeriod period, long timestamp) {
        return QuestPeriods.shouldRenew(period, timestamp);
    }

    /**
     * Load quests for a player with no data.
     *
     * @param playerName   player name.
     * @param activeQuests all active quests.
     */
    public static void loadNewPlayerQuests(String playerName, Map<String, PlayerQuests> activeQuests, Map<String, Integer> totalAchievedQuestsByCategory, int totalAchievedQuests) {
        loadNewPlayerQuests(playerName, QuestPeriod.DAILY, totalAchievedQuestsByCategory, totalAchievedQuests);
    }

    public static void loadNewPlayerQuests(String playerName, QuestPeriod period, Map<String, Integer> totalAchievedQuestsByCategory, int totalAchievedQuests) {
        loadNewPlayerQuests(playerName, period, totalAchievedQuestsByCategory, totalAchievedQuests, true);
    }

    public static void loadNewPlayerQuests(String playerName, QuestPeriod period, Map<String, Integer> totalAchievedQuestsByCategory, int totalAchievedQuests, boolean notifyPlayer) {
        Debugger.write("Entering loadNewPlayerQuests method for player " + playerName + ".");
        QuestsManager.getActiveQuests(period).remove(playerName);

        final Player player = Bukkit.getPlayer(playerName);
        Debugger.write("Attempting to renew quests for player " + playerName + ".");
        if (player == null) {
            Debugger.write("Player " + playerName + " is null. Impossible to renew quests.");
            PluginLogger.warn("It seems that " + playerName + " disconnected before the end of the quest renewal.");
            return;
        }

        final Map<AbstractQuest, Progression> quests = QuestsManager.selectRandomQuests(player, period);
        final PlayerQuests playerQuests = new PlayerQuests(period, System.currentTimeMillis(), quests);

        playerQuests.setTotalAchievedQuests(totalAchievedQuests);
        playerQuests.setTotalAchievedQuestsByCategory(totalAchievedQuestsByCategory);
        playerQuests.setRecentRerolls(0);

        final String msg = QuestsMessages.QUESTS_RENEWED.getMessage(player);
        if (notifyPlayer && msg != null && player.hasPermission(QuestsPermissions.QUESTS_PROGRESS.get())) {
            player.sendMessage(msg.replace("%period%", period.getDisplayName()));
        }

        QuestsManager.registerPlayerPeriod(playerName, period, playerQuests);
        if (Logs.isEnabled()) {
            PluginLogger.info(playerName + "'s quests have been renewed.");
        }

        Debugger.write("Quests of player " + playerName + " have been renewed.");
    }

    /**
     * Check if it's time to renew quests. If so, renew them.
     *
     * @param player       player.
     * @param activeQuests all active quests.
     * @return true if it's time to renew quests.
     */
    public static boolean isTimeToRenew(Player player, Map<String, PlayerQuests> activeQuests) {
        return isTimeToRenew(player, QuestPeriod.DAILY);
    }

    public static boolean isTimeToRenew(Player player, QuestPeriod period) {
        if (QuestPeriods.get(period).timestampMode() == 1) return false;
        final PlayerQuests playerQuests = QuestsManager.getPlayerQuests(player.getName(), period);
        if (playerQuests == null) return false;

        if (checkTimestamp(period, playerQuests.getTimestamp())) {
            loadNewPlayerQuests(player.getName(), period, playerQuests.getTotalAchievedQuestsByCategory(), playerQuests.getTotalAchievedQuests());
            return true;
        }

        return false;
    }

    /**
     * Find quest with index in arrays.
     *
     * @param playerName player name.
     * @param questIndex index of quest in array.
     * @param id         number of player quest.
     * @return quest of index.
     */
    public static AbstractQuest findQuest(String playerName, int questIndex, int id) {
        return findQuest(QuestPeriod.DAILY, playerName, questIndex, id);
    }

    public static AbstractQuest findQuest(QuestPeriod period, String playerName, int questIndex, int id) {
        AbstractQuest quest = null;

        final Map<String, Category> categoryMap = CategoriesLoader.getAllCategories(period);
        int totalQuestsCount = 0;

        for (Map.Entry<String, Category> entry : categoryMap.entrySet()) {
            String categoryName = entry.getKey();
            Category category = entry.getValue();
            final QuestAmountSetting amountSetting = QuestPeriods.getQuestAmounts(period).get(categoryName);
            int categoryQuestsAmount = amountSetting == null || amountSetting.getStaticAmount() == null
                    ? category.size()
                    : amountSetting.getStaticAmount();

            if (id <= totalQuestsCount + categoryQuestsAmount) {
                quest = getQuestAtIndex(category, questIndex, playerName);
                break;
            }

            totalQuestsCount += categoryQuestsAmount;
        }

        if (quest == null) {
            PluginLogger.warn("Quest ID " + id + " was not found. Player quests will be reset.");
            PluginLogger.warn("This can happen after a server reload or if the quest was deleted from the file.");
        }

        return quest;
    }

    public static AbstractQuest findQuest(String playerName, String categoryName, int questIndex, int id) {
        return findQuest(QuestPeriod.DAILY, playerName, categoryName, questIndex, id);
    }

    public static AbstractQuest findQuest(QuestPeriod period, String playerName, String categoryName, int questIndex, int id) {
        if (categoryName != null && !categoryName.isEmpty()) {
            final Category category = CategoriesLoader.getCategoryByName(period, categoryName);
            if (category == null) {
                PluginLogger.warn("Category '" + categoryName + "' referenced in player " + playerName + " data no longer exists. New quests will be drawn for the player.");
                return null;
            }
            return getQuestAtIndex(category, questIndex, playerName);
        }

        return findQuest(period, playerName, questIndex, id);
    }

    /**
     * Try to get quest from index.
     *
     * @param category   the array where find the quest.
     * @param index      the supposed index of the quest in the array.
     * @param playerName the name of the player for whom the quest is intended.
     * @return the quest.
     */
    public static AbstractQuest getQuestAtIndex(Category category, int index, String playerName) {
        AbstractQuest quest = null;
        try {
            quest = category.get(index);
        } catch (IndexOutOfBoundsException e) {
            if (!category.isEmpty()) playerQuestMissing(playerName);
            else noQuestsAvailable();
        }

        return quest;
    }

    private static void playerQuestMissing(String playerName) {
        PluginLogger.warn("A quest of the player " + playerName + " could not be loaded.");
        PluginLogger.warn("This happens when a previously loaded quest has been deleted from the file.");
        PluginLogger.warn("To avoid this problem, you should reset player progressions when you delete quests.");
        PluginLogger.warn("New quests will be drawn for the player.");
    }

    private static void noQuestsAvailable() {
        PluginLogger.error("There is no quest at all available!");
        PluginLogger.error("It can happen if Nexo integration is enabled without the corresponding plugin.");
        PluginLogger.error("Please check your configuration. If the problem persists, contact the developer.");
    }
}
