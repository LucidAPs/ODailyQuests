package com.lucidaps.odailyquests.events.restart;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.tools.PluginLogger;

public class RestartHandler {

    private final ODailyQuests plugin;

    public RestartHandler(ODailyQuests oDailyQuests) {
        this.plugin = oDailyQuests;
    }

    public void setServerStopping() {
        if (plugin.isServerStopping()) return;

        PluginLogger.warn("Server is stopping. The datas will be saved in synchronous mode.");
        PluginLogger.warn("If you think this is a mistake, please contact the developer!");
        plugin.setServerStopping(true);
    }

    public void registerSubClasses() {
        plugin.getServer().getPluginManager().registerEvents(new RestartCommandListener(plugin), plugin);
    }
}
