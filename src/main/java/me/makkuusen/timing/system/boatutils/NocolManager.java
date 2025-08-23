package me.makkuusen.timing.system.boatutils;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;

public class NocolManager {
    private static final short PACKET_ID_NOCOL = 27;
    private static final short NOCOL_MODE_ON_ID = 2;
    private static final short NOCOL_MODE_OFF_ID = 0;

    private static void sendShortAndShortPacket(Player player, short packetId, short value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeShort(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE,
                    "Failed to serialize and send packet " + packetId + " for player " + player.getName(), e);
        }
    }

    public static boolean playerCanUseNocol(Player player) {
        if (player == null) return false;
        TPlayer tplayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
        if (!tplayer.hasBoatUtils()) return false;
        return (tplayer.getBoatUtilsVersion() >= 10);
    }

    public static void setCollisionMode(Player player, boolean shouldCollide) {
        if (!playerCanUseNocol(player)) return;
        if (player == null) return;

        if (shouldCollide) {
            sendShortAndShortPacket(player, PACKET_ID_NOCOL, NOCOL_MODE_OFF_ID);
            return;
        } else {
            sendShortAndShortPacket(player, PACKET_ID_NOCOL, NOCOL_MODE_ON_ID);
            return;
        }
    }
}
