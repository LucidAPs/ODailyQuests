package com.lucidaps.odailyquests.quests.player.progression.storage;

import com.lucidaps.odailyquests.configuration.essentials.Database;
import com.lucidaps.odailyquests.enums.StorageMode;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.quests.player.progression.storage.sql.SQLManager;
import com.lucidaps.odailyquests.quests.player.progression.storage.yaml.YamlManager;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlltimeLeaderboard {

    private static final long CACHE_DURATION_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final DatabaseManager databaseManager;
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    private volatile LeaderboardSnapshot storedSnapshot = LeaderboardSnapshot.empty();
    private volatile long lastRefreshMillis = 0L;

    public AlltimeLeaderboard(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<LeaderboardEntry> getEntry(int position) {
        if (position < 1) return Optional.empty();

        final LeaderboardSnapshot snapshot = getSnapshot();
        if (position > snapshot.entries().size()) return Optional.empty();

        return Optional.of(snapshot.entries().get(position - 1));
    }

    public int getPosition(UUID playerUuid) {
        if (playerUuid == null) return 0;

        return getSnapshot().positions().getOrDefault(playerUuid, 0);
    }

    public void clear() {
        storedSnapshot = LeaderboardSnapshot.empty();
        lastRefreshMillis = 0L;
        refreshInProgress.set(false);
    }

    public void refresh() {
        requestRefresh(true);
    }

    private LeaderboardSnapshot getSnapshot() {
        requestRefreshIfStale();
        return mergeActivePlayers(storedSnapshot);
    }

    private void requestRefreshIfStale() {
        final long now = System.currentTimeMillis();
        if (lastRefreshMillis > 0 && now - lastRefreshMillis < CACHE_DURATION_MILLIS) {
            return;
        }

        requestRefresh(false);
    }

    private void requestRefresh(boolean force) {
        if (!force && refreshInProgress.get()) {
            return;
        }

        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }

        final StorageMode mode = getStorageMode();
        if (mode == null) {
            refreshInProgress.set(false);
            return;
        }

        if (mode == StorageMode.YAML) {
            refreshYaml();
            return;
        }

        refreshSql();
    }

    private void refreshYaml() {
        final Runnable task = () -> {
            List<StoredLeaderboardEntry> entries = null;
            try {
                entries = loadYamlEntries();
            } catch (RuntimeException e) {
                PluginLogger.error("Impossible to load all-time leaderboard: " + e.getMessage());
            }

            finishRefresh(entries);
        };

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            TaskScheduler.runSync(task);
        }
    }

    private void refreshSql() {
        TaskScheduler.runAsync(() -> {
            List<StoredLeaderboardEntry> entries = null;
            try {
                entries = loadSqlEntries();
            } catch (RuntimeException e) {
                PluginLogger.error("Impossible to load all-time leaderboard: " + e.getMessage());
            }

            final List<StoredLeaderboardEntry> loadedEntries = entries;
            TaskScheduler.runSync(() -> finishRefresh(loadedEntries));
        });
    }

    private void finishRefresh(List<StoredLeaderboardEntry> entries) {
        try {
            if (entries != null) {
                storedSnapshot = LeaderboardSnapshot.from(resolveStoredEntries(entries));
                lastRefreshMillis = System.currentTimeMillis();
            }
        } finally {
            refreshInProgress.set(false);
        }
    }

    private List<StoredLeaderboardEntry> loadYamlEntries() {
        final YamlManager yamlManager = databaseManager.getYamlManager();
        if (yamlManager == null) {
            return null;
        }

        final FileConfiguration config = yamlManager.getProgressionFile().getConfig();
        final List<StoredLeaderboardEntry> entries = new ArrayList<>();

        for (String playerUuid : config.getKeys(false)) {
            final UUID uuid = parseUuid(playerUuid);
            if (uuid == null) continue;

            final ConfigurationSection playerSection = config.getConfigurationSection(playerUuid);
            if (playerSection == null) continue;

            entries.add(new StoredLeaderboardEntry(uuid, playerSection.getInt("totalAchievedQuests")));
        }

        return entries;
    }

    private List<StoredLeaderboardEntry> loadSqlEntries() {
        final SQLManager sqlManager = databaseManager.getSqlManager();
        if (sqlManager == null) {
            return null;
        }

        final List<StoredLeaderboardEntry> entries = new ArrayList<>();
        final String query = "SELECT `player_uuid`, `total_achieved_quests` FROM `odq_player`;";

        try (Connection connection = sqlManager.getConnection()) {
            if (connection == null) {
                PluginLogger.error("Impossible to load all-time leaderboard: database connection unavailable.");
                return null;
            }

            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final UUID uuid = parseUuid(resultSet.getString("player_uuid"));
                    if (uuid == null) continue;

                    entries.add(new StoredLeaderboardEntry(uuid, resultSet.getInt("total_achieved_quests")));
                }
            }
        } catch (SQLException e) {
            PluginLogger.error("Impossible to load all-time leaderboard: " + e.getMessage());
            return null;
        }

        return entries;
    }

    private List<LeaderboardEntry> resolveStoredEntries(List<StoredLeaderboardEntry> entries) {
        final List<LeaderboardEntry> resolvedEntries = new ArrayList<>();
        for (StoredLeaderboardEntry entry : entries) {
            resolvedEntries.add(new LeaderboardEntry(entry.playerUuid(), resolvePlayerName(entry.playerUuid()), entry.totalAchievedQuests()));
        }
        return resolvedEntries;
    }

    private LeaderboardSnapshot mergeActivePlayers(LeaderboardSnapshot snapshot) {
        if (!Bukkit.isPrimaryThread()) {
            return snapshot;
        }

        final Map<UUID, LeaderboardEntry> entriesByUuid = new HashMap<>();
        for (LeaderboardEntry entry : snapshot.entries()) {
            entriesByUuid.put(entry.playerUuid(), entry);
        }

        for (Map.Entry<String, PlayerQuests> activeEntry : QuestsManager.getActiveQuests().entrySet()) {
            final Player player = Bukkit.getPlayerExact(activeEntry.getKey());
            if (player == null || activeEntry.getValue() == null) continue;

            entriesByUuid.put(player.getUniqueId(), new LeaderboardEntry(
                    player.getUniqueId(),
                    player.getName(),
                    activeEntry.getValue().getTotalAchievedQuests()
            ));
        }

        return LeaderboardSnapshot.from(new ArrayList<>(entriesByUuid.values()));
    }

    private String resolvePlayerName(UUID playerUuid) {
        final Player onlinePlayer = Bukkit.getPlayer(playerUuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        final String playerName = offlinePlayer.getName();
        return (playerName == null || playerName.isBlank()) ? playerUuid.toString() : playerName;
    }

    private UUID parseUuid(String playerUuid) {
        if (playerUuid == null) return null;

        try {
            return UUID.fromString(playerUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private StorageMode getStorageMode() {
        try {
            return Database.getMode();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record StoredLeaderboardEntry(UUID playerUuid, int totalAchievedQuests) {
    }

    private record LeaderboardSnapshot(List<LeaderboardEntry> entries, Map<UUID, Integer> positions) {

        private static final Comparator<LeaderboardEntry> ENTRY_COMPARATOR =
                Comparator.comparingInt(LeaderboardEntry::totalAchievedQuests)
                        .reversed()
                        .thenComparing(entry -> entry.playerName().toLowerCase(Locale.ROOT))
                        .thenComparing(entry -> entry.playerUuid().toString());

        private static LeaderboardSnapshot empty() {
            return new LeaderboardSnapshot(List.of(), Map.of());
        }

        private static LeaderboardSnapshot from(List<LeaderboardEntry> entries) {
            final List<LeaderboardEntry> sortedEntries = new ArrayList<>(entries);
            sortedEntries.sort(ENTRY_COMPARATOR);

            final Map<UUID, Integer> positions = new HashMap<>();
            for (int i = 0; i < sortedEntries.size(); i++) {
                positions.put(sortedEntries.get(i).playerUuid(), i + 1);
            }

            return new LeaderboardSnapshot(List.copyOf(sortedEntries), Map.copyOf(positions));
        }

        private boolean isEmpty() {
            return entries.isEmpty();
        }
    }
}
