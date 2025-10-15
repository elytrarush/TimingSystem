package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.entity.Player;


import java.util.*;

/**
 * Sidebar HUD showing per-player leaderboard info while in a time trial, using scoreboard-library directly.
 */
public class LeaderboardHud {

    // Manage one Sidebar per player for the HUD
    private static final Map<UUID, Sidebar> sidebars = new HashMap<>();

    public static void render(TPlayer tPlayer, TimeTrial timeTrial) {
        Player player = tPlayer.getPlayer();
        if (player == null) return;
        var theme = tPlayer.getTheme();
        Track track = timeTrial.getTrack();

        // Build Component Sidebar (title + lines) using the component builder API
        SidebarComponent titleComponent = SidebarComponent.dynamicLine(() ->
            Component.text(track.getDisplayName())
                .color(theme.getPrimary())
                .decorate(TextDecoration.BOLD)
        );

        SidebarComponent.Builder linesBuilder = SidebarComponent.builder()
            // 1) Branding
            .addStaticLine(Component.text("elytrarush.com").color(theme.getPrimary()).decorate(TextDecoration.BOLD))
            // 2) Current map
            .addStaticLine(Component.text(track.getDisplayName()).color(theme.getSecondary()))
            // 3) PB and WR
            .addDynamicLine(() -> {
                long pb = timeTrial.getBestTime();
                var top = track.getTimeTrials().getTopList(1);
                long wr = top.isEmpty() ? -1 : top.get(0).getTime();
                return Component.text("PB: ").color(theme.getSecondary())
                    .append(Component.text(pb == -1 ? "(-)" : ApiUtilities.formatAsTime(pb)).color(theme.getSuccess()))
                    .append(Component.text(" WR: ").color(theme.getSecondary()))
                    .append(Component.text(wr == -1 ? "(-)" : ApiUtilities.formatAsTime(wr)).color(theme.getPrimary()));
            })
            // 4) Player position
            .addDynamicLine(() -> {
                int pos = track.getTimeTrials().getPlayerTopListPosition(tPlayer);
                return pos > 0
                    ? Component.text("Position: ").color(theme.getSecondary()).append(Component.text("#" + pos).color(theme.getPrimary()))
                    : Component.text("Position: (-)").color(theme.getSecondary());
            })
            // Spacer
            .addBlankLine();

        // 5) Checkpoint list (best vs current)
        int total = track.getNumberOfCheckpoints();
        for (int i = 1; i <= total; i++) {
            final int cpIndex = i;
            linesBuilder.addDynamicLine(() -> {
                boolean compareRecord = tPlayer.getSettings().isLeaderboardCompareRecord();
                var top = track.getTimeTrials().getTopList(1);
                TimeTrialFinish compareFinish = compareRecord ? (top.isEmpty() ? null : top.get(0)) : timeTrial.getBestFinish();

                int latest = timeTrial.getLatestCheckpoint();
                Long currentSplit = cpIndex <= latest ? timeTrial.getCheckpointTime(cpIndex) : null;
                Long compareSplit = (compareFinish != null && compareFinish.hasCheckpointTimes()) ? compareFinish.getCheckpointTime(cpIndex) : null;

                return Component.text("CP " + cpIndex + ": ").color(theme.getSecondary())
                    .append(Component.text(compareSplit == null ? "-" : ApiUtilities.formatAsTime(compareSplit)).color(theme.getPrimary()))
                    .append(Component.text(" | ").color(theme.getSecondary()))
                    .append(Component.text(currentSplit == null ? "-" : ApiUtilities.formatAsTime(currentSplit)).color(currentSplit != null ? theme.getSuccess() : theme.getSecondary()));
            });
        }

        ComponentSidebarLayout layout = new ComponentSidebarLayout(titleComponent, linesBuilder.build());

        // Reuse or create a per-player sidebar and ensure the player is added as a viewer
        Sidebar sidebar = sidebars.computeIfAbsent(player.getUniqueId(), uuid -> {
            Sidebar sb = TimingSystem.scoreboardLibrary.createSidebar(TimingSystem.configuration.getScoreboardMaxRows());
            sb.addPlayer(player);
            return sb;
        });

        // If the sidebar exists but the player isn't attached (e.g., reconnected), attach again
        try {
            sidebar.addPlayer(player);
        } catch (Exception ignore) {
            // addPlayer may throw if already added; safe to ignore
        }

        // Apply/refresh layout; dynamic lines will be updated by the library
        layout.apply(sidebar);
    }

    public static void hide(UUID uuid) {
        Sidebar sidebar = sidebars.remove(uuid);
        if (sidebar != null) {
            try {
                sidebar.close();
            } catch (Exception ignored) {
            }
        }
    }
}
