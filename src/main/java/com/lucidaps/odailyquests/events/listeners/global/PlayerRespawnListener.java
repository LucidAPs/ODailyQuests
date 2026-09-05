package com.lucidaps.odailyquests.events.listeners.global;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.essentials.Debugger;

import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener extends PlayerProgressor implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        if (player.hasMetadata("odailyquests:dead")) {
            player.removeMetadata("odailyquests:dead", ODailyQuests.INSTANCE);

            Debugger.write("=========================================================================================");
            Debugger.write("PlayerRespawnListener: onPlayerRespawn summoned by " + player.getName());
            setPlayerQuestProgression(event, player, 1, "PLAYER_DEATH");
        }
    }
}
