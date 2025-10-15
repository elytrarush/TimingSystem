package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version12 {

    public static void updateMySQL() throws SQLException {
        // Add leaderboard HUD related settings
        DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `leaderboardHud` tinyint(1) NOT NULL DEFAULT '1' AFTER `alternativeHud`;");
        DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `leaderboardCompareRecord` tinyint(1) NOT NULL DEFAULT '0' AFTER `leaderboardHud`;");
    }

    public static void updateSQLite() throws SQLException {
        // SQLite doesn't support ALTER TABLE ADD COLUMN with position, but ADD COLUMN is fine
        DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `leaderboardHud` INTEGER NOT NULL DEFAULT 1;");
        DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `leaderboardCompareRecord` INTEGER NOT NULL DEFAULT 0;");
    }
}
