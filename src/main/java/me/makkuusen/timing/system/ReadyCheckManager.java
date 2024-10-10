package me.makkuusen.timing.system;

import lombok.Getter;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.heat.ReadyCheck;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class ReadyCheckManager {

    @Getter
    private static HashMap<UUID, ReadyCheck> readyChecks = new HashMap<UUID, ReadyCheck>();

    // TODO display ready check title to all ready check players
    // TODO display ready check text to all ready check players


    public static void remove(UUID initiatorUuid) {
        readyChecks.remove(initiatorUuid);
    }

    public static boolean isReadyCheckInProgress(Player player) {
        return readyChecks.containsKey(player.getUniqueId());
    }

    public static ReadyCheck getReadyCheck(Player player) {
        return readyChecks.get(player.getUniqueId());
    }

    public static ReadyCheck createReadyCheck(Player player, Heat heat) {
        ReadyCheck readyCheck = new ReadyCheck(player, heat);
        readyChecks.put(player.getUniqueId(), readyCheck);
        return readyCheck;
    }

    public static void playerIsReady(Player player) {
        for (ReadyCheck readyCheck : readyChecks.values()) {
            readyCheck.playerIsReady(player);
        }
    }

}
