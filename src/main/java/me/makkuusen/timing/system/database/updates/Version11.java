package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version11 {

    public static void updateMySQL() throws SQLException {
        try {
            DB.executeUpdate("ALTER TABLE `ts_regions` ADD `highlight` tinyint(1) NOT NULL DEFAULT 0 AFTER `rocketReward`;");
        } catch (SQLException e) {
            if (e.getErrorCode() != 1060) {
                throw e;
            }
        }
    }

    public static void updateSQLite() throws SQLException {
        try {
            DB.executeUpdate("ALTER TABLE `ts_regions` ADD COLUMN `highlight` INTEGER NOT NULL DEFAULT 0;");
        } catch (SQLException e) {
            var message = e.getMessage();
            if (message == null || !message.toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
    }
}
