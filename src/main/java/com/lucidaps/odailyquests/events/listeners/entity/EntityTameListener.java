package com.lucidaps.odailyquests.events.listeners.entity;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;

import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

public class EntityTameListener extends PlayerProgressor implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTameEvent(EntityTameEvent event) {
        if (event.isCancelled()) return;

        if (event.getOwner() instanceof Player player) {
            Debugger.write("=========================================================================================");
            Debugger.write("EntityTameEvent: onEntityTameEvent summoned by " + player.getName() + " for " + event.getEntityType() + ".");

            setPlayerQuestProgression(event, player, 1, "TAME");
        }
    }
}
