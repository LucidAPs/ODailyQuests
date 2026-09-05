package com.lucidaps.odailyquests.commands.admin;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.enums.QuestsMessages;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AdminCommands implements CommandExecutor {

    private final ODailyQuests plugin;
    private final AdminCommandRegistry adminCommandRegistry;

    public AdminCommands(ODailyQuests plugin, AdminCommandRegistry adminCommandRegistry) {
        this.plugin = plugin;
        this.adminCommandRegistry = adminCommandRegistry;
    }

    /**
     * Handles the main admin command entry point.
     * <p>
     * This method:
     * <ul>
     *   <li>checks admin permission</li>
     *   <li>handles the built-in "reload" subcommand</li>
     *   <li>delegates other subcommands to the {@link AdminCommandRegistry}</li>
     *   <li>falls back to the help message when needed</li>
     * </ul>
     *
     * @param sender  the command sender
     * @param command the Bukkit command
     * @param label   the alias used
     * @param args    command arguments
     * @return true if the command was handled (Bukkit convention)
     */
    @Override
    public boolean onCommand(
            CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission(QuestsPermissions.QUESTS_ADMIN.get())) {
            noPermission(sender);
            return true;
        }

        if (args.length == 0) {
            help(sender);
            return true;
        }

        if (args.length == 1) {
            return handleSingleArg(sender, args[0]);
        }

        return handleRegisteredSubcommand(sender, args);
    }

    /**
     * Handles single-argument admin commands (e.g. "/dqa reload").
     *
     * @param sender     the command sender
     * @param subcommand the first argument
     * @return true when handled
     */
    private boolean handleSingleArg(CommandSender sender, String subcommand) {
        if ("reload".equalsIgnoreCase(subcommand)) {
            plugin.getReloadService().reload();
            sendMessage(sender, QuestsMessages.PLUGIN_RELOADED);
            return true;
        }

        help(sender);
        return true;
    }

    /**
     * Resolves and executes a registered admin subcommand.
     * <p>
     * If the subcommand is unknown or the sender lacks permission,
     * an appropriate message is sent.
     *
     * @param sender the command sender
     * @param args   full command arguments
     * @return true when handled
     */
    private boolean handleRegisteredSubcommand(CommandSender sender, String[] args) {
        final AdminCommand handler = adminCommandRegistry.getCommandHandler(args[0]);
        if (handler == null) {
            help(sender);
            return true;
        }

        if (!sender.hasPermission(handler.getPermission())) {
            noPermission(sender);
            return true;
        }

        handler.execute(sender, args);
        return true;
    }

    /**
     * Sends the admin help message to the sender.
     *
     * @param sender the command sender
     */
    private void help(CommandSender sender) {
        sendMessage(sender, QuestsMessages.ADMIN_HELP);
    }

    /**
     * Sends a message to the sender if they do not have permission to use the command.
     *
     * @param sender the command sender
     */
    private void noPermission(CommandSender sender) {
        sendMessage(sender, QuestsMessages.NO_PERMISSION);
    }

    /**
     * Sends a localized message to a sender, if available.
     *
     * @param sender  the command sender
     * @param message the message enum to send
     */
    private void sendMessage(CommandSender sender, QuestsMessages message) {
        final String msg = message.toString();
        if (msg != null) {
            sender.sendMessage(msg);
        }
    }
}
