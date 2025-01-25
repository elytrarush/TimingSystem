package me.makkuusen.timing.system.loneliness;

import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.api.events.BoatSpawnEvent;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LonelinessController implements Listener {

    private static Plugin plugin = null;
    private static final Set<UUID> ghostedPlayers = ConcurrentHashMap.newKeySet();
    private static final boolean DEBUG_ENABLED = false;

    public LonelinessController(Plugin plugin) {
        LonelinessController.plugin = plugin;
    }

    // Ghost Management Methods
    public static void ghost(TPlayer tPlayer, boolean ghost) {
        if (ghost) {
            ghostedPlayers.add(tPlayer.getUniqueId());
            updateGhostedPlayerVisibility(tPlayer.getPlayer(), true);
        } else {
            ghostedPlayers.remove(tPlayer.getUniqueId());
            updateGhostedPlayerVisibility(tPlayer.getPlayer(), false);
        }
    }

    private static void updateGhostedPlayerVisibility(Player ghostedPlayer, boolean shouldHide) {
        if (ghostedPlayer == null || !ghostedPlayer.isOnline()) return;

        Runnable visibilityTask = () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.equals(ghostedPlayer)) continue; // Do not hide from themselves

                Entity vehicle = ghostedPlayer.getVehicle();
                if (vehicle != null) {
                    if (shouldHide) {
                        player.hideEntity(plugin, vehicle);
                        player.hideEntity(plugin, ghostedPlayer);
                    } else {
                        player.showEntity(plugin, vehicle);
                        player.showEntity(plugin, ghostedPlayer);
                    }
                }
            }
        };
        plugin.getServer().getScheduler().runTask(plugin, visibilityTask);
    }

    // Boat Visibility Management Methods
    public static void updateBoatsVisibility(Player player, boolean inBoat) {
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Attempted to update visibility for invalid player");
            return;
        }

        Runnable visibilityTask = () -> {
            try {
                TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
                if (tPlayer == null) return;

                boolean isLonely = tPlayer.getSettings().isLonely();
                Set<Entity> currentBoats = getCurrentBoats(player);

                for (Entity boat : currentBoats) {
                    boolean isGhostedBoat = boat.getPassengers().stream()
                            .anyMatch(passenger -> passenger instanceof Player &&
                                    ghostedPlayers.contains(passenger.getUniqueId()));

                    // Determine visibility based on conditions
                    boolean shouldHideBoat = (isLonely && inBoat) || // Lonely player in boat - hide all other boats
                            (inBoat && isGhostedBoat); // Any player in boat - hide ghosted boats

                    if (shouldHideBoat) {
                        player.hideEntity(plugin, boat);
                        boat.getPassengers().forEach(passenger -> player.hideEntity(plugin, passenger));
                    } else {
                        player.showEntity(plugin, boat);
                        boat.getPassengers().forEach(passenger -> player.showEntity(plugin, passenger));
                    }
                }

                if (DEBUG_ENABLED) {
                    plugin.getLogger().info(String.format("Updated boat visibility for player %s: isLonely=%b, inBoat=%b",
                            player.getName(), isLonely, inBoat));
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error updating boat visibility: " + e.getMessage());
            }
        };
        plugin.getServer().getScheduler().runTask(plugin, visibilityTask);
    }

    private static Set<Entity> getCurrentBoats(Player player) {
        Set<Entity> boats = new HashSet<>();
        boats.addAll(player.getWorld().getEntitiesByClass(Boat.class).stream()
                .filter(boat -> !boat.getPassengers().contains(player))
                .collect(Collectors.toSet()));
        boats.addAll(player.getWorld().getEntitiesByClass(ChestBoat.class).stream()
                .filter(boat -> !boat.getPassengers().contains(player))
                .collect(Collectors.toSet()));
        return boats;
    }

    public static void updateBoatVisibilityToAllPlayers(Entity boat) {
        if (boat == null) {
            plugin.getLogger().warning("Attempted to update visibility for a null boat.");
            return;
        }

        boolean isGhostedBoat = boat.getPassengers().stream()
                .anyMatch(passenger -> passenger instanceof Player &&
                        ghostedPlayers.contains(passenger.getUniqueId()));

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (boat.getPassengers().contains(player)) continue; // Do not hide from passengers

            TPlayer tPlayer = TimingSystemAPI.getTPlayer(player.getUniqueId());
            if (tPlayer == null) continue;

            boolean isInBoat = player.isInsideVehicle() &&
                    (player.getVehicle() instanceof Boat || player.getVehicle() instanceof ChestBoat);
            boolean shouldHideBoat = (tPlayer.getSettings().isLonely() && isInBoat) ||
                    (isInBoat && isGhostedBoat);

            if (shouldHideBoat) {
                player.hideEntity(plugin, boat);
                boat.getPassengers().forEach(passenger -> player.hideEntity(plugin, passenger));
            } else {
                player.showEntity(plugin, boat);
                boat.getPassengers().forEach(passenger -> player.showEntity(plugin, passenger));
            }
        }
    }

    // Event Handlers
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (ghostedPlayers.contains(player.getUniqueId())) {
            updateGhostedPlayerVisibility(player, true);
        }

        if (player.isInsideVehicle() && (player.getVehicle() instanceof Boat || player.getVehicle() instanceof ChestBoat)) {
            updateBoatsVisibility(player, true);
            updateBoatVisibilityToAllPlayers(player.getVehicle());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnterBoat(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            updateBoatsVisibility(player, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExitBoat(VehicleExitEvent event) {
        if (event.isCancelled()) return;

        if (event.getExited() instanceof Player player) {
            updateBoatsVisibility(player, false);
            for (Player p: plugin.getServer().getOnlinePlayers()) {
                p.showEntity(plugin, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBoatSpawn(BoatSpawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> updateBoatVisibilityToAllPlayers(event.getBoat()), 5);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        boolean isInBoat = player.isInsideVehicle() &&
                (player.getVehicle() instanceof Boat || player.getVehicle() instanceof ChestBoat);
        updateBoatsVisibility(player, isInBoat);
    }

    public static boolean isGhosted(UUID uuid) {
        return ghostedPlayers.contains(uuid);
    }

    public void removeAllGhosted() {
        new ArrayList<>(ghostedPlayers).forEach(uuid -> {
            TPlayer tPlayer = TimingSystemAPI.getTPlayer(uuid);
            if (tPlayer != null) {
                ghost(tPlayer, false);
            }
        });
    }
}