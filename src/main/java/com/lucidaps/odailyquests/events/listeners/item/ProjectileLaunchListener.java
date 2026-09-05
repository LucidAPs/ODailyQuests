package com.lucidaps.odailyquests.events.listeners.item;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;

import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class ProjectileLaunchListener extends PlayerProgressor implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.isCancelled()) return;

        if (event.getEntity().getShooter() instanceof Player player) {
            Debugger.write("ProjectileLaunchListener: onProjectileLaunch summoned by " + player.getName() + " for " + event.getEntity().getType() + ".");
            setPlayerQuestProgression(event, player, 1, "LAUNCH");
        }
    }
}
