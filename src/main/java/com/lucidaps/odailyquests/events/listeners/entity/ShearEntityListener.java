package com.lucidaps.odailyquests.events.listeners.entity;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;

import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;

public class ShearEntityListener extends PlayerProgressor implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShearEntityEvent(PlayerShearEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        final Entity entity = event.getEntity();

        Debugger.write("=========================================================================================");
        Debugger.write("ShearEntityListener: Shear event by " + player.getName() + " on " + entity.getType());

        setPlayerQuestProgression(event, player, 1, "SHEAR");
    }
}
