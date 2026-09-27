package com.lucidaps.odailyquests.commands.admin.handlers;

import com.lucidaps.odailyquests.commands.admin.AdminCommandBase;
import com.lucidaps.odailyquests.commands.interfaces.playerinterface.PlayerQuestsInterface;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AShowCommand extends AdminCommandBase {

    private final PlayerQuestsInterface playerQuestsInterface;

    public AShowCommand(PlayerQuestsInterface playerQuestsInterface) {
        this.playerQuestsInterface = playerQuestsInterface;
    }

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public String getPermission() {
        return QuestsPermissions.QUESTS_ADMIN.get();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            openTargetInventory(playerQuestsInterface, sender, args, player);
        } else {
            final String msg = QuestsMessages.PLAYER_ONLY.toString();
            if (msg != null) sender.sendMessage(msg);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 3) {
            return QuestPeriods.getEnabledPeriods().stream().map(QuestPeriod::getConfigKey).toList();
        }
        if (args.length >= 4) {
            return Collections.emptyList();
        }

        return null;
    }
}
