package me.makkuusen.timing.system.database.updates;

import co.aikar.idb.DB;

import java.sql.SQLException;

public class Version15 {

    public static void updateMySQL() throws SQLException {
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `cheaters` (
                  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
                  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                  PRIMARY KEY (`uuid`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
                """);
    }

    public static void updateSQLite() throws SQLException {
        DB.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `cheaters` (
                  `uuid` TEXT PRIMARY KEY NOT NULL,
                  `reason` TEXT DEFAULT NULL
                );
                """);
    }
}
