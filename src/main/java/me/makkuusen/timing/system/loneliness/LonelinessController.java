package me.makkuusen.timing.system.loneliness;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.BoatSpawnEvent;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.DriverState;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
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
    
    public static void updatePlayersVisibility(Player player) {

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isInsideVehicle()) {
                showAllOthers(player);
                return;
            }

            if (TimeTrialController.timeTrials.containsKey(player.getUniqueId())) {
                hideAllOthers(player);
                return;
            }

            // All non heat related code should be above this line
            var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
            if (!maybeDriver.isPresent()) {
                return;
            }

            Driver driver = maybeDriver.get();
            Heat heat = driver.getHeat();

            // Driver is not participating
            if (driver.getState() == DriverState.DISQUALIFIED || driver.getState() == DriverState.SETUP || driver.getState() == DriverState.FINISHED) {
                return;
            }

            if (heat.getLonely()) {
                hideAllOthers(player);
                return;
            }

            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }

                // Do not hide players in the same non loneliness heat, unless they are ghosted
                maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(p.getUniqueId());
                if (maybeDriver.isPresent() && maybeDriver.get().getHeat().getId() == heat.getId() && !ghostedPlayers.contains(p.getUniqueId()) && !(DeltaGhostingController.isDeltaGhosted(driver, maybeDriver.get()))) {
                    showPlayerAndCustomBoat(player, p);
                    continue;
                }

                hidePlayerAndCustomBoat(player, p);
            }
        }, 5L);
    }

    private static void showPlayerAndCustomBoat(Player player, Player boatOwner) {
        if (boatOwner.isInsideVehicle() && (boatOwner.getVehicle() instanceof Boat || boatOwner.getVehicle() instanceof ChestBoat)) {
            if (TimingSystem.configuration.isCustomBoatsAddOnEnabled() && !boatOwner.getVehicle().getPassengers().isEmpty()) {
                for (Entity e : boatOwner.getVehicle().getPassengers()) {
                    if (e instanceof Villager) {
                        player.showEntity(plugin, e);
                    }
                }
            }
            player.showEntity(plugin, boatOwner.getVehicle());
        }
        player.showEntity(plugin, boatOwner);
    }

    private static void hidePlayerAndCustomBoat(Player player, Player boatOwner) {
        if (boatOwner.isInsideVehicle() && (boatOwner.getVehicle() instanceof Boat || boatOwner.getVehicle() instanceof ChestBoat)) {
            if (TimingSystem.configuration.isCustomBoatsAddOnEnabled() && !boatOwner.getVehicle().getPassengers().isEmpty()) {
                for (Entity e : boatOwner.getVehicle().getPassengers()) {
                    if (e instanceof Villager) {
                        player.hideEntity(plugin, e);
                    }
                }
            }
            player.hideEntity(plugin, boatOwner.getVehicle());
        }
        player.hideEntity(plugin, boatOwner);
    }

    private static void showAllOthers(Player player) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            showPlayerAndCustomBoat(player, p);
        }
    }

    private static void hideAllOthers(Player player) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            hidePlayerAndCustomBoat(player, p);
        }
    }

    public static void updatePlayerVisibility(Player player) {
         Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                // if p is not in a boat there is no situation where they should not see player
                if (!(p.getVehicle() instanceof Boat) && !(p.getVehicle() instanceof ChestBoat)) {
                    showPlayerAndCustomBoat(p, player);
                    continue;
                }

                if (TimeTrialController.timeTrials.containsKey(p.getUniqueId())) {
                    hidePlayerAndCustomBoat(p, player);
                    continue;
                }

                var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(p.getUniqueId());
                if (!maybeDriver.isPresent()) {
                    showPlayerAndCustomBoat(p, player);
                    continue;
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

                // Driver is not participating
                if (d.getState() == DriverState.DISQUALIFIED || d.getState() == DriverState.SETUP || d.getState() == DriverState.FINISHED) {
                    showPlayerAndCustomBoat(p, player);
                    continue;
                }

                maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());

                if (!maybeDriver.isPresent()) {
                    hidePlayerAndCustomBoat(p, player);
                    continue;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error updating boat visibility: " + e.getMessage());
            }
        };
        plugin.getServer().getScheduler().runTask(plugin, visibilityTask);
    }

                if (maybeDriver.get().getHeat().getId() != heat.getId()) {
                    hidePlayerAndCustomBoat(p, player);
                    continue;
                }

                if (heat.getLonely()) {
                    hidePlayerAndCustomBoat(p, player);
                    continue;
                }

                if (ghostedPlayers.contains(player.getUniqueId())) {
                    hidePlayerAndCustomBoat(p, player);
                    continue;
                }

                showPlayerAndCustomBoat(p, player);
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updatePlayerVisibility(player);
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.isCancelled()) {
            return;
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