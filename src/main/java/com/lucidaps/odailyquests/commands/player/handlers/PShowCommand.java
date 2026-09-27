package com.lucidaps.odailyquests.commands.player.handlers;

import com.lucidaps.odailyquests.commands.player.PlayerCommandBase;
import com.lucidaps.odailyquests.commands.interfaces.QuestsInterfaces;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import com.lucidaps.odailyquests.quests.categories.CategoriesLoader;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PShowCommand extends PlayerCommandBase {

    private static final String PERMISSION_PREFIX = "odailyquests.";

    private final QuestsInterfaces questsInterfaces;

    public PShowCommand(QuestsInterfaces questsInterfaces) {
        this.questsInterfaces = questsInterfaces;
    }

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public String getPermission() {
        return QuestsPermissions.QUESTS_PLAYER_SHOW.get();
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2 || args.length > 3) {
            help(player);
            return;
        }

        final QuestPeriod period = args.length == 3
                ? QuestPeriod.fromString(args[1]).orElse(null)
                : QuestPeriods.getDefaultPeriod();
        if (period == null || !QuestPeriods.isEnabled(period)) {
            help(player);
            return;
        }
        openCategory(player, period, args[args.length - 1]);
    }

    /**
     * Opens the category interface.
     * @param player the player who wants to open the category.
     * @param category the category.
     */
    private void openCategory(Player player, QuestPeriod period, String category) {
        if (!CategoriesLoader.hasCategory(period, category)) {
            invalidCategory(player);
            return;
        }

        if (!player.hasPermission(PERMISSION_PREFIX + category)) {
            noPermissionCategory(player);
            return;
        }

        final Inventory inventory = questsInterfaces.getInterfaceFirstPage(period, category, player);
        if (inventory == null) {
            final String msg = QuestsMessages.CONFIGURATION_ERROR.toString();
            if (msg != null) player.sendMessage(msg);
            return;
        }

        player.openInventory(inventory);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            return QuestPeriods.getEnabledPeriods().stream().map(QuestPeriod::getConfigKey).toList();
        }
        if (args.length == 3) {
            final QuestPeriod period = QuestPeriod.fromString(args[1]).orElse(QuestPeriod.DAILY);
            List<String> categories = new ArrayList<>(CategoriesLoader.getAllCategories(period).keySet());
            Collections.sort(categories);
            return categories;
        }

        return Collections.emptyList();
    }
}
