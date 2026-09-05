package com.lucidaps.odailyquests.events.restart;

import com.lucidaps.odailyquests.ODailyQuests;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class RestartCommandListener extends RestartHandler implements Listener {

    public RestartCommandListener(ODailyQuests oDailyQuests) {
        super(oDailyQuests);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        final String command = event.getCommand();
        if (command.equalsIgnoreCase("stop") || command.equalsIgnoreCase("restart")) {
            setServerStopping();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        final String raw = event.getMessage().trim();
        final String lower = raw.toLowerCase(java.util.Locale.ROOT);

        final String base = lower.split("\\s+")[0];
        if (!isStopOrRestartBase(base)) return;

        final Player player = event.getPlayer();
        if (!hasStopRestartPermission(player, base)) return;

        setServerStopping();
    }

    private boolean isStopOrRestartBase(String base) {
        return base.equals("/stop")
                || base.equals("/minecraft:stop")
                || base.equals("/restart")
                || base.equals("/minecraft:restart");
    }

    private boolean hasStopRestartPermission(Player player, String base) {
        if (player.isOp()) return true;

        if (base.endsWith("stop")) {
            return player.hasPermission("bukkit.command.stop")
                    || player.hasPermission("minecraft.command.stop");
        }

        if (base.endsWith("restart")) {
            return player.hasPermission("bukkit.command.restart");
        }

        return false;
    }
}
