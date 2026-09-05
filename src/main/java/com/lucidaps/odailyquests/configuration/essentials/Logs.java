package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;

public class Logs implements IConfigurable {

    private final ConfigurationFile configurationFile;

    private boolean isEnabled;

    public Logs(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Override
    public void load() {
        isEnabled = !configurationFile.getConfig().getBoolean("disable_logs");
    }

    public boolean isEnabledInternal() {
        return isEnabled;
    }

    private static Logs getInstance() {
        return ConfigFactory.getConfig(Logs.class);
    }

    public static boolean isEnabled() {
        return getInstance().isEnabledInternal();
    }
}
