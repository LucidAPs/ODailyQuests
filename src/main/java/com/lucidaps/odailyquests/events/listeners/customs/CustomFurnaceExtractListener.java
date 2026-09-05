package com.lucidaps.odailyquests.events.listeners.customs;


import com.lucidaps.odailyquests.events.customs.CustomFurnaceExtractEvent;
import com.lucidaps.odailyquests.quests.player.progression.PlayerProgressor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CustomFurnaceExtractListener extends PlayerProgressor implements Listener {

    @EventHandler
    public void onCustomFurnaceExtractEvent(CustomFurnaceExtractEvent event) {
        setPlayerQuestProgression(event, event.getPlayer(), event.getAmount(), "COOK");
    }
}
