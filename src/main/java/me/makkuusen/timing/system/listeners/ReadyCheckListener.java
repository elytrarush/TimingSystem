package me.makkuusen.timing.system.listeners;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.ReadyCheckManager;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.heat.ReadyCheck;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class ReadyCheckListener implements Listener {

    @EventHandler
    public void onVehicleExitEvent(VehicleExitEvent e) {
        LivingEntity livingEntity = e.getExited();

        if (livingEntity instanceof Player player) {
            ReadyCheckManager.playerIsReady(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        for (ReadyCheck readyCheck : ReadyCheckManager.getReadyChecks().values()) {
            if (e.getClickedInventory() != null && e.getClickedInventory().equals(readyCheck.getReadyInventory())) {
                e.setCancelled(true);
            }
        }
    }

}
