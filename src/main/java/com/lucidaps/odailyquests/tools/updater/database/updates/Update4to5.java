package com.lucidaps.odailyquests.tools.updater.database.updates;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.updater.database.DatabaseUpdater;

/**
 * Marks the introduction of independent period storage. SQL tables are created
 * idempotently by SQLManager; legacy rows are imported lazily on first load.
 * YAML legacy data is likewise read as the server's former configured period
 * and written in the new layout on the next save.
 */
public final class Update4to5 extends DatabaseUpdater {

    public Update4to5(ODailyQuests plugin) {
        super(plugin);
    }

    @Override
    public void apply(ODailyQuests plugin, String version) {
        switch (com.lucidaps.odailyquests.configuration.essentials.Database.getMode()) {
            case MYSQL -> applyMySQL();
            case SQLITE -> applySQLite();
            case YAML -> applyYAML();
        }
        updateVersion(version);
    }

    @Override
    public void applyMySQL() {
        PluginLogger.info("Database update 4 -> 5: independent period tables are ready; legacy data will be imported on player load.");
    }

    @Override
    public void applySQLite() {
        PluginLogger.info("Database update 4 -> 5: independent period tables are ready; legacy data will be imported on player load.");
    }

    @Override
    public void applyYAML() {
        PluginLogger.info("Database update 4 -> 5: legacy quest data will be migrated to the "
                + QuestPeriods.getLegacyPeriod().getDisplayName() + " period on its next save.");
    }
}
