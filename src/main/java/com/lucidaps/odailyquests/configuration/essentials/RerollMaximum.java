package com.lucidaps.odailyquests.configuration.essentials;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;

/**
 * Configuration holder for the maximum number of quest rerolls allowed per player.
 * <p>
 * This value is loaded from the configuration file and exposed through
 * a static accessor for convenient use throughout the plugin.
 */
public class RerollMaximum implements IConfigurable {

    private final ConfigurationFile configurationFile;
    private int rerollMaximumConf;

    /**
     * Creates a new reroll maximum configuration loader.
     *
     * @param configurationFile the configuration file providing the reroll maximum value
     */
    public RerollMaximum(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    /**
     * Loads the reroll maximum value from the configuration file.
     * <p>
     * The value is read from the "reroll_maximum" configuration path.
     */
    @Override
    public void load() {
        final String path = "reroll_maximum";
        rerollMaximumConf = configurationFile.getConfig().getInt(path);
    }

    /**
     * Retrieves the singleton configuration instance managed by the configuration factory.
     *
     * @return the loaded RerollMaximum configuration instance
     */
    private static RerollMaximum getInstance() {
        return ConfigFactory.getConfig(RerollMaximum.class);
    }

    /**
     * Returns the maximum number of rerolls allowed for a player.
     *
     * @return the configured maximum reroll count
     */
    public static int getMaxRerolls() {
        return getInstance().rerollMaximumConf;
    }
}
