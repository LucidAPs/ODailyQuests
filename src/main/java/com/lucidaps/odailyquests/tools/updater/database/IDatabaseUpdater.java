package com.lucidaps.odailyquests.tools.updater.database;

import com.lucidaps.odailyquests.ODailyQuests;

public interface IDatabaseUpdater {
    void apply(ODailyQuests plugin, String version);

    void applyMySQL();
    void applySQLite();
    void applyYAML();
}