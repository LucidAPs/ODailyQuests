package com.lucidaps.odailyquests.configuration.functionalities.rewards;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;
import com.lucidaps.odailyquests.rewards.Reward;
import com.lucidaps.odailyquests.rewards.RewardLoader;
import com.lucidaps.odailyquests.rewards.RewardManager;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CategoriesRewards implements IConfigurable {

    private final Map<QuestPeriod, Map<String, Reward>> categoryRewards = new java.util.EnumMap<>(QuestPeriod.class);

    private final ConfigurationFile configurationFile;
    private final RewardLoader rewardLoader = new RewardLoader();

    public CategoriesRewards(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Override
    public void load() {
        categoryRewards.clear();
        for (QuestPeriod period : QuestPeriod.values()) {
            ConfigurationSection section = configurationFile.getConfig().getConfigurationSection(
                    "quest_periods." + period.getConfigKey() + ".categories_rewards"
            );
            if (section == null && period == QuestPeriod.DAILY) {
                section = configurationFile.getConfig().getConfigurationSection("categories_rewards");
            }

            final Map<String, Reward> periodRewards = new HashMap<>();
            if (section != null) {
                for (String category : section.getKeys(false)) {
                    final ConfigurationSection rewardSection = section.getConfigurationSection(category);
                    if (rewardSection != null) {
                        periodRewards.put(category, rewardLoader.getRewardFromSection(rewardSection, "config.yml", null));
                    }
                }
            }
            categoryRewards.put(period, periodRewards);
        }
    }

    /**
     * Send a reward to a player depending on the category.
     *
     * @param player   player.
     * @param category category.
     */
    public void sendCategoryRewardInternal(Player player, QuestPeriod period, String category) {
        final Map<String, Reward> periodRewards = categoryRewards.getOrDefault(period, Map.of());
        if (!periodRewards.containsKey(category)) {
            Debugger.write("Category " + category + " is missing in the categories_rewards section.");
            return;
        }

        final Reward reward = periodRewards.get(category);
        if (reward != null) {
            final String msg = QuestsMessages.CATEGORY_QUESTS_ACHIEVED.toString();
            if (msg != null) {
                player.sendMessage(msg.replace("%category%", category).replace("%period%", period.getDisplayName()));
            }

            RewardManager.sendReward(player, reward, Collections.emptyMap());
        } else {
            PluginLogger.error("No reward found for category " + category);
        }
    }

    private static CategoriesRewards getInstance() {
        return ConfigFactory.getConfig(CategoriesRewards.class);
    }

    public static void sendCategoryReward(Player player, String category) {
        sendCategoryReward(player, QuestPeriod.DAILY, category);
    }

    public static void sendCategoryReward(Player player, QuestPeriod period, String category) {
        getInstance().sendCategoryRewardInternal(player, period, category);
    }
}
