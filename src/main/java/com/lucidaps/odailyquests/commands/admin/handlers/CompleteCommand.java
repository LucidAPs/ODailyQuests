package com.lucidaps.odailyquests.commands.admin.handlers;

import com.lucidaps.odailyquests.commands.admin.AdminCommandBase;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.player.progression.QuestCompletionHandler;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CompleteCommand extends AdminCommandBase {

    @Override
    public String getName() {
        return "complete";
    }

    @Override
    public String getPermission() {
        return QuestsPermissions.QUESTS_ADMIN.get();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (args.length < 3 || args.length > 4) {
            help(sender);
            return;
        }
        final Player target = getTargetPlayer(sender, args[1]);
        if (target == null) {
            return;
        }

        final QuestPeriod period = args.length == 4
                ? QuestPeriod.fromString(args[2]).orElse(null)
                : QuestPeriods.getDefaultPeriod();
        if (period == null || !QuestPeriods.isEnabled(period)) {
            help(sender);
            return;
        }
        int questIndex = parseQuestIndex(sender, args[args.length - 1]);
        if (questIndex == -1) {
            return;
        }

        complete(sender, questIndex, target, period);
    }

    /**
     * Completes a quest for a player
     *
     * @param sender the command sender
     * @param questIndex the index of the quest
     * @param target     the player
     */
    private void complete(CommandSender sender, int questIndex, Player target, QuestPeriod period) {
        final var loadedQuests = getLoadedPlayerQuests(sender, target, period);
        if (loadedQuests == null) return;

        final Map<AbstractQuest, Progression> playerQuests = loadedQuests.getQuests();
        if (questIndex >= 1 && questIndex <= playerQuests.size()) {

            int index = 0;
            for (Map.Entry<AbstractQuest, Progression> entry : playerQuests.entrySet()) {
                if (index != questIndex - 1) {
                    index++;
                    continue;
                }

                final AbstractQuest quest = entry.getKey();
                final Progression progression = entry.getValue();

                if (playerQuests.get(quest).isAchieved()) {
                    final String msg = QuestsMessages.QUEST_ALREADY_ACHIEVED.toString();
                    if (msg != null) sender.sendMessage(msg);
                    return;
                }

                QuestCompletionHandler.completeQuest(target, progression, quest);

                return;
            }
            return;
        }
        invalidQuest(sender);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 3) {
            return QuestPeriods.getEnabledPeriods().stream().map(QuestPeriod::getConfigKey).toList();
        }
        if (args.length == 4) {
            final Player target = (args.length >= 2) ? org.bukkit.Bukkit.getPlayerExact(args[1]) : null;
            if (target == null) {
                return Collections.emptyList();
            }

            final QuestPeriod period = QuestPeriod.fromString(args[2]).orElse(QuestPeriod.DAILY);
            final var activeQuests = QuestsManager.getPlayerQuests(target.getName(), period);
            if (activeQuests == null) {
                return Collections.emptyList();
            }

            List<String> questNumbers = new ArrayList<>();
            for (int i = 1; i <= activeQuests.getQuests().size(); i++) {
                questNumbers.add(String.valueOf(i));
            }
            return questNumbers;
        }

        if (args.length >= 5) {
            return Collections.emptyList();
        }

        return null;
    }
}
