package me.makkuusen.timing.system.heat;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.theme.Theme;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class BossBarHud {
    private static final Map<Player, BossBar> bars = new HashMap<>();

    public static void showTimer(Player player, long currentTimeMs, long bestTimeMs, Theme theme, Component delta) {
        BossBar bar = bars.computeIfAbsent(player, p -> BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS));

        boolean isBetter = bestTimeMs == -1 || currentTimeMs < bestTimeMs;
        BossBar.Color color = isBetter ? BossBar.Color.GREEN : BossBar.Color.RED;
        String label = ApiUtilities.formatAsTime(currentTimeMs);
        Component name = Component.text(label);
        if (delta != null) {
            name = name.append(Component.text("  ")).append(delta);
        }

        bar.name(name);
        bar.color(color);
        // If bestTimeMs is known, show progress relative to best; otherwise keep at full.
        if (bestTimeMs > 0) {
            float progress = Math.max(0f, Math.min(1f, (float) currentTimeMs / (float) bestTimeMs));
            bar.overlay(BossBar.Overlay.PROGRESS);
            bar.progress(progress);
        } else {
            bar.progress(1f);
        }

        player.showBossBar(bar);
    }

    public static void hide(Player player) {
        BossBar bar = bars.remove(player);
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public static void showCountdown(Player player, long timeLeftMs, long totalMs, String label) {
        BossBar bar = bars.computeIfAbsent(player, p -> BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS));
        String prefix = label == null || label.isEmpty() ? "Time left" : label;
        Component name = Component.text(prefix + ": " + ApiUtilities.formatAsSeconds(timeLeftMs / 1000));
        bar.name(name);

        // progress shows remaining time proportion
        float progress = 1f;
        if (totalMs > 0) {
            progress = Math.max(0f, Math.min(1f, (float) timeLeftMs / (float) totalMs));
        }
        bar.progress(progress);

        BossBar.Color color = timeLeftMs <= 30_000 ? BossBar.Color.RED : BossBar.Color.GREEN;
        bar.color(color);

        player.showBossBar(bar);
    }
}
