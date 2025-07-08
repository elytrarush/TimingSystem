package me.makkuusen.timing.system.track.medals;

import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum Medals {
    NETHERITE_CUP("Netherite Cup", "§8", "\uE070", Material.NETHERITE_INGOT, 1, 6, 1),
    EMERALD_CUP("Emerald Cup","§a","\uE071",  Material.EMERALD, 1, 5, 0.75),
    DIAMOND_MEDAL("Diamond Medal","§b", "\uE072",  Material.DIAMOND, 1, 4, 0.5),
    GOLD_MEDAL("Gold Medal","§e", "\uE073", Material.GOLD_INGOT, 1, 3, 0.25),
    SILVER_MEDAL("Silver Medal","§7", "\uE074", Material.IRON_INGOT, 1, 2, 0.15),
    COPPER_MEDAL("Copper Medal","§6", "\uE075", Material.COPPER_INGOT, 1, 1, 0.05),
    NO_MEDAL("No Medal","§f", "\uE076", Material.WHITE_DYE, 11, 0, 0),
    INACTIVE("No Medals on this track","§7", "", Material.STRUCTURE_VOID, 0, -1, 0);

    private final String name;
    private final String color;
    private final String font;
    private final Material material;
    private final int customModelData;
    private final int number;
    private final double xpBonus;

    private static final Map<Integer, Medals> BY_NUMBER = Arrays.stream(values()).collect(Collectors.toMap(Medals::getNumber, Function.identity()));

    Medals(String name, String color, String font, Material material, int customModelData, int number, double xpBonus) {
        this.name = name;
        this.color = color;
        this.font = font;
        this.material = material;
        this.customModelData = customModelData;
        this.number = number;
        this.xpBonus = xpBonus;
    }

    public static Medals fromNumber(int number) {
        return BY_NUMBER.getOrDefault(number, NO_MEDAL);
    }
}
