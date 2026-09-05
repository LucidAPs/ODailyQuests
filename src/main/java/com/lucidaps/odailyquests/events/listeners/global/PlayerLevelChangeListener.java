package com.lucidaps.odailyquests.events.listeners.global;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;

import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLevelChangeEvent;

public class PlayerLevelChangeListener extends PlayerProgressor implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLevelChangeEvent(PlayerLevelChangeEvent event) {
        final int diff = event.getNewLevel() - event.getOldLevel();
        if (diff > 0) {
            Debugger.write("=========================================================================================");
            Debugger.write("PlayerLevelChangeListener: onPlayerLevelChangeEvent summoned by " + event.getPlayer().getName());

            setPlayerQuestProgression(event, event.getPlayer(), diff, "EXP_LEVELS");
        }
    }
}
