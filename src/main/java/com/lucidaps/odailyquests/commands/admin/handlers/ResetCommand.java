package com.lucidaps.odailyquests.commands.admin.handlers;

import com.lucidaps.odailyquests.commands.admin.AdminCommandBase;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.quests.categories.CategoriesLoader;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.quests.player.progression.QuestLoaderUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ResetCommand extends AdminCommandBase {

    private static final String RESET = "reset";

    @Override
    public String getName() {
        return RESET;
    }

    @Override
    public String getPermission() {
        return QuestsPermissions.QUESTS_ADMIN.get();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            help(sender);
            return;
        }

        final String action = args[1].toLowerCase();
        if (action.equalsIgnoreCase(TOTAL)) {
            handleResetTotal(sender, args);
        } else if (action.equalsIgnoreCase(QUESTS)) {
            handleResetQuests(sender, args);
        } else {
            help(sender);
        }
    }

    private void handleResetTotal(CommandSender sender, String[] args) {
        if (args.length == 3) {
            final Player target = getTargetPlayer(sender, args[2]);
            if (target != null) {
                resetTotal(sender, target);
            }
        } else if (args.length == 4) {
            final String category = args[2];
            if (!CategoriesLoader.getAllCategories(QuestPeriods.getDefaultPeriod()).containsKey(category)) {
                invalidCategory(sender);
                return;
            }
            final Player target = getTargetPlayer(sender, args[3]);
            if (target != null) {
                resetCategory(sender, target, category);
            }
        } else {
            help(sender);
        }
    }

    private void handleResetQuests(CommandSender sender, String[] args) {
        final Player target = getTargetPlayer(sender, args[2]);
        if (target != null) {
            if (args.length == 4 && args[3].equalsIgnoreCase("all")) {
                for (QuestPeriod period : QuestPeriods.getEnabledPeriods()) quests(sender, target, period);
                return;
            }
            final QuestPeriod period = args.length == 4
                    ? QuestPeriod.fromString(args[3]).orElse(null)
                    : QuestPeriods.getDefaultPeriod();
            if (period == null || !QuestPeriods.isEnabled(period)) {
                help(sender);
                return;
            }
            quests(sender, target, period);
        }
    }


    /**
     * Resets the current active quests of the player.
     *
     * @param sender the command sender
     * @param target the player to reset
     */
    public void quests(CommandSender sender, Player target) {
        quests(sender, target, QuestPeriod.DAILY);
    }

    public void quests(CommandSender sender, Player target, QuestPeriod period) {
        final String playerName = target.getName();
        final PlayerQuests playerQuests = getLoadedPlayerQuests(sender, target, period);
        if (playerQuests == null) return;

        final Map<String, Integer> totalAchievedQuestsByCategory = playerQuests.getTotalAchievedQuestsByCategory();
        final int totalAchievedQuests = playerQuests.getTotalAchievedQuests();

        QuestLoaderUtils.loadNewPlayerQuests(playerName, period, totalAchievedQuestsByCategory, totalAchievedQuests);

        String msg = QuestsMessages.QUESTS_RENEWED_ADMIN.toString();
        if (msg != null) sender.sendMessage(msg.replace(TARGET, target.getName()));
    }

    private void resetTotal(CommandSender sender, Player target) {
        if (!QuestsManager.isPlayerLoaded(target.getName())) return;
        QuestsManager.setOverallLifetimeTotal(target.getName(), 0);

        String msg = QuestsMessages.TOTAL_AMOUNT_RESET_ADMIN.toString();
        if (msg != null) sender.sendMessage(msg.replace(TARGET, target.getName()));

        msg = QuestsMessages.TOTAL_AMOUNT_RESET.getMessage(target);
        if (msg != null) target.sendMessage(msg);
    }

    private void resetCategory(CommandSender sender, Player target, String category) {
        final PlayerQuests playerQuests = getLoadedPlayerQuests(sender, target);
        if (playerQuests == null) return;

        playerQuests.setTotalCategoryAchievedQuests(category, 0);

        String msg = QuestsMessages.TOTAL_CATEGORY_RESET_ADMIN.toString();
        if (msg != null) sender.sendMessage(
                msg.replace(TARGET, target.getName())
                        .replace(CATEGORY, category)
        );

        msg = QuestsMessages.TOTAL_CATEGORY_RESET_TARGET.getMessage(target);
        if (msg != null) target.sendMessage(msg.replace(CATEGORY, category));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase(RESET)) {
            return List.of(QUESTS, TOTAL);
        }

        if (args.length == 3 && args[1].equalsIgnoreCase(TOTAL)) {
            final Set<String> categories = CategoriesLoader.getAllCategories(QuestPeriods.getDefaultPeriod()).keySet();
            final List<String> suggestions = new ArrayList<>(categories);

            Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
            return suggestions;
        }

        if (args.length == 4 && args[1].equalsIgnoreCase(TOTAL) && CategoriesLoader.getAllCategories(QuestPeriods.getDefaultPeriod()).containsKey(args[2])) {
            return null;
        }

        if (args.length == 4 && args[1].equalsIgnoreCase(QUESTS)) {
            final List<String> periods = new ArrayList<>(QuestPeriods.getEnabledPeriods().stream().map(QuestPeriod::getConfigKey).toList());
            periods.add("all");
            return periods;
        }

        if (args.length >= 5) {
            return Collections.emptyList();
        }

        if (args.length == 3 && args[1].equalsIgnoreCase(QUESTS)) {
            return null;
        }

        return Collections.emptyList();
    }
}
