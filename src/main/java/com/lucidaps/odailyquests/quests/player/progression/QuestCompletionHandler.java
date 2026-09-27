package com.lucidaps.odailyquests.quests.player.progression;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.configuration.functionalities.rewards.CategoriesRewards;
import com.lucidaps.odailyquests.configuration.functionalities.rewards.GlobalReward;
import com.lucidaps.odailyquests.configuration.functionalities.rewards.TotalRewards;
import com.lucidaps.odailyquests.quests.categories.CategoriesLoader;
import com.lucidaps.odailyquests.quests.categories.Category;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.rewards.Reward;
import com.lucidaps.odailyquests.rewards.RewardManager;
import com.lucidaps.odailyquests.tools.QuestPlaceholders;
import com.lucidaps.odailyquests.tools.TaskScheduler;
import com.lucidaps.odailyquests.tools.TextFormatter;
import org.bukkit.entity.Player;

import java.util.Collections;

/**
 * Internal quest completion/reward flow.
 */
public final class QuestCompletionHandler {

    private QuestCompletionHandler() {
    }

    public static void completeQuest(Player player, Progression progression, AbstractQuest quest) {
        Debugger.write("QuestCompletionHandler: completeQuest summoned by " + player.getName() + " for " + quest.getQuestName() + ".");

        /* prevention of excess progressions when mobs are killed with the sweeping edge enchantment */
        if (progression.isAchieved()) return;

        progression.setAchieved();

        final String formattedQuestName = QuestPlaceholders.replaceQuestPlaceholders(
                TextFormatter.format(player, quest.getQuestName()),
                player,
                quest,
                progression,
                null,
                null
        );

        RewardManager.sendQuestRewardItems(formattedQuestName, player, quest.getReward(), progression);

        final PlayerQuests playerQuests = QuestsManager.getActiveQuests(quest.getPeriod()).get(player.getName());
        if (playerQuests != null) {
            playerQuests.increaseCategoryAchievedQuests(quest.getCategoryName(), player);
        }
    }

    public static void handleAllCategoryQuestsCompleted(Player player, String categoryName) {
        handleAllCategoryQuestsCompleted(player, QuestPeriod.DAILY, categoryName);
    }

    public static void handleAllCategoryQuestsCompleted(Player player, QuestPeriod period, String categoryName) {
        final Category category = CategoriesLoader.getCategoryByName(period, categoryName);
        if (category == null) return;

        CategoriesRewards.sendCategoryReward(player, period, category.getName());
    }

    public static void handleAllQuestsCompleted(Player player) {
        handleAllQuestsCompleted(player, QuestPeriod.DAILY);
    }

    public static void handleAllQuestsCompleted(Player player, QuestPeriod period) {
        TaskScheduler.runSyncLater(() -> GlobalReward.sendGlobalReward(player.getName(), period), 1L);
    }

    public static void handleGlobalTotalReward(Player player, int totalCompleted) {
        final Reward reward = TotalRewards.getGlobalTotalReward(totalCompleted);
        if (reward == null) return;

        RewardManager.sendReward(player, reward, Collections.emptyMap());
    }

    public static void handleCategoryTotalReward(Player player, String category, int completedInCategory) {
        final Reward reward = TotalRewards.getCategoryTotalReward(category, completedInCategory);
        if (reward == null) return;

        RewardManager.sendReward(player, reward, Collections.emptyMap());
    }
}
