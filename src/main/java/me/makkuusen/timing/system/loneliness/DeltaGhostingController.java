package me.makkuusen.timing.system.loneliness;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.participant.Driver;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DeltaGhostingController {
    private static final HashMap<Driver, Set<Driver>> deltaGhostedPlayers = new HashMap<>();

    public static void checkDeltas(Driver driver) {
        Heat heat = driver.getHeat();
        int ghostingDelta = heat.getGhostingDelta();

        for (Driver otherDriver : heat.getLivePositions()) {
            if (otherDriver.equals(driver)) {
                continue;
            }

            if (otherDriver.getPosition() < driver.getPosition()) {
                long delta = driver.getTimeGap(otherDriver);
                if (Math.abs(delta) >= ghostingDelta) {
                    deltaGhost(otherDriver, driver);
                } else {
                    unDeltaGhost(otherDriver, driver);
                }
            } else {
                unDeltaGhost(otherDriver, driver);
            }
        }
    }

    private static void deltaGhost(Driver leadingDriver, Driver trailingDriver) {
        Heat heat = leadingDriver.getHeat();
        if (heat.getLonely() || LonelinessController.isGhosted(trailingDriver.getTPlayer().getUniqueId())) {
            return;
        }

        deltaGhostedPlayers.computeIfAbsent(leadingDriver, k -> new HashSet<>()).add(trailingDriver);

        Player leadingPlayer = leadingDriver.getTPlayer().getPlayer();
        Player trailingPlayer = trailingDriver.getTPlayer().getPlayer();
        Vehicle vehicle = (Vehicle) trailingPlayer.getVehicle();

        if (vehicle instanceof Boat) {
            leadingPlayer.hideEntity(TimingSystem.getPlugin(), vehicle);
        }

        leadingPlayer.hideEntity(TimingSystem.getPlugin(), trailingPlayer);
    }

    private static void unDeltaGhost(Driver leadingDriver, Driver trailingDriver) {
        Heat heat = leadingDriver.getHeat();
        if (heat.getLonely() || LonelinessController.isGhosted(trailingDriver.getTPlayer().getUniqueId())) {
            return;
        }

        Set<Driver> ghosted = deltaGhostedPlayers.get(leadingDriver);
        if (ghosted != null) {
            ghosted.remove(trailingDriver);
            if (ghosted.isEmpty()) {
                deltaGhostedPlayers.remove(leadingDriver);
            }
        }

        Player leadingPlayer = leadingDriver.getTPlayer().getPlayer();
        Player trailingPlayer = trailingDriver.getTPlayer().getPlayer();
        Vehicle vehicle = (Vehicle) trailingPlayer.getVehicle();

        if (vehicle instanceof Boat) {
            leadingPlayer.showEntity(TimingSystem.getPlugin(), vehicle);
        }

        leadingPlayer.showEntity(TimingSystem.getPlugin(), trailingPlayer);
    }

    public static boolean isDeltaGhosted(Driver leadingDriver, Driver trailingDriver) {
        if (leadingDriver.getHeat().getLonely()) {
            return false;
        }

        Set<Driver> ghosted = deltaGhostedPlayers.get(leadingDriver);
        return ghosted != null && ghosted.contains(trailingDriver);
    }

    public static void clearDeltaGhosts(Heat heat) {
        deltaGhostedPlayers.keySet().removeIf(driver -> driver.getHeat().equals(heat));
    }

    public static void removeHeatDriver(Driver driver) {
        deltaGhostedPlayers.remove(driver);
        deltaGhostedPlayers.values().forEach(set -> set.remove(driver));
    }
}