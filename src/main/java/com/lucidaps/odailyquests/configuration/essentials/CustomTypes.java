package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;

import java.util.HashSet;
import java.util.Set;

public class CustomTypes implements IConfigurable {

    private final ConfigurationFile configurationFile;
    private final Set<String> types = new HashSet<>();

    public CustomTypes(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    @Override
    public void load() {
        types.clear();

        for (String customType : configurationFile.getConfig().getStringList("custom_types")) {
            types.add(customType);
        }
    }

    private static CustomTypes getInstance() {
        return ConfigFactory.getConfig(CustomTypes.class);
    }

    public static Set<String> getCustomTypes() {
        return getInstance().types;
    }
}
