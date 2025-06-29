package me.makkuusen.timing.system.track.medals;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public enum Medals {
    NETHERITE_CUP("Netherite Cup", "§8", " ", Material.NETHERITE_INGOT, 1, 6, 1),
    EMERALD_CUP("Emerald Cup","§a"," ",  Material.EMERALD, 1, 5, 0.75),
    DIAMOND_MEDAL("Diamond Medal","§b", " ",  Material.DIAMOND, 1, 4, 0.5),
    GOLD_MEDAL("Gold Medal","§e", " ", Material.GOLD_INGOT, 1, 3, 0.25),
    SILVER_MEDAL("Silver Medal","§7", " ", Material.IRON_INGOT, 1, 2, 0.15),
    COPPER_MEDAL("Copper Medal","§6", " ", Material.COPPER_INGOT, 1, 1, 0.05),
    NO_MEDAL("No Medal","§f", " ", Material.WHITE_DYE, 11, 0, 0),
    INACTIVE("No Medals on this track","§7", " ", Material.BARRIER, 0, -1, 0);

    private final String name;
    private final String color;
    private final String font;
    private final Material material;
    private final int customModelData;
    private final int number;
    private final double xpBonus;

    Medals(String name, String color, String font, Material material, int customModelData, int number, double xpBonus) {
        this.name = name;
        this.color = color;
        this.font = font;
        this.material = material;
        this.customModelData = customModelData;
        this.number = number;
        this.xpBonus = xpBonus;
    }
}
