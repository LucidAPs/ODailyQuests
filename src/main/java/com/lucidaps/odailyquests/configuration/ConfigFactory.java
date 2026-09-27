package com.lucidaps.odailyquests.configuration;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.essentials.*;
import com.lucidaps.odailyquests.configuration.functionalities.progression.*;
import com.lucidaps.odailyquests.configuration.integrations.NPCNames;
import com.lucidaps.odailyquests.configuration.integrations.NexoEnabled;
import com.lucidaps.odailyquests.configuration.integrations.PapiPlaceholders;
import com.lucidaps.odailyquests.configuration.essentials.*;
import com.lucidaps.odailyquests.configuration.functionalities.CommandAliases;
import com.lucidaps.odailyquests.configuration.functionalities.CompleteOnlyOnClick;
import com.lucidaps.odailyquests.configuration.functionalities.DisabledWorlds;
import com.lucidaps.odailyquests.configuration.functionalities.SpawnerProgression;
import com.lucidaps.odailyquests.configuration.functionalities.TakeItem;
import com.lucidaps.odailyquests.configuration.functionalities.progression.*;
import com.lucidaps.odailyquests.configuration.functionalities.rewards.CategoriesRewards;
import com.lucidaps.odailyquests.configuration.functionalities.rewards.GlobalReward;
import com.lucidaps.odailyquests.configuration.functionalities.rewards.TotalRewards;
import com.lucidaps.odailyquests.configuration.integrations.*;
import com.lucidaps.odailyquests.files.FilesManager;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;
import com.lucidaps.odailyquests.files.implementations.TotalRewardsFile;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigFactory {

    private ConfigFactory() {}

    private static final Map<Class<? extends IConfigurable>, IConfigurable> configs = new LinkedHashMap<>();

    public static void registerConfigs(FilesManager filesManager) {
        final ConfigurationFile configurationFile = filesManager.getConfigurationFile();
        final TotalRewardsFile totalRewardsFile = filesManager.getTotalRewardsFile();

        // essentials
        configs.put(Prefix.class, new Prefix(configurationFile));
        configs.put(Antiglitch.class, new Antiglitch(configurationFile));
        configs.put(CustomFurnaceResults.class, new CustomFurnaceResults(configurationFile));
        configs.put(CustomTypes.class, new CustomTypes(configurationFile));
        configs.put(Database.class, new Database(configurationFile));
        configs.put(PlayerDataLoadDelay.class, new PlayerDataLoadDelay(configurationFile));
        configs.put(Debugger.class, new Debugger(configurationFile));
        configs.put(JoinMessageDelay.class, new JoinMessageDelay(configurationFile));
        configs.put(ReloadMessage.class, new ReloadMessage(configurationFile));
        configs.put(Logs.class, new Logs(configurationFile));
        configs.put(TimestampMode.class, new TimestampMode(configurationFile));
        configs.put(SafetyMode.class, new SafetyMode(configurationFile));
        configs.put(QuestsPerCategory.class, new QuestsPerCategory(configurationFile));
        configs.put(QuestPeriods.class, new QuestPeriods(configurationFile));
        configs.put(RerollNotAchieved.class, new RerollNotAchieved(configurationFile));
        configs.put(RerollMaximum.class, new RerollMaximum(configurationFile));
        configs.put(Synchronization.class, new Synchronization(configurationFile));
        configs.put(RenewInterval.class, new RenewInterval(configurationFile));
        configs.put(RenewTime.class, new RenewTime(configurationFile));
        // functionalities
        configs.put(ActionBar.class, new ActionBar(configurationFile));
        configs.put(ProgressBar.class, new ProgressBar(configurationFile));

        // if there was a previous ProgressionMessage config, clean it up first
        final IConfigurable prev = configs.get(ProgressionMessage.class);
        if (prev instanceof ProgressionMessage pm) {
            pm.cleanup(); // remove existing boss bars
        }
        configs.put(ProgressionMessage.class, new ProgressionMessage(configurationFile));

        configs.put(Title.class, new Title(configurationFile));
        configs.put(ToastNotification.class, new ToastNotification(configurationFile));
        configs.put(DisabledWorlds.class, new DisabledWorlds(configurationFile));
        configs.put(SpawnerProgression.class, new SpawnerProgression(configurationFile));
        configs.put(TakeItem.class, new TakeItem(configurationFile));
        configs.put(CompleteOnlyOnClick.class, new CompleteOnlyOnClick(configurationFile));
        configs.put(CommandAliases.class, new CommandAliases(configurationFile));
        configs.put(PapiPlaceholders.class, new PapiPlaceholders(configurationFile));

        // rewards
        configs.put(CategoriesRewards.class, new CategoriesRewards(configurationFile));
        configs.put(GlobalReward.class, new GlobalReward(configurationFile));
        configs.put(TotalRewards.class, new TotalRewards(totalRewardsFile));

        // integrations
        configs.put(NPCNames.class, new NPCNames(configurationFile));
        configs.put(NexoEnabled.class, new NexoEnabled(configurationFile));

        // load all configs
        configs.values().forEach(IConfigurable::load);

        // reload the timer task
        ODailyQuests.INSTANCE.reloadPeriodTimers();
    }

    public static <T extends IConfigurable> T getConfig(Class<T> clazz) {
        return clazz.cast(configs.get(clazz));
    }
}
