package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.editor.TrackEditor;
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

    // Simple cache to avoid recomputing lobby stats too often
    private static final Map<UUID, StatsCache> statsCacheMap = new HashMap<>();

    public static void render(TPlayer tPlayer, TimeTrial timeTrial) {
        Player player = tPlayer.getPlayer();
        if (player == null) return;
        var theme = tPlayer.getTheme();
        Track track = timeTrial.getTrack();

        // Build Component Sidebar (title + lines) using the component builder API
        SidebarComponent titleComponent = SidebarComponent.dynamicLine(() ->
            Component.text("elytrarush.com")
                .color(theme.getPrimary())
                .decorate(TextDecoration.BOLD)
        );

        SidebarComponent.Builder linesBuilder = SidebarComponent.builder()
            // Current map
            .addStaticLine(Component.text(track.getDisplayName()).color(theme.getSecondary()))
            // PB and WR
            .addDynamicLine(() -> {
                long pb = timeTrial.getBestTime();
                var top = track.getTimeTrials().getTopList(1);
                long wr = top.isEmpty() ? -1 : top.get(0).getTime();
                return Component.text("PB: ").color(theme.getSecondary())
                    .append(Component.text(pb == -1 ? "(-)" : ApiUtilities.formatAsTime(pb)).color(theme.getSuccess()))
                    .append(Component.text(" WR: ").color(theme.getSecondary()))
                    .append(Component.text(wr == -1 ? "(-)" : ApiUtilities.formatAsTime(wr)).color(theme.getPrimary()));
            })
            // Player position
            .addDynamicLine(() -> {
                int pos = track.getTimeTrials().getPlayerTopListPosition(tPlayer);
                return pos > 0
                    ? Component.text("Position: ").color(theme.getSecondary()).append(Component.text("#" + pos).color(theme.getPrimary()))
                    : Component.text("Position: (-)").color(theme.getSecondary());
            })
            // Spacer
            .addBlankLine();

        //  Checkpoint list (best vs current)
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

    /**
     * Render the HUD when the player is not in a time trial.
     * Shows either lobby-wide stats or a summary of the last selected track.
     */
    public static void renderOverview(TPlayer tPlayer) {
        Player player = tPlayer.getPlayer();
        if (player == null) return;

        var theme = tPlayer.getTheme();

        // Determine which track to show (last played/selected), otherwise show lobby summary
        Track track = null;
        if (TimeTrialController.lastTimeTrialTrack.containsKey(tPlayer.getUniqueId())) {
            track = TimeTrialController.lastTimeTrialTrack.get(tPlayer.getUniqueId());
        } else if (TrackEditor.hasTrackSelected(tPlayer.getUniqueId())) {
            track = TrackEditor.getPlayerTrackSelection(tPlayer.getUniqueId());
        }

        SidebarComponent titleComponent = SidebarComponent.dynamicLine(() ->
            Component.text("elytrarush.com")
                .color(theme.getPrimary())
                .decorate(TextDecoration.BOLD)
        );

        SidebarComponent.Builder linesBuilder = SidebarComponent.builder();

        if (track == null) {
            // Lobby summary: completed maps and placement counts
            linesBuilder.addStaticLine(Component.text("Lobby").color(theme.getSecondary()));

            PlayerStats stats = getPlayerStatsCached(tPlayer);

            linesBuilder
                .addDynamicLine(() -> Component.text("Completed: ").color(theme.getSecondary())
                    .append(Component.text(stats.completed + "/" + stats.totalMaps).color(theme.getPrimary())))
                .addDynamicLine(() -> Component.text("1st: ").color(theme.getSecondary())
                    .append(Component.text(stats.firsts).color(theme.getPrimary())))
                .addDynamicLine(() -> Component.text("2nd: ").color(theme.getSecondary())
                    .append(Component.text(stats.seconds).color(theme.getPrimary())))
                .addDynamicLine(() -> Component.text("3rd: ").color(theme.getSecondary())
                    .append(Component.text(stats.thirds).color(theme.getPrimary())))
                .addDynamicLine(() -> Component.text("Top 10: ").color(theme.getSecondary())
                    .append(Component.text(stats.top10).color(theme.getPrimary())));
        } else {
            // Track summary: name, PB, WR, Position
            Track finalTrack = track;
            linesBuilder
                .addStaticLine(Component.text(finalTrack.getDisplayName()).color(theme.getSecondary()))
                .addDynamicLine(() -> {
                    long pb = Optional.ofNullable(finalTrack.getTimeTrials().getBestFinish(tPlayer))
                        .map(TimeTrialFinish::getTime).orElse(-1L);
                    var top = finalTrack.getTimeTrials().getTopList(1);
                    long wr = top.isEmpty() ? -1 : top.get(0).getTime();
                    return Component.text("PB: ").color(theme.getSecondary())
                        .append(Component.text(pb == -1 ? "(-)" : ApiUtilities.formatAsTime(pb)).color(theme.getSuccess()))
                        .append(Component.text(" WR: ").color(theme.getSecondary()))
                        .append(Component.text(wr == -1 ? "(-)" : ApiUtilities.formatAsTime(wr)).color(theme.getPrimary()));
                })
                .addDynamicLine(() -> {
                    int pos = finalTrack.getTimeTrials().getPlayerTopListPosition(tPlayer);
                    return pos > 0
                        ? Component.text("Position: ").color(theme.getSecondary()).append(Component.text("#" + pos).color(theme.getPrimary()))
                        : Component.text("Position: (-)").color(theme.getSecondary());
                });
        }

        ComponentSidebarLayout layout = new ComponentSidebarLayout(titleComponent, linesBuilder.build());

        Sidebar sidebar = sidebars.computeIfAbsent(player.getUniqueId(), uuid -> {
            Sidebar sb = TimingSystem.scoreboardLibrary.createSidebar(TimingSystem.configuration.getScoreboardMaxRows());
            sb.addPlayer(player);
            return sb;
        });

        try {
            sidebar.addPlayer(player);
        } catch (Exception ignore) {
        }

        layout.apply(sidebar);
    }

    private static PlayerStats getPlayerStatsCached(TPlayer tPlayer) {
        long now = System.currentTimeMillis();
        var cache = statsCacheMap.get(tPlayer.getUniqueId());
        if (cache != null && (now - cache.timestampMs) < 2000) {
            return cache.stats; // return cached within 2 seconds
        }
        PlayerStats computed = computePlayerStats(tPlayer);
        statsCacheMap.put(tPlayer.getUniqueId(), new StatsCache(now, computed));
        return computed;
    }

    private static PlayerStats computePlayerStats(TPlayer tPlayer) {
        int total = TrackDatabase.tracks.size();
        int completed = 0;
        int first = 0, second = 0, third = 0, top10 = 0;

        for (Track t : TrackDatabase.tracks) {
            if (t.getTimeTrials().getBestFinish(tPlayer) != null) {
                completed++;
            }
            Integer pos = t.getTimeTrials().getPlayerTopListPosition(tPlayer);
            if (pos != null && pos > 0) {
                if (pos == 1) first++;
                else if (pos == 2) second++;
                else if (pos == 3) third++;
                else if (pos <= 10) top10++;
            }
        }
        return new PlayerStats(total, completed, first, second, third, top10);
    }

    private record PlayerStats(int totalMaps, int completed, int firsts, int seconds, int thirds, int top10) {}
    private record StatsCache(long timestampMs, PlayerStats stats) {}

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
