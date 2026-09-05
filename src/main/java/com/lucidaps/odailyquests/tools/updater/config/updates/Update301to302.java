package com.lucidaps.odailyquests.tools.updater.config.updates;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.tools.updater.config.ConfigUpdater;

public class Update301to302 extends ConfigUpdater {

    public Update301to302(ODailyQuests plugin) {
        super(plugin);
    }

    @Override
    public void apply(ODailyQuests plugin, String version) {
        setDefaultConfigItem("reroll_maximum", -1, config, configFile, false);
        setDefaultConfigItem("placeholders.status_achieved", "&a✓", config, configFile, false);
        setDefaultConfigItem("placeholders.status_not_achieved", "&c✗", config, configFile, false);

        updateVersion(version);
    }
}
