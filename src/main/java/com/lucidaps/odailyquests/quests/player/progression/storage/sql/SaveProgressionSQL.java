package com.lucidaps.odailyquests.quests.player.progression.storage.sql;

import com.lucidaps.odailyquests.configuration.essentials.Database;
import com.lucidaps.odailyquests.configuration.essentials.Logs;
import com.lucidaps.odailyquests.configuration.essentials.QuestPeriods;
import com.lucidaps.odailyquests.enums.QuestPeriod;
import com.lucidaps.odailyquests.enums.SQLQuery;
import com.lucidaps.odailyquests.enums.StorageMode;
import com.lucidaps.odailyquests.quests.player.PlayerQuests;
import com.lucidaps.odailyquests.quests.player.progression.Progression;
import com.lucidaps.odailyquests.quests.player.progression.storage.PlayerQuestData;
import com.lucidaps.odailyquests.quests.types.AbstractQuest;
import com.lucidaps.odailyquests.tools.PluginLogger;
import com.lucidaps.odailyquests.tools.TaskScheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

public class SaveProgressionSQL {

    private final SQLManager sqlManager;

    public SaveProgressionSQL(SQLManager sqlManager) {
        this.sqlManager = sqlManager;
    }

    public void saveProgression(String playerName, String playerUuid, PlayerQuestData data, boolean forceSync) {
        if (data == null) return;
        if (forceSync) save(playerName, playerUuid, data);
        else TaskScheduler.runAsync(() -> save(playerName, playerUuid, data));
    }

    /** Legacy converter adapter: imported data belongs to the server's former single period. */
    public void saveProgression(String playerName, String playerUuid, PlayerQuests playerQuests, boolean forceSync) {
        final QuestPeriod legacyPeriod = QuestPeriods.getLegacyPeriod();
        saveProgression(
                playerName,
                playerUuid,
                new PlayerQuestData(
                        playerQuests.getTotalAchievedQuests(),
                        Map.of(legacyPeriod, copyForPeriod(playerQuests, legacyPeriod))
                ),
                forceSync
        );
    }

    private PlayerQuests copyForPeriod(PlayerQuests source, QuestPeriod period) {
        if (source.getPeriod() == period) return source;

        final PlayerQuests converted = new PlayerQuests(period, source.getTimestamp(), source.getQuests());
        converted.setAchievedQuests(source.getAchievedQuests());
        converted.setTotalAchievedQuests(source.getTotalAchievedQuests());
        converted.setRecentRerolls(source.getRecentlyRolled());
        converted.setTotalAchievedQuestsByCategory(source.getTotalAchievedQuestsByCategory());
        return converted;
    }

    private void save(String playerName, String playerUuid, PlayerQuestData data) {
        try (Connection connection = sqlManager.getConnection()) {
            if (connection == null) return;
            final boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                saveOverall(connection, playerUuid, data);
                for (Map.Entry<QuestPeriod, PlayerQuests> entry : data.periods().entrySet()) {
                    savePeriod(connection, playerUuid, entry.getKey(), entry.getValue());
                }
                connection.commit();
                if (Logs.isEnabled()) PluginLogger.info(playerName + "'s data saved.");
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException exception) {
            PluginLogger.error("An error occurred while saving " + playerName + " data: " + exception.getMessage());
        }
    }

    private void saveOverall(Connection connection, String playerUuid, PlayerQuestData data) throws SQLException {
        final PlayerQuests daily = data.periods().get(QuestPeriod.DAILY);
        final PlayerQuests fallback = daily != null
                ? daily
                : data.periods().values().stream().findFirst().orElse(null);
        final long timestamp = fallback == null ? System.currentTimeMillis() : fallback.getTimestamp();
        final int achieved = fallback == null ? 0 : fallback.getAchievedQuests();
        final int rerolls = fallback == null ? 0 : fallback.getRecentlyRolled();
        final String query = Database.getMode() == StorageMode.MYSQL
                ? SQLQuery.MYSQL_SAVE_PLAYER.getQuery()
                : SQLQuery.SQLITE_SAVE_PLAYER.getQuery();

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerUuid);
            statement.setLong(2, timestamp);
            statement.setInt(3, achieved);
            statement.setInt(4, data.overallLifetimeTotal());
            statement.setInt(5, rerolls);
            statement.executeUpdate();
        }
    }

    private void savePeriod(Connection connection, String playerUuid, QuestPeriod period, PlayerQuests quests) throws SQLException {
        final String periodKey = period.name();
        try (PreparedStatement deleteProgress = connection.prepareStatement(
                "DELETE FROM odq_period_progression WHERE player_uuid = ? AND period = ?");
             PreparedStatement deleteStats = connection.prepareStatement(
                     "DELETE FROM odq_period_category_stats WHERE player_uuid = ? AND period = ?")) {
            deleteProgress.setString(1, playerUuid);
            deleteProgress.setString(2, periodKey);
            deleteProgress.executeUpdate();
            deleteStats.setString(1, playerUuid);
            deleteStats.setString(2, periodKey);
            deleteStats.executeUpdate();
        }

        final String stateQuery = Database.getMode() == StorageMode.MYSQL
                ? """
                  INSERT INTO odq_period_state (player_uuid, period, player_timestamp, achieved_quests, total_achieved_quests, recent_rerolls)
                  VALUES (?, ?, ?, ?, ?, ?)
                  ON DUPLICATE KEY UPDATE player_timestamp=VALUES(player_timestamp), achieved_quests=VALUES(achieved_quests),
                  total_achieved_quests=VALUES(total_achieved_quests), recent_rerolls=VALUES(recent_rerolls)
                  """
                : """
                  INSERT OR REPLACE INTO odq_period_state (player_uuid, period, player_timestamp, achieved_quests, total_achieved_quests, recent_rerolls)
                  VALUES (?, ?, ?, ?, ?, ?)
                  """;
        try (PreparedStatement statement = connection.prepareStatement(stateQuery)) {
            statement.setString(1, playerUuid);
            statement.setString(2, periodKey);
            statement.setLong(3, quests.getTimestamp());
            statement.setInt(4, quests.getAchievedQuests());
            statement.setInt(5, quests.getTotalAchievedQuests());
            statement.setInt(6, quests.getRecentlyRolled());
            statement.executeUpdate();
        }

        final String progressQuery = """
                INSERT INTO odq_period_progression
                (player_uuid, period, player_quest_id, quest_index, category, advancement, required_amount, reward_amount, is_achieved, selected_required)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(progressQuery)) {
            int slot = 0;
            for (Map.Entry<AbstractQuest, Progression> entry : quests.getQuests().entrySet()) {
                final AbstractQuest quest = entry.getKey();
                final Progression progression = entry.getValue();
                statement.setString(1, playerUuid);
                statement.setString(2, periodKey);
                statement.setInt(3, slot++);
                statement.setInt(4, quest.getQuestIndex());
                statement.setString(5, quest.getCategoryName());
                statement.setInt(6, progression.getAdvancement());
                statement.setInt(7, progression.getRequiredAmount());
                statement.setDouble(8, progression.hasRewardAmount()
                        ? progression.getRewardAmount()
                        : quest.getReward().resolveRewardAmount());
                statement.setBoolean(9, progression.isAchieved());
                if (progression.getSelectedRequiredIndex() < 0) statement.setNull(10, Types.INTEGER);
                else statement.setInt(10, progression.getSelectedRequiredIndex());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO odq_period_category_stats (player_uuid, period, category, total_achieved_quests)
                VALUES (?, ?, ?, ?)
                """)) {
            for (Map.Entry<String, Integer> entry : quests.getTotalAchievedQuestsByCategory().entrySet()) {
                statement.setString(1, playerUuid);
                statement.setString(2, periodKey);
                statement.setString(3, entry.getKey());
                statement.setInt(4, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
