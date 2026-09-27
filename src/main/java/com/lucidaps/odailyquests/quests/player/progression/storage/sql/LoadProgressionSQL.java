package com.lucidaps.odailyquests.quests.player.progression.storage.sql;

import com.lucidaps.odailyquests.configuration.essentials.PlayerDataLoadDelay;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.QuestsManager;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.player.progression.ProgressionLoader;
import com.lucidaps.odailyquests.quests.player.progression.QuestLoaderUtils;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoadProgressionSQL extends ProgressionLoader {

    private record PeriodState(long timestamp, int achieved, int total, int rerolls) {
    }

    private record LoadedPeriod(PeriodState state, LinkedHashMap<AbstractQuest, Progression> quests,
                                Map<String, Integer> categoryTotals) {
    }

    private final SQLManager sqlManager;

    public LoadProgressionSQL(SQLManager sqlManager) {
        this.sqlManager = sqlManager;
    }

    public void loadProgression(String playerName, Map<String, PlayerQuests> ignored, boolean sendStatusMessage) {
        TaskScheduler.runSyncLater(() -> {
            final Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                handlePlayerDisconnected(playerName);
                return;
            }
            final String uuid = player.getUniqueId().toString();
            TaskScheduler.runAsync(() -> loadAsync(playerName, uuid, sendStatusMessage));
        }, TaskScheduler.ticksFromMillis(PlayerDataLoadDelay.getDelay()));
    }

    private void loadAsync(String playerName, String uuid, boolean sendStatusMessage) {
        try (Connection connection = sqlManager.getConnection()) {
            if (connection == null) {
                registerOnMainThread(playerName, 0, new EnumMap<>(QuestPeriod.class), sendStatusMessage);
                return;
            }

            final LegacyPlayer legacy = loadLegacyPlayer(connection, uuid);
            final int overallTotal = legacy == null ? 0 : legacy.total();
            final EnumMap<QuestPeriod, LoadedPeriod> loaded = loadPeriodData(connection, playerName, uuid);

            if (loaded.isEmpty() && legacy != null) {
                final QuestPeriod legacyPeriod = QuestPeriods.getLegacyPeriod();
                final LoadedPeriod legacyData = loadLegacyPeriod(connection, playerName, uuid, legacyPeriod, legacy);
                if (legacyData != null) loaded.put(legacyPeriod, legacyData);
            }

            registerOnMainThread(playerName, overallTotal, loaded, sendStatusMessage);
        } catch (SQLException exception) {
            PluginLogger.error("An error occurred while loading " + playerName + "'s quests: " + exception.getMessage());
            registerOnMainThread(playerName, 0, new EnumMap<>(QuestPeriod.class), sendStatusMessage);
        }
    }

    private record LegacyPlayer(long timestamp, int achieved, int total, int rerolls) {
    }

    private LegacyPlayer loadLegacyPlayer(Connection connection, String uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT player_timestamp, achieved_quests, total_achieved_quests, recent_rerolls
                FROM odq_player WHERE player_uuid = ?
                """)) {
            statement.setString(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new LegacyPlayer(
                        result.getLong("player_timestamp"),
                        result.getInt("achieved_quests"),
                        result.getInt("total_achieved_quests"),
                        result.getInt("recent_rerolls")
                );
            }
        }
    }

    private EnumMap<QuestPeriod, LoadedPeriod> loadPeriodData(Connection connection, String playerName, String uuid) throws SQLException {
        final EnumMap<QuestPeriod, LoadedPeriod> loaded = new EnumMap<>(QuestPeriod.class);
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT period, player_timestamp, achieved_quests, total_achieved_quests, recent_rerolls
                FROM odq_period_state WHERE player_uuid = ?
                """)) {
            statement.setString(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final QuestPeriod period = QuestPeriod.fromString(result.getString("period")).orElse(null);
                    if (period == null || !QuestPeriods.isEnabled(period)) continue;
                    final PeriodState state = new PeriodState(
                            result.getLong("player_timestamp"),
                            result.getInt("achieved_quests"),
                            result.getInt("total_achieved_quests"),
                            result.getInt("recent_rerolls")
                    );
                    loaded.put(period, loadStoredPeriod(connection, playerName, uuid, period, state));
                }
            }
        }
        return loaded;
    }

    private LoadedPeriod loadStoredPeriod(Connection connection, String playerName, String uuid,
                                          QuestPeriod period, PeriodState state) throws SQLException {
        final Map<String, Integer> totals = loadCategoryTotals(connection, uuid, period, true);
        if (QuestLoaderUtils.checkTimestamp(period, state.timestamp())) {
            return new LoadedPeriod(state, null, totals);
        }
        return new LoadedPeriod(state, loadQuests(connection, playerName, uuid, period, true), totals);
    }

    private LoadedPeriod loadLegacyPeriod(Connection connection, String playerName, String uuid,
                                          QuestPeriod period, LegacyPlayer legacy) throws SQLException {
        final PeriodState state = new PeriodState(legacy.timestamp(), legacy.achieved(), legacy.total(), legacy.rerolls());
        final Map<String, Integer> totals = loadCategoryTotals(connection, uuid, period, false);
        if (QuestLoaderUtils.checkTimestamp(period, state.timestamp())) {
            return new LoadedPeriod(state, null, totals);
        }
        return new LoadedPeriod(state, loadQuests(connection, playerName, uuid, period, false), totals);
    }

    private Map<String, Integer> loadCategoryTotals(Connection connection, String uuid,
                                                    QuestPeriod period, boolean periodAware) throws SQLException {
        final Map<String, Integer> totals = new HashMap<>();
        final String query = periodAware
                ? "SELECT category, total_achieved_quests FROM odq_period_category_stats WHERE player_uuid = ? AND period = ?"
                : "SELECT category, total_achieved_quests FROM odq_player_category_stats WHERE player_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uuid);
            if (periodAware) statement.setString(2, period.name());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) totals.put(result.getString("category"), result.getInt("total_achieved_quests"));
            }
        }
        return totals;
    }

    private LinkedHashMap<AbstractQuest, Progression> loadQuests(Connection connection, String playerName,
                                                                  String uuid, QuestPeriod period,
                                                                  boolean periodAware) throws SQLException {
        final LinkedHashMap<AbstractQuest, Progression> quests = new LinkedHashMap<>();
        final String query = periodAware
                ? "SELECT * FROM odq_period_progression WHERE player_uuid = ? AND period = ? ORDER BY player_quest_id"
                : "SELECT * FROM odq_progression WHERE player_uuid = ? ORDER BY player_quest_id";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uuid);
            if (periodAware) statement.setString(2, period.name());
            try (ResultSet result = statement.executeQuery()) {
                int slot = 1;
                while (result.next()) {
                    final int questIndex = result.getInt("quest_index");
                    final String category = result.getString("category");
                    final int required = result.getInt("required_amount");
                    if (required == 0) return null;

                    final AbstractQuest quest = QuestLoaderUtils.findQuest(period, playerName, category, questIndex, slot++);
                    if (quest == null) return null;
                    int selected = result.getInt("selected_required");
                    if (result.wasNull()) selected = -1;
                    if (isSelectedRequiredInvalid(quest, selected, playerName)) return null;
                    if (!quest.isRandomRequiredAmount() && required != Integer.parseInt(quest.getRequiredAmountRaw())) return null;

                    Double reward = result.getDouble("reward_amount");
                    if (result.wasNull()) reward = quest.getReward().resolveRewardAmount();
                    final Progression progression = new Progression(
                            required,
                            reward,
                            result.getInt("advancement"),
                            result.getBoolean("is_achieved")
                    );
                    if (selected != -1) progression.setSelectedRequiredIndex(selected);
                    quests.put(quest, progression);
                }
            }
        }
        return quests.isEmpty() ? null : quests;
    }

    private void registerOnMainThread(String playerName, int overallTotal,
                                      EnumMap<QuestPeriod, LoadedPeriod> loaded,
                                      boolean sendStatusMessage) {
        TaskScheduler.runSync(() -> {
            final Player player = Bukkit.getPlayer(playerName);
            if (player == null) return;

            for (QuestPeriod period : QuestPeriods.getEnabledPeriods()) {
                final LoadedPeriod stored = loaded.get(period);
                if (stored == null || stored.quests() == null) {
                    final Map<String, Integer> totals = stored == null ? new HashMap<>() : stored.categoryTotals();
                    final int periodTotal = stored == null ? 0 : stored.state().total();
                    QuestLoaderUtils.loadNewPlayerQuests(playerName, period, totals, periodTotal, false);
                    continue;
                }

                final PlayerQuests quests = new PlayerQuests(period, stored.state().timestamp(), stored.quests());
                quests.setAchievedQuests(stored.state().achieved());
                quests.setTotalAchievedQuests(stored.state().total());
                quests.setRecentRerolls(stored.state().rerolls());
                quests.setTotalAchievedQuestsByCategory(stored.categoryTotals());
                QuestsManager.registerPlayerPeriod(playerName, period, quests);
            }

            QuestsManager.markPlayerLoaded(playerName, overallTotal);
            if (sendStatusMessage) {
                final QuestPeriod first = QuestPeriods.getEnabledPeriods().getFirst();
                final PlayerQuests quests = QuestsManager.getPlayerQuests(playerName, first);
                if (quests != null) sendQuestStatusMessage(player, quests.getAchievedQuests(), quests);
            }
        });
    }
}
