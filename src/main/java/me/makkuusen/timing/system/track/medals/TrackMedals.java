package me.makkuusen.timing.system.track.medals;

import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.TimeTrials;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TrackMedals {
    private boolean isActive;
    private long netheriteCupTime; // top 3
    private long emeraldCupTime; // top 10
    private long diamondMedalTime; // top 5%
    private long goldMedalTime; // top 10%
    private long silverMedalTime; // top 25%
    private long copperMedalTime; // top 50%

    public TrackMedals() {
        isActive = false;
    }

    public void updateMedalsTimes(TimeTrials timeTrials) {
        if (TimingSystem.configuration.isMedalsAddOnEnabled()) {
            timeTrials.getTopList(1);
            int totalPositions = timeTrials.getCachedPositions().size();
            TimingSystem.getPlugin().logger.info(totalPositions + " total positions");
            if (totalPositions < 10) { isActive = false; return; }
            isActive = true;
            copperMedalTime = timeTrials.getBestFinish(timeTrials.getCachedPositions().get((int) (totalPositions * 0.9))).getTime();
            silverMedalTime = timeTrials.getBestFinish(timeTrials.getCachedPositions().get((int) (totalPositions * 0.75))).getTime();
            goldMedalTime = timeTrials.getBestFinish(timeTrials.getCachedPositions().get((int) (totalPositions * 0.5))).getTime();
            diamondMedalTime = timeTrials.getBestFinish(timeTrials.getCachedPositions().get((int) (totalPositions * 0.25))).getTime();
            emeraldCupTime = timeTrials.getBestFinish(timeTrials.getCachedPositions().get(1)).getTime();
            netheriteCupTime = timeTrials.getBestFinish(timeTrials.getCachedPositions().get(0)).getTime();
        }
    }

    public ItemStack getMedalItem(TPlayer tPlayer, String trackName, Long time) {
        if (!isActive) {
            ItemStack item = new ItemStack(Medals.INACTIVE.getMaterial(), 1);
            ItemMeta im = item.getItemMeta();
            im.displayName(Component.text(trackName).color(tPlayer.getTheme().getSecondary()));
            item.setItemMeta(im);
            return item;
        }
        Medals medal;
        if (time == null) {
            medal = Medals.NO_MEDAL;
        } else {
            medal = getMedal(time);
        }
        ItemStack item = new ItemStack(medal.getMaterial(), 1);
        ItemMeta im = item.getItemMeta();
        im.displayName(Component.text(trackName).color(tPlayer.getTheme().getSecondary()));
        im.lore(getMedalLore(time));
        im.setCustomModelData(medal.getCustomModelData());
        item.setItemMeta(im);
        return item;
    }

    public Component getMedalMessage(TimeTrials timeTrials, long prevTime, long time, String trackName) {
        updateMedalsTimes(timeTrials);
        Medals prevMedal = getMedal(prevTime);
        Medals medal = getMedal(time);
        if (medal.getNumber() > prevMedal.getNumber()) {
            Component hoverText = Component.join(JoinConfiguration.builder().separator(Component.text("\n")).build(), getMedalLore(time));
            return Component.text("\n§f=== " + medal.getColor() + "§lNew Time Trial Trophy§r§f ===\n\nYou unlocked " + medal.getColor() + "§l" + medal.getName() + "§r§f on " + trackName + "!").hoverEvent(HoverEvent.showText(hoverText));
        }
        return null;
    }

    public List<Component> getMedalLore(long time) {
        List<Component> lore = new ArrayList<>();
        String yourTime = ApiUtilities.formatAsMedalTime(time);
        if (time <= netheriteCupTime) lore.add(Component.text("§f> : " + yourTime + " - §lYOU"));
        String color = time <= netheriteCupTime ? "§a" : "§c";
        lore.add(Component.text("§f" + Medals.NETHERITE_CUP.getFont() + " : " + color + ApiUtilities.formatAsMedalTime(netheriteCupTime) + " §f(top 1)"));
        if (time > netheriteCupTime && time <= emeraldCupTime) lore.add(Component.text("§f> : " + yourTime + " - §lYOU"));
        color = time <= emeraldCupTime ? "§a" : "§c";
        lore.add(Component.text("§f" + Medals.NETHERITE_CUP.getFont() + " : " + color + ApiUtilities.formatAsMedalTime(emeraldCupTime) + " §f(top 2)"));
        if (time > emeraldCupTime && time <= diamondMedalTime) lore.add(Component.text("§f> : " + yourTime + " - §lYOU"));
        color = time <= diamondMedalTime ? "§a" : "§c";
        lore.add(Component.text("§f" + Medals.NETHERITE_CUP.getFont() + " : " + color + ApiUtilities.formatAsMedalTime(diamondMedalTime) + " §f(top 25%)"));
        if (time > diamondMedalTime && time <= goldMedalTime) lore.add(Component.text("§f> : " + yourTime + " - §lYOU"));
        color = time <= goldMedalTime ? "§a" : "§c";
        lore.add(Component.text("§f" + Medals.NETHERITE_CUP.getFont() + " : " + color + ApiUtilities.formatAsMedalTime(goldMedalTime) + " §f(top 50%)"));
        if (time > goldMedalTime && time <= silverMedalTime) lore.add(Component.text("§f> : " + yourTime + " - §lYOU"));
        color = time <= silverMedalTime ? "§a" : "§c";
        lore.add(Component.text("§f" + Medals.NETHERITE_CUP.getFont() + " : " + color + ApiUtilities.formatAsMedalTime(silverMedalTime) + " §f(top 75%)"));
        if (time > silverMedalTime && time <= copperMedalTime) lore.add(Component.text("§f> : " + yourTime + " - §lYOU"));
        color = time <= copperMedalTime ? "§a" : "§c";
        lore.add(Component.text("§f" + Medals.NETHERITE_CUP.getFont() + " : " + color + ApiUtilities.formatAsMedalTime(copperMedalTime) + " §f(top 99%)"));
        if (time > copperMedalTime) lore.add(Component.text("§f> : " + yourTime + " - §lYOU"));
        return lore;
    }

    private @NotNull Medals getMedal(long time) {
        if (time <= netheriteCupTime)      return Medals.NETHERITE_CUP;
        else if (time <= emeraldCupTime)   return Medals.EMERALD_CUP;
        else if (time <= diamondMedalTime) return Medals.DIAMOND_MEDAL;
        else if (time <= goldMedalTime)    return Medals.GOLD_MEDAL;
        else if (time <= silverMedalTime)  return Medals.SILVER_MEDAL;
        else if (time <= copperMedalTime)  return Medals.COPPER_MEDAL;
        else                              return Medals.NO_MEDAL;
    }
}
