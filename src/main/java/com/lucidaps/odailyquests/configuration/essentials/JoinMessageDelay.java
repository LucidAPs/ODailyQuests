package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;

public class JoinMessageDelay implements IConfigurable {

    private final ConfigurationFile configurationFile;

    private double delay;

    public JoinMessageDelay(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Override
    public void load() {
        delay = configurationFile.getConfig().getDouble("join_message_delay");
    }

    private static JoinMessageDelay getInstance() {
        return ConfigFactory.getConfig(JoinMessageDelay.class);
    }

    public static double getDelay() {
        return getInstance().delay;
    }
}
