package com.lucidaps.odailyquests.quests.categories;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.essentials.QuestAmountSetting;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.configuration.essentials.SafetyMode;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.files.implementations.QuestsFiles;
import com.lucidaps.odailyquests.quests.QuestTypeRegistry;
import com.lucidaps.odailyquests.quests.QuestsLoader;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoriesLoader {

    private static final Map<QuestPeriod, Map<String, Category>> categories = new java.util.EnumMap<>(QuestPeriod.class);

    private final QuestsLoader questsLoader;

    public CategoriesLoader(QuestTypeRegistry questTypeRegistry) {
        this.questsLoader = new QuestsLoader(questTypeRegistry);
    }

    /**
     * Load all quests from files.
     */
    public void loadCategories() {
        categories.clear();

        final boolean safetyMode = SafetyMode.isSafetyModeEnabled();

        for (QuestPeriod period : QuestPeriods.getEnabledPeriods()) {
            categories.put(period, new LinkedHashMap<>());
            loadPeriod(period, safetyMode);
            if (!ODailyQuests.INSTANCE.isEnabled()) return;
        }
    }

    private void loadPeriod(QuestPeriod period, boolean safetyMode) {
        final Map<String, Category> periodCategories = categories.get(period);
        for (Map.Entry<String, QuestAmountSetting> entry : QuestPeriods.getQuestAmounts(period).entrySet()) {
            final String categoryName = entry.getKey();
            final QuestAmountSetting setting = entry.getValue();
            final Integer requiredAmount = setting.getStaticAmount();

            final Category category = new Category(categoryName, period);
            periodCategories.put(categoryName, category);

            final FileConfiguration configFile = QuestsFiles.getQuestsConfigurationByCategory(period, categoryName);
            if (configFile == null) {
                PluginLogger.error("Failed to load configuration file for " + categoryName + ". Plugin will be disabled.");
                Bukkit.getPluginManager().disablePlugin(ODailyQuests.INSTANCE);
                return;
            }

            questsLoader.loadQuests(configFile, category, categoryName, period);
            if (!validateCategory(category, requiredAmount, categoryName, safetyMode, setting.isDynamic())) {
                Bukkit.getPluginManager().disablePlugin(ODailyQuests.INSTANCE);
                return;
            }
        }
    }

    /**
     * Validate that a category has enough quests and enough quests without permission.
     *
     * @param category       The quest category.
     * @param requiredAmount The required number of quests.
     * @param categoryName   The name of the category (for logs).
     * @return true if valid, false otherwise.
     */
    private boolean validateCategory(Category category,
                                     Integer requiredAmount,
                                     String categoryName,
                                     boolean safetyMode,
                                     boolean dynamicAmount) {
        final int totalQuests = category.size();
        final int publicQuests = (int) category
                .stream()
                .filter(quest -> {
                    List<String> permissions = quest.getRequiredPermissions();
                    return (permissions == null || permissions.isEmpty()) && !quest.hasPlaceholderConditions();
                })
                .count();

        if (requiredAmount != null) {
            if (totalQuests < requiredAmount) {
                PluginLogger.error("Impossible to enable the plugin.");
                PluginLogger.error("You need at least " + requiredAmount + " quests in your " + categoryName + ".yml file.");
                return false;
            }

            if (safetyMode) {
                if (publicQuests < requiredAmount) {
                    PluginLogger.error("Impossible to enable the plugin.");
                    PluginLogger.error("Category '" + categoryName + "': only " + publicQuests + " public quest(s) but " + requiredAmount + " required (safety_mode=true).");
                    PluginLogger.error("Disable 'safety_mode' if you want permission- or condition-gated categories; note players without the required permissions or placeholders may end up with no quests.");
                    return false;
                }
            } else if (publicQuests == 0) {
                PluginLogger.warn("Category '" + categoryName + "' has no public quests. Players without permissions may receive 0 quests (safety_mode=false).");
            }
        } else if (dynamicAmount) {
            PluginLogger.warn("Category '" + categoryName + "' uses a placeholder-based amount. Make sure your quests files contain enough entries for the highest possible value.");
            if (safetyMode && publicQuests == 0) {
                PluginLogger.warn("safety_mode is enabled but category '" + categoryName + "' has 0 public quests. Players without permissions may receive 0 quests if their placeholder evaluates to a positive value.");
            }
        }

        return true;
    }

    /**
     * Get category by name.
     *
     * @param name category name.
     * @return category.
     */
    public static Category getCategoryByName(String name) {
        return getCategoryByName(QuestPeriod.DAILY, name);
    }

    public static Category getCategoryByName(QuestPeriod period, String name) {
        return categories.getOrDefault(period, Map.of()).get(name);
    }

    /**
     * Get all categories.
     *
     * @return all categories.
     */
    public static Map<String, Category> getAllCategories() {
        return getAllCategories(QuestPeriod.DAILY);
    }

    public static Map<String, Category> getAllCategories(QuestPeriod period) {
        return categories.getOrDefault(period, Map.of());
    }

    public static boolean hasCategory(String categoryName) {
        return hasCategory(QuestPeriod.DAILY, categoryName);
    }

    public static boolean hasCategory(QuestPeriod period, String categoryName) {
        return categories.getOrDefault(period, Map.of()).containsKey(categoryName);
    }
}
