package com.lucidaps.odailyquests.quests.player.progression.storage;

import java.util.UUID;

public record LeaderboardEntry(UUID playerUuid, String playerName, int totalAchievedQuests) {
}
