package com.lucidaps.odailyquests.tools.updater.config;

import com.lucidaps.odailyquests.ODailyQuests;

public interface IConfigUpdater {
    void apply(ODailyQuests plugin, String version);
}