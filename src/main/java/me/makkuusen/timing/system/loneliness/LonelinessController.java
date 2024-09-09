package me.makkuusen.timing.system.loneliness;

import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.api.events.BoatSpawnEvent;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.stream.Collectors;

public class LonelinessController implements Listener {

    private static Plugin plugin = null;

    public LonelinessController(Plugin plugin) {
        this.plugin = plugin;
    }

    public static void updateBoatsVisibility(Player player, boolean shouldHide) {
        Runnable visibilityTask = () -> {
            Set<Entity> boats = player.getWorld().getEntitiesByClass(Boat.class).stream()
                    .filter(boat -> !boat.getPassengers().contains(player))
                    .collect(Collectors.toSet());
            boats.addAll(player.getWorld().getEntitiesByClass(ChestBoat.class).stream()
                    .filter(boat -> !boat.getPassengers().contains(player))
                    .collect(Collectors.toSet()));

            for (Entity boat : boats) {
                if (shouldHide) {
                    player.hideEntity(plugin, boat);
                } else {
                    player.showEntity(plugin, boat);
                }
                boat.getPassengers().forEach(passenger -> {
                    if (shouldHide) {
                        player.hideEntity(plugin, passenger);
                    } else {
                        player.showEntity(plugin, passenger);
                    }
                });
            }
        };
        plugin.getServer().getScheduler().runTask(plugin, visibilityTask);
    }

    public static void updateBoatVisibilityToLonelyDrivers(Entity boat, boolean shouldHide) {
        if (boat == null) {
            plugin.getLogger().warning("Attempted to update visibility for a null boat.");
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
            if (tPlayer != null && !boat.getPassengers().contains(player) && tPlayer.getSettings().isLonely()
                    && player.isInsideVehicle() && (player.getVehicle() instanceof Boat || player.getVehicle() instanceof ChestBoat)) {
                if (shouldHide) {
                    player.hideEntity(plugin, boat);
                } else {
                    player.showEntity(plugin, boat);
                }
                boat.getPassengers().forEach(passenger -> {
                    if (shouldHide) {
                        player.hideEntity(plugin, passenger);
                    } else {
                        player.showEntity(plugin, passenger);
                    }
                });
            }
        }
    }

    public static void updateBoatVisibilityToAllDrivers(Entity boat, boolean shouldHide) {
        if (boat == null) {
            plugin.getLogger().warning("Attempted to update visibility for a null boat.");
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
            if (tPlayer != null && !boat.getPassengers().contains(player) && player.isInsideVehicle() && (player.getVehicle() instanceof Boat || player.getVehicle() instanceof ChestBoat)) {
                if (shouldHide) {
                    player.hideEntity(plugin, boat);
                } else {
                    player.showEntity(plugin, boat);
                }
                boat.getPassengers().forEach(passenger -> {
                    if (shouldHide) {
                        player.hideEntity(plugin, passenger);
                    } else {
                        player.showEntity(plugin, passenger);
                    }
                });
            }
        }
    }

    @EventHandler
    public void onEnterBoat(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
            if (tPlayer != null && tPlayer.getSettings().isLonely()) {
                updateBoatsVisibility(player, true);
            }
        }
    }

    @EventHandler
    public void onExitBoat(VehicleExitEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getExited() instanceof Player player) {
            TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
            if (tPlayer != null && tPlayer.getSettings().isLonely()) {
                updateBoatsVisibility(player, false);
            }
        }
    }

    @EventHandler
    public void onBoatSpawn(BoatSpawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> updateBoatVisibilityToLonelyDrivers(event.getBoat(), true), 5);
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
        if (tPlayer != null && tPlayer.getSettings().isLonely() && player.isInsideVehicle()
                && (player.getVehicle() instanceof Boat || player.getVehicle() instanceof ChestBoat)) {
            updateBoatsVisibility(player, true);
        }
    }
}
