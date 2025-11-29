package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version14 {

    public static void updateMySQL() throws SQLException {
        try {
            // Add autoTpOnStopFlying column with default true (1)
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `autoTpOnStopFlying` tinyint(1) NOT NULL DEFAULT 1 AFTER `sendFinalLaps`;");
        } catch (SQLException e) {
            if (e.getErrorCode() != 1060) { // Duplicate column name
                throw e;
            }
        }
        try {
            // Update verbose default to 1 for new players (existing players keep their setting)
            DB.executeUpdate("ALTER TABLE `ts_players` ALTER COLUMN `verbose` SET DEFAULT 1;");
        } catch (SQLException e) {
            // Ignore if already set
        }
    }

    public static void updateSQLite() throws SQLException {
        try {
            // Add autoTpOnStopFlying column with default true (1)
            DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `autoTpOnStopFlying` INTEGER NOT NULL DEFAULT 1;");
        } catch (SQLException e) {
            // SQLite doesn't expose codes consistently; check message
            if (!e.getMessage().toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
        // SQLite doesn't support ALTER COLUMN, but the CREATE TABLE already handles defaults
    }
}
