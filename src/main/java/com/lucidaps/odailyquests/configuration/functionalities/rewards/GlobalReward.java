package com.lucidaps.odailyquests.configuration.functionalities.rewards;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;
import com.lucidaps.odailyquests.rewards.Reward;
import com.lucidaps.odailyquests.rewards.RewardLoader;
import com.lucidaps.odailyquests.rewards.RewardManager;
import com.lucidaps.odailyquests.rewards.RewardType;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.TextFormatter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;

public class GlobalReward implements IConfigurable {

    private final ConfigurationFile configurationFile;
    private final RewardLoader rewardLoader = new RewardLoader();

    public GlobalReward(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    private final java.util.EnumMap<QuestPeriod, Reward> rewards = new java.util.EnumMap<>(QuestPeriod.class);

    @Override
    public void load() {
        rewards.clear();
        for (QuestPeriod period : QuestPeriod.values()) {
            ConfigurationSection section = configurationFile.getConfig().getConfigurationSection(
                    "quest_periods." + period.getConfigKey() + ".global_reward"
            );
            if (section == null && period == QuestPeriod.DAILY) {
                section = configurationFile.getConfig().getConfigurationSection("global_reward");
            }
            if (section == null || !section.getBoolean("enabled", false)) continue;

            rewards.put(period, rewardLoader.getRewardFromSection(section, "config.yml", null));
            PluginLogger.fine(period.getDisplayName() + " all-completed reward successfully loaded.");
        }
    }

    public void sendGlobalRewardInternal(String playerName, QuestPeriod period) {
        final Reward reward = rewards.get(period);
        if (reward != null) {
            final Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                PluginLogger.warn("Impossible to send global reward to " + playerName + " because he is offline.");
                return;
            }

            final String msg = QuestsMessages.ALL_QUESTS_ACHIEVED.getMessage(playerName);
            if (msg != null) player.sendMessage(msg.replace("%period%", period.getDisplayName()));

            RewardManager.sendReward(Bukkit.getPlayer(playerName), reward, Collections.emptyMap());
        }
    }

    private static GlobalReward getInstance() {
        return ConfigFactory.getConfig(GlobalReward.class);
    }

    public static void sendGlobalReward(String playerName) {
        sendGlobalReward(playerName, QuestPeriod.DAILY);
    }

    public static void sendGlobalReward(String playerName, QuestPeriod period) {
        getInstance().sendGlobalRewardInternal(playerName, period);
    }
}
