package com.lucidaps.odailyquests.events.listeners.entity;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.configuration.functionalities.SpawnerProgression;
import com.lucidaps.odailyquests.events.antiglitch.EntitySource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;

public class SpawnerSpawnListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawnerSpawnEvent(SpawnerSpawnEvent event) {
        if (event.isCancelled()) return;

        Debugger.write("Spawner spawn event: " + event.getEntity().getType());
        if (SpawnerProgression.isSpawnersProgressionDisabled()) {
            EntitySource.addEntityFromSpawner(event.getEntity());
        }
    }
}
