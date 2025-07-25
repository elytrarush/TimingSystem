package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version9 {

    public static void updateMySQL() throws SQLException {
        // Add the column to link tracks to custom modes, if it doesn't exist
        try {
            DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_custom_boatutils_modes` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
              `data` TEXT COLLATE utf8mb4_unicode_ci NOT NULL,
              PRIMARY KEY (`id`),
              UNIQUE KEY `name` (`name`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);

            DB.executeUpdate("ALTER TABLE `ts_tracks` ADD `customBoatUtilsModeId` int(11) DEFAULT NULL AFTER `boatUtilsMode`;");
        } catch (SQLException e) {
            // Error 1060: Duplicate column name. We can ignore this.
            if (e.getErrorCode() != 1060) {
                throw e;
            }
        }
    }

    public static void updateSQLite() throws SQLException {
        // Add the column to link tracks to custom modes, if it doesn't exist
        try {
            DB.executeUpdate("""
            CREATE TABLE IF NOT EXISTS `ts_custom_boatutils_modes` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `name` TEXT NOT NULL UNIQUE,
              `data` TEXT NOT NULL
            );
            """);

            DB.executeUpdate("ALTER TABLE `ts_tracks` ADD COLUMN `customBoatUtilsModeId` INTEGER DEFAULT NULL;");
        } catch (SQLException e) {
            // It's safer to check for the error message in SQLite
            if (!e.getMessage().toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
    }
}