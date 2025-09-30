package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version10 {

    public static void updateMySQL() throws SQLException {
        try {
            DB.executeUpdate("ALTER TABLE `ts_regions` ADD `rocketReward` int(11) NOT NULL DEFAULT 0 AFTER `spawn`;");
        } catch (SQLException e) {
            if (e.getErrorCode() != 1060) { // Duplicate column name
                throw e;
            }
        }
    }

    public static void updateSQLite() throws SQLException {
        try {
            DB.executeUpdate("ALTER TABLE `ts_regions` ADD COLUMN `rocketReward` INTEGER NOT NULL DEFAULT 0;");
        } catch (SQLException e) {
            // SQLite doesn't expose codes consistently; check message
            if (!e.getMessage().toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
    }
}