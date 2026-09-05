package com.lucidaps.odailyquests.events.listeners.integrations.nexo;

import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.integrations.NexoEnabled;
import com.lucidaps.odailyquests.tools.PluginLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class NexoItemsLoadedListener implements Listener {

    private final ODailyQuests plugin;

    public NexoItemsLoadedListener(ODailyQuests plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNexoItemsLoaded(NexoItemsLoadedEvent event) {
        PluginLogger.info("Nexo updated its data. Reloading...");
        NexoEnabled.setLoaded(true);
        plugin.getReloadService().reload();
    }
}
