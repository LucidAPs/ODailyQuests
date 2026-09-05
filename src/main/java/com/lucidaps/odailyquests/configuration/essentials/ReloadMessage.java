package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;

/**
 * Controls whether the plugin should notify players about their quest status
 * when data are reloaded (e.g. after an integrations-triggered reload).
 */
public class ReloadMessage implements IConfigurable {

    private final ConfigurationFile configurationFile;

    private boolean sendOnReload;

    public ReloadMessage(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Override
    public void load() {
        sendOnReload = configurationFile.getConfig().getBoolean("send_reload_message", true);
    }

    private static ReloadMessage getInstance() {
        return ConfigFactory.getConfig(ReloadMessage.class);
    }

    public static boolean shouldSendOnReload() {
        return getInstance().sendOnReload;
    }
}
