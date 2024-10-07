package me.makkuusen.timing.system.loneliness;

import org.bukkit.Bukkit;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class GhostingController implements Listener {
    public static List<Player> ghostedPlayers = new ArrayList<>();

    private static Plugin plugin = null;

    public GhostingController(Plugin plugin) {
        this.plugin = plugin;
    }

    public static void ghostPlayer(Player player) {
        ghostedPlayers.add(player);
    }

    public static void unghostPlayer(Player player) {
        ghostedPlayers.remove(player);
    }

    public static boolean isGhosted(Player player) {
        return ghostedPlayers.contains(player);
    }

    public static void toggleGhosted(Player player) {
        if (isGhosted(player)) {
            unghostPlayer(player);
            if (isPlayerInBoat(player)) {
                updateGhostedToAll(player, false);
            }
        } else {
            ghostPlayer(player);
            if (isPlayerInBoat(player)) {
                updateGhostedToAll(player, true);
            }
        }
    }

    // hide or show a boat to all players in boats
    public static void updateGhostedToAll(Player player, Boolean shouldHide) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> isPlayerInBoat(p) && p != player)
                .forEach(p -> updateBoatVisibilityToSpecificDriver(p, player, shouldHide));
    }

    // hide or show all ghosted boats to a specific player
    public static void updateAllGhostedToPlayer(Player player, Boolean shouldHide) {
        ghostedPlayers.stream()
                .filter(p -> p != player)
                .forEach(p -> updateBoatVisibilityToSpecificDriver(player, p, shouldHide));
    }

    //hide or show specific boat to specific player
    public static void updateBoatVisibilityToSpecificDriver(Player player, Player target, Boolean shouldHide) {
        if (shouldHide) {
            if (isPlayerInBoat(target)) {
                player.hideEntity(plugin, target.getVehicle());
                for (Entity passenger : target.getVehicle().getPassengers()) {
                    player.hideEntity(plugin, passenger);
                }
            } else {
                player.hideEntity(plugin, target);
            }
        } else {
            if (isPlayerInBoat(target)) {
                player.showEntity(plugin, target.getVehicle());
                for (Entity passenger : target.getVehicle().getPassengers()) {
                    player.showEntity(plugin, passenger);
                }
            } else {
                player.showEntity(plugin, target);
            }
        }
    }

    public static boolean isPlayerInBoat(Player player) {
        return player.isInsideVehicle() && (player.getVehicle() instanceof Boat || player.getVehicle() instanceof ChestBoat);
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isGhosted(player)) {
                    updateGhostedToAll(player, true);
                }
                updateAllGhostedToPlayer(player, true);
            }, 5);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getExited() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isGhosted(player)) {
                    updateGhostedToAll(player, false);
                }
                updateAllGhostedToPlayer(player, false);
            }, 5);
        }
    }

    @EventHandler
    public void onServerLeave(PlayerQuitEvent event) {
        if (isGhosted(event.getPlayer())) {
            unghostPlayer(event.getPlayer());
        }
    }
}

