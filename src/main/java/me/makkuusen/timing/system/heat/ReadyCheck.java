package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ReadyCheckManager;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.sounds.PlaySound;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

public class ReadyCheck {

    private Heat heat;
    private UUID playerWhoInitiated;
    private Inventory readyInventory;
    private ArrayList<UUID> toReadyCheckPlayers = new ArrayList<UUID>();
    private int taskId;

    public ReadyCheck(Player p, Heat heat) {
        this.heat = heat;
        this.playerWhoInitiated = p.getUniqueId();
        this.readyInventory = Bukkit.createInventory(null, 54, Component.text("Ready Check", NamedTextColor.GOLD));
        for (Driver driver : heat.getDrivers().values()) {
            toReadyCheckPlayers.add(driver.getTPlayer().getUniqueId());
        }
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(TimingSystem.getPlugin(), new Runnable() {
            @Override
            public void run() {
                Component readyText = Component.text("Are you ready?", NamedTextColor.GOLD);
                Component pressText = Component.text("Press ", NamedTextColor.YELLOW)
                        .append(Component.keybind("key.sneak", NamedTextColor.RED))
                        .append(Component.text(" to ready up!", NamedTextColor.YELLOW));

                Title title = Title.title(readyText, pressText,
                        Title.Times.times(Duration.ofMillis(100L), Duration.ofSeconds(15L), Duration.ofMillis(100L)));
                for (UUID uuid : toReadyCheckPlayers) {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                    if (off.isOnline()) {
                        off.getPlayer().showTitle(title);
                        off.getPlayer().sendMessage(readyText);
                        off.getPlayer().sendMessage(pressText);
                        PlaySound.countDownPling(TimingSystemAPI.getTPlayer(off.getUniqueId()));
                    }
                }
            }
        }, 1, 15*20);
    }

    public void openGUIToInitiator() {
        OfflinePlayer off = Bukkit.getOfflinePlayer(playerWhoInitiated);
        if (off.isOnline()) {
            off.getPlayer().openInventory(readyInventory);
        }
    }

    public void playerIsReady(Player readyPlayer) {
        if (!toReadyCheckPlayers.contains(readyPlayer.getUniqueId())) {
            return;
        }

        toReadyCheckPlayers.remove(readyPlayer.getUniqueId());
        updateInventory();
        readyPlayer.clearTitle();

        if (toReadyCheckPlayers.isEmpty()) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(playerWhoInitiated);
            if (off.isOnline()) {
                off.getPlayer().sendMessage(Component.text("All heat drivers are ready!", NamedTextColor.GREEN));
                end();
            }
        }
    }

    private void updateInventory() {
        readyInventory.clear();
        for (UUID uuid : toReadyCheckPlayers) {
            readyInventory.addItem(getGUIItem(uuid));
        }
    }

    /**
     *
     * @param playerUuid
     * @return A player head itemstack or null if it cannot be created.
     */
    private ItemStack getGUIItem(UUID playerUuid) {
        ItemStack is = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) is.getItemMeta();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        if (offlinePlayer.isOnline() || offlinePlayer.hasPlayedBefore()) {
            sm.setOwningPlayer(offlinePlayer);
        } else {
            return null;
        }

        sm.displayName(Component.text(offlinePlayer.getName() + " is not ready", NamedTextColor.RED));

        is.setItemMeta(sm);
        return is;
    }

    public void end() {
        Bukkit.getScheduler().cancelTask(taskId);
        OfflinePlayer off = Bukkit.getOfflinePlayer(playerWhoInitiated);
        if (!off.isOnline()) {
            return;
        }

        for (UUID uuid : toReadyCheckPlayers) {
            OfflinePlayer toCheckPlayer = Bukkit.getOfflinePlayer(uuid);
            off.getPlayer().sendMessage(Component.text(toCheckPlayer.getName() + " is not ready.", NamedTextColor.RED));
        }

        off.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        ReadyCheckManager.remove(playerWhoInitiated);
        Bukkit.getScheduler().cancelTask(taskId);
    }

}
