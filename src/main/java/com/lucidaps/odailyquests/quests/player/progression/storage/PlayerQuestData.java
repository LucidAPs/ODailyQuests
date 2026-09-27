package com.lucidaps.odailyquests.quests.player.progression.storage;

import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;

import java.util.EnumMap;
import java.util.Map;

/** Snapshot of all persisted quest data for one player. */
public record PlayerQuestData(int overallLifetimeTotal, Map<QuestPeriod, PlayerQuests> periods) {

    public PlayerQuestData {
        periods = Map.copyOf(periods);
    }

    public static PlayerQuestData capture(String playerName) {
        final EnumMap<QuestPeriod, PlayerQuests> values = new EnumMap<>(QuestPeriod.class);
        for (QuestPeriod period : QuestPeriod.values()) {
            final PlayerQuests playerQuests = QuestsManager.getPlayerQuests(playerName, period);
            if (playerQuests != null) values.put(period, playerQuests);
        }
        return new PlayerQuestData(QuestsManager.getOverallLifetimeTotal(playerName), values);
    }
}
