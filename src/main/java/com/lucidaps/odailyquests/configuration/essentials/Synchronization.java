package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;

public class Synchronization implements IConfigurable {

    private final ConfigurationFile configurationFile;
    private boolean isEnabled;

    public Synchronization(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Override
    public void load() {
        final String path = "synchronised_progression";
        isEnabled = configurationFile.getConfig().getBoolean(path);
    }

    private static Synchronization getInstance() {
        return ConfigFactory.getConfig(Synchronization.class);
    }

    public static boolean isSynchronised() {
        return getInstance().isEnabled;
    }
}
