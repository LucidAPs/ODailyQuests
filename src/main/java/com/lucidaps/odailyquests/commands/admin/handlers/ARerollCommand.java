package com.lucidaps.odailyquests.commands.admin.handlers;

import com.lucidaps.odailyquests.commands.admin.AdminCommandBase;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ARerollCommand extends AdminCommandBase {

    @Override
    public String getName() {
        return "reroll";
    }

    @Override
    public String getPermission() {
        return QuestsPermissions.QUESTS_ADMIN.get();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length >= 3 && args.length <= 4 && args[1] != null) {

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
            int index = parseQuestIndex(sender, args[args.length - 1]);
            if (index == -1) {
                return;
            }

            reroll(sender, target, period, index);

        } else help(sender);
    }

    /**
     * Rerolls a specific quest for a player.
     *
     * @param sender the command sender
     * @param target the player to reroll the quest for
     * @param index  the index of the quest to reroll
     */
    private void reroll(CommandSender sender, Player target, QuestPeriod period, int index) {
        final String playerName = target.getName();
        final PlayerQuests playerQuests = getLoadedPlayerQuests(sender, target, period);
        if (playerQuests == null) return;

        if (index < 1 || index > playerQuests.getQuests().size()) {
            invalidQuest(sender);
            return;
        }

        int count = playerQuests.getRecentlyRolled();
        if (playerQuests.rerollQuest(index - 1, target, true)) {
            confirmationToSender(sender, index, playerName);
            confirmationToTarget(index, QuestPeriods.get(period).rerollMaximum()-count, target);
        }
    }

    /**
     * Sends the confirmation message to the sender.
     *
     * @param sender the command sender
     * @param index  the index of the quest that was rerolled
     * @param target the name of the player who had their quest rerolled
     */
    private void confirmationToSender(CommandSender sender, int index, String target) {
        final String msg = QuestsMessages.QUEST_REROLLED_ADMIN.toString();
        if (msg != null) {
            sender.sendMessage(msg
                    .replace("%index%", String.valueOf(index))
                    .replace("%target%", target));
        }
    }

    /**
     * Sends the confirmation message to the target player.
     *
     * @param index  the index of the quest that was rerolled
     * @param target the player who had their quest rerolled
     */
    private void confirmationToTarget(int index, int remaining, Player target) {
        final String msg = QuestsMessages.QUEST_REROLLED.toString();
        if (msg != null) target.sendMessage(msg.replace("%index%", String.valueOf(index)).replace("%remaining%", String.valueOf(remaining)));
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
            final PlayerQuests playerQuests = QuestsManager.getPlayerQuests(target.getName(), period);
            if (playerQuests == null) {
                return Collections.emptyList();
            }

            List<String> questNumbers = new ArrayList<>();
            for (int i = 1; i <= playerQuests.getQuests().size(); i++) {
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
