package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;

public class TimestampMode implements IConfigurable {

    private final ConfigurationFile configurationFile;

    public TimestampMode(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    private int mode;

    @Override
    public void load() {
        mode = configurationFile.getConfig().getInt("timestamp_mode");
    }

    private static TimestampMode getInstance() {
        return ConfigFactory.getConfig(TimestampMode.class);
    }

    public static int getTimestampMode() {
        return getInstance().mode;
    }
}

