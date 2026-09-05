package com.lucidaps.odailyquests.events.listeners.item;

import com.lucidaps.odailyquests.configuration.essentials.Debugger;

import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;

public class EnchantItemListener extends PlayerProgressor implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchantItemEvent(EnchantItemEvent event) {
        if (event.isCancelled()) return;

        Debugger.write("EnchantItemListener: onEnchantItemEvent summoned by " + event.getEnchanter().getName() + " for " + event.getItem().getType() + ".");
        setPlayerQuestProgression(event, event.getEnchanter(), 1, "ENCHANT");
    }
}
