package com.lucidaps.odailyquests.externs.hooks.points;

import com.lucidaps.odailyquests.tools.PluginUtils;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;

public class PlayerPointsHook {

    private PlayerPointsHook() {}

    private static PlayerPointsAPI playerPointsAPI;

    /**
     * Setup PlayerPoints API.
     */
    public static void setupPlayerPointsAPI() {
        if (PluginUtils.isPluginEnabled("PlayerPoints")) {
            playerPointsAPI = PlayerPoints.getInstance().getAPI();
        }
    }

    public static boolean isPlayerPointsSetup() {
        return playerPointsAPI != null;
    }

    public static PlayerPointsAPI getPlayerPointsAPI() {
        return playerPointsAPI;
    }
}
