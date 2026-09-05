package com.lucidaps.odailyquests.commands.admin.handlers;

import com.lucidaps.odailyquests.commands.admin.AdminCommandBase;
import com.lucidaps.odailyquests.commands.interfaces.playerinterface.PlayerQuestsInterface;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class OpenCommand extends AdminCommandBase {

    private final PlayerQuestsInterface playerQuestsInterface;

    public OpenCommand(PlayerQuestsInterface playerQuestsInterface) {
        this.playerQuestsInterface = playerQuestsInterface;
    }

    @Override
    public String getName() {
        return "open";
    }

    @Override
    public String getPermission() {
        return QuestsPermissions.QUESTS_ADMIN.get();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        final Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            openTargetInventory(playerQuestsInterface, sender, args, target);
        } else help(sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length >= 3) {
            return Collections.emptyList();
        }

        return null;
    }
}
