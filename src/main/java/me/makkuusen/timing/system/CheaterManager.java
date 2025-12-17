package me.makkuusen.timing.system;

import co.aikar.idb.DB;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CheaterManager {

    private static final Map<UUID, String> CHEATERS = new ConcurrentHashMap<>();

    private CheaterManager() {}

    public static void load() {
        CHEATERS.clear();
        try {
            var rows = DB.getResults("SELECT `uuid`, `reason` FROM `cheaters`;");
            if (rows == null) return;
            for (var row : rows) {
                String uuidStr = row.getString("uuid");
                if (uuidStr == null) continue;
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String reason = row.getString("reason");
                    CHEATERS.put(uuid, reason);
                } catch (IllegalArgumentException ignored) {
                    // skip invalid UUIDs
                }
            }
        } catch (SQLException ignored) {
            // Table may not exist yet (first boot before migration), or DB not ready.
        }
    }

    public static boolean isCheater(UUID uuid) {
        return uuid != null && CHEATERS.containsKey(uuid);
    }

    public static Optional<String> getReason(UUID uuid) {
        return Optional.ofNullable(CHEATERS.get(uuid));
    }

    public static Set<UUID> getCheaters() {
        return Collections.unmodifiableSet(CHEATERS.keySet());
    }

    public static void ban(UUID uuid, String reason) {
        if (uuid == null) return;

        // Persist
        try {
            if (TimingSystem.getDatabase() instanceof me.makkuusen.timing.system.database.SQLiteDatabase) {
                DB.executeUpdate("INSERT OR REPLACE INTO `cheaters` (`uuid`, `reason`) VALUES(?, ?);", uuid.toString(), reason);
            } else {
                DB.executeUpdate("INSERT INTO `cheaters` (`uuid`, `reason`) VALUES(?, ?) ON DUPLICATE KEY UPDATE `reason` = VALUES(`reason`);", uuid.toString(), reason);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        CHEATERS.put(uuid, reason);
    }

    public static void unban(UUID uuid) {
        if (uuid == null) return;

        try {
            DB.executeUpdate("DELETE FROM `cheaters` WHERE `uuid` = ?;", uuid.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        CHEATERS.remove(uuid);
    }
}
