package com.lucidaps.odailyquests.commands.player.handlers;

import com.lucidaps.odailyquests.commands.player.PlayerCommandBase;
import com.lucidaps.odailyquests.commands.interfaces.playerinterface.PlayerQuestsInterface;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class MeCommand extends PlayerCommandBase {

    private final PlayerQuestsInterface playerQuestsInterface;

    public MeCommand(PlayerQuestsInterface playerQuestsInterface) {
        this.playerQuestsInterface = playerQuestsInterface;
    }

    @Override
    public String getName() {
        return "me";
    }

    @Override
    public String getPermission() {
        return QuestsPermissions.QUESTS_PLAYER_USE.get();
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length > 2) {
            help(player);
            return;
        }

        final QuestPeriod period;
        if (args.length == 2) {
            period = QuestPeriod.fromString(args[1]).orElse(null);
            if (period == null || !QuestPeriods.isEnabled(period)) {
                help(player);
                return;
            }
        } else {
            period = QuestPeriods.getDefaultPeriod();
        }

        openInventory(player, period);
    }

    /**
     * Opens the quests interface for the player.
     *
     * @param player the player.
     */
    private void openInventory(Player player, QuestPeriod period) {
        final Inventory inventory = playerQuestsInterface.getPlayerQuestsInterface(player, period);
        if (inventory == null) {
            String msg = QuestsMessages.IMPOSSIBLE_TO_OPEN_INVENTORY.toString();
            if (msg != null) player.sendMessage(msg);

            msg = QuestsMessages.CONTACT_ADMIN.toString();
            if (msg != null) player.sendMessage(msg);

            return;
        }

        player.openInventory(inventory);
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length != 2) return java.util.Collections.emptyList();
        return QuestPeriods.getEnabledPeriods().stream().map(QuestPeriod::getConfigKey).toList();
    }
}
