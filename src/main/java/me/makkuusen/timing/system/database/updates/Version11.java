package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version11 {

    public static void updateMySQL() throws SQLException {
        // Add alternativeHud column to ts_players if it does not exist
        DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `alternativeHud` tinyint(1) NOT NULL DEFAULT '0' AFTER `compactScoreboard`;");
    }

    public static void updateSQLite() throws SQLException {
        // SQLite does not support IF NOT EXISTS for ADD COLUMN in all versions; attempt to add, ignore if exists
        try {
            DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `alternativeHud` INTEGER NOT NULL DEFAULT 0;");
        } catch (Exception ignored) {
            // Column may already exist
        }
    }
}
