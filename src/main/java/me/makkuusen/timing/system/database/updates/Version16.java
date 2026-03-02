package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version16 {

    public static void updateMySQL() throws SQLException {
        try {
            DB.executeUpdate("ALTER TABLE `ts_players` ADD `hidePlayers` tinyint(1) NOT NULL DEFAULT 0 AFTER `autoTpOnStopFlying`;");
        } catch (SQLException e) {
            if (e.getErrorCode() != 1060) { // Duplicate column name
                throw e;
            }
        }
    }

    public static void updateSQLite() throws SQLException {
        try {
            DB.executeUpdate("ALTER TABLE `ts_players` ADD COLUMN `hidePlayers` INTEGER NOT NULL DEFAULT 0;");
        } catch (SQLException e) {
            if (!e.getMessage().toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
    }
}
