package me.makkuusen.timing.system.listeners;

import me.makkuusen.timing.system.ReadyCheckManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class ReadyCheckListener implements Listener {

    @EventHandler
    public void onVehicleExitEvent(VehicleExitEvent e) {
        LivingEntity livingEntity = e.getExited();

        if (livingEntity instanceof Player player) {
            ReadyCheckManager.playerIsReady(player);
        }
    }

}
