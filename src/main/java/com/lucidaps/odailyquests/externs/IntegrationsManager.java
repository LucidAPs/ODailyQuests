package com.lucidaps.odailyquests.externs;

import com.lucidaps.odailyquests.ODailyQuests;
import com.lucidaps.odailyquests.externs.hooks.Protection;
import com.lucidaps.odailyquests.externs.hooks.eco.VaultHook;
import com.lucidaps.odailyquests.externs.hooks.placeholders.PAPIExpansion;
import com.lucidaps.odailyquests.externs.hooks.points.PlayerPointsHook;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.PluginUtils;

public class IntegrationsManager {

    private final ODailyQuests oDailyQuests;

    public IntegrationsManager(ODailyQuests oDailyQuests) {
        this.oDailyQuests = oDailyQuests;
    }

    /**
     * Load all dependencies.
     */
    public void loadAllDependencies() {
        safeHook("Vault", this::loadVault);
        safeHook("PlayerPoints", this::loadPointsPlugin);
        safeHook("PlaceholderAPI", this::loadPAPI);
        safeHook("Protection", () -> new Protection().load());
    }

    private void safeHook(String name, Runnable hook) {
        try {
            hook.run();
        } catch (Exception err) {
            PluginLogger.warn("Failed to hook into " + name + ". Is the plugin installed and up to date?");
        }
    }

    /**
     * Hook - PlayerPoints
     */
    private void loadPointsPlugin() {
        PlayerPointsHook.setupPlayerPointsAPI();
    }

    /**
     * Hook - Vault
     */
    private void loadVault() {
        VaultHook.setupEconomy();
    }

    /**
     * Hook - PlaceholderAPI
     */
    private void loadPAPI() {
        if (PluginUtils.isPluginEnabled("PlaceholderAPI")) {
            new PAPIExpansion(
                    oDailyQuests.getInterfacesManager().getPlayerQuestsInterface(),
                    oDailyQuests.getDatabaseManager().getAlltimeLeaderboard()
            ).register();
        }
    }

}
