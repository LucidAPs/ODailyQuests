package com.lucidaps.odailyquests.configuration.integrations;

import com.lucidaps.odailyquests.configuration.ConfigFactory;
import com.lucidaps.odailyquests.configuration.IConfigurable;
import com.lucidaps.odailyquests.files.implementations.ConfigurationFile;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public class PapiPlaceholders implements IConfigurable {

    private static final String DEFAULT_STATUS_NOT_ACHIEVED = ChatColor.RED + "✗";
    private static final String DEFAULT_STATUS_ACHIEVED = ChatColor.GREEN + "✓";

    private final ConfigurationFile configurationFile;

    public PapiPlaceholders(ConfigurationFile configurationFile) {
        this.configurationFile = configurationFile;
    }

    private String statusNotAchieved;
    private String statusAchieved;

    @Override
    public void load() {
        final ConfigurationSection section = configurationFile.getConfig().getConfigurationSection("placeholders");

        if (section == null) {
            statusNotAchieved = DEFAULT_STATUS_NOT_ACHIEVED;
            statusAchieved = DEFAULT_STATUS_ACHIEVED;
            return;
        }

        statusNotAchieved = section.getString("status_not_achieved", DEFAULT_STATUS_NOT_ACHIEVED);
        statusAchieved = section.getString("status_achieved", DEFAULT_STATUS_ACHIEVED);
    }

    public static PapiPlaceholders getInstance() {
        return ConfigFactory.getConfig(PapiPlaceholders.class);
    }

    public String getStatusNotAchieved() {
        return statusNotAchieved;
    }

    public String getStatusAchieved() {
        return statusAchieved;
    }

    public static String parseStatus(boolean achieved) {
        final PapiPlaceholders papiPlaceholders = getInstance();
        return achieved ? papiPlaceholders.getStatusAchieved() : papiPlaceholders.getStatusNotAchieved();
    }
}
