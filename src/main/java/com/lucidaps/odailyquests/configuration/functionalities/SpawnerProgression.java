package com.lucidaps.odailyquests.configuration.functionalities;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;

public class SpawnerProgression implements IConfigurable {

    private final ConfigurationFile configurationFile;

    public SpawnerProgression(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    private boolean disabled = false;

    @Override
    public void load() {
        final String path = "disable_spawners_progression";
        disabled = configurationFile.getConfig().getBoolean(path);
    }

    private static SpawnerProgression getInstance() {
        return ConfigFactory.getConfig(SpawnerProgression.class);
    }

    public static boolean isSpawnersProgressionDisabled() {
        return getInstance().disabled;
    }
}
