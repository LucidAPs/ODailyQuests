package com.lucidaps.odailyquests.events.listeners.entity;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;
import com.lucidaps.odailyquests.enums.QuestsPermissions;
import com.lucidaps.odailyquests.events.antiglitch.EntitySource;

import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener extends PlayerProgressor implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeathEvent(EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        final Player killer = entity.getKiller();
        if (killer == null) return;

        if (isSpawnerKillWithoutBypass(entity, killer)) return;

        Debugger.write("EntityDeathListener: onEntityDeathEvent summoned by " + killer.getName() + " for " + entity.getType() + ".");
        setPlayerQuestProgression(event, killer, 1, "KILL");
    }

    /**
     * Returns true if entity comes from a spawner and the killer lacks the bypass permission.
     */
    private boolean isSpawnerKillWithoutBypass(LivingEntity entity, Player killer) {
        final boolean fromSpawner = EntitySource.isEntityFromSpawner(entity);
        final boolean hasBypass = killer.hasPermission(QuestsPermissions.QUESTS_PLAYER_BYPASS_SPAWNER.get());

        if (fromSpawner && !hasBypass) {
            Debugger.write("EntityDeathListener: Entity is from spawner, cancelling progression.");
            return true;
        }
        return false;
    }
}
