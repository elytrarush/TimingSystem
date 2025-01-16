package me.makkuusen.timing.system.gui;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.events.GuiOpenEvent;
import me.makkuusen.timing.system.database.TSDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BaseGui {

    public final TimingSystem plugin;
    private final Inventory inventory;
    public final Component title;
    private final List<GuiButton> buttons = new ArrayList<>();

    public BaseGui(Component title, int rows) {
        if (TimingSystem.configuration.isCustomBoatsEnabled() && rows > 3) {
            String replacement = switch (rows) {
                case 4 -> TimingSystem.configuration.getGuiPrefix4();
                case 5 -> TimingSystem.configuration.getGuiPrefix5();
                case 6 -> TimingSystem.configuration.getGuiPrefix6();
                default -> TimingSystem.configuration.getGuiPrefix3();
            };
            title = title.replaceText(TextReplacementConfig.builder().matchLiteral(TimingSystem.configuration.getGuiPrefix3()).replacement(replacement).build());
        }

        this.inventory = Bukkit.createInventory(null, rows * 9, title);
        this.title = title;
        plugin = TimingSystem.getPlugin();
    }

    public void setItem(GuiButton button, int slot) {
        buttons.add(button);
        inventory.setItem(slot, button.getStack());
    }

    public void removeItem(int slot) {
        var item = inventory.getItem(slot);
        if (item == null) {
            return;
        }
        var maybeButton = buttons.stream().filter(b -> b.getStack().isSimilar(item)).findFirst();
        if (maybeButton.isEmpty()) {
            return;
        }
        buttons.remove(maybeButton.get());
        inventory.remove(item);
    }

    public boolean handleButton(ItemStack stack) {
        var maybeButton = buttons.stream().filter(b -> b.getStack().isSimilar(stack)).findFirst();
        if (maybeButton.isPresent()) {
            maybeButton.get().getAction().run();
            return true;
        }
        return false;
    }

    public boolean hasButton(GuiButton button) {
        return buttons.contains(button);
    }

    public void show(Player player) {
        var tPlayer = TSDatabase.getPlayer(player.getUniqueId());
        GuiOpenEvent guiOpenEvent = new GuiOpenEvent(player, this);
        Bukkit.getServer().getPluginManager().callEvent(guiOpenEvent);
        if (guiOpenEvent.isCancelled()) {
            return;
        }
        tPlayer.setOpenGui(this);
        player.openInventory(inventory);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean equalsInv(Inventory inv) {
        return inv.equals(inventory);
    }
}
