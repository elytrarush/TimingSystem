package me.makkuusen.timing.system.timetrial;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.editor.TrackEditor;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextColor;
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

    // Shimmer colors for the title
    private static final TextColor SHIMMER_BASE = TextColor.color(0xFFCC00); // #ffcc00
    private static final TextColor SHIMMER_HIGHLIGHT = TextColor.color(0xFF7300); // #ff7300

    // Medal colors for lobby leaderboard
    private static final TextColor GOLD = TextColor.color(0xFFD700);
    private static final TextColor SILVER = TextColor.color(0xC0C0C0);
    private static final TextColor BRONZE = TextColor.color(0xCD7F32);

    public static void render(TPlayer tPlayer, TimeTrial timeTrial) {
        Player player = tPlayer.getPlayer();
        if (player == null) return;
        var theme = tPlayer.getTheme();
        Track track = timeTrial.getTrack();
        int totalCheckpoints = track.getNumberOfCheckpoints();
        int cpDigits = Math.max(1, String.valueOf(totalCheckpoints).length());

        // Build Component Sidebar (title + lines) using the component builder API
        SidebarComponent titleComponent = SidebarComponent.dynamicLine(() -> shimmerTitle("elytrarush.com"));

        SidebarComponent.Builder linesBuilder = SidebarComponent.builder()
            // Current map
            .addStaticLine(
                Component.text("Track: ").color(theme.getSecondary())
                    .append(Component.text(track.getDisplayName()).color(theme.getSecondary()).decorate(TextDecoration.BOLD))
            )
            // PB and WR
            .addDynamicLine(() -> {
                long pb = timeTrial.getBestTime();
                var top = track.getTimeTrials().getTopList(1);
                long wr = top.isEmpty() ? -1 : top.get(0).getTime();
                return Component.text("PB: ").color(theme.getSecondary())
                    .append(Component.text(pb == -1 ? "(-)" : ApiUtilities.formatAsTime(pb)).color(theme.getSuccess()).decorate(TextDecoration.BOLD))
                    .append(Component.text("  WR: ").color(theme.getSecondary()))
                    .append(Component.text(wr == -1 ? "(-)" : ApiUtilities.formatAsTime(wr)).color(theme.getSecondary()).decorate(TextDecoration.BOLD));
            })
            // Player position
            .addDynamicLine(() -> {
                int pos = track.getTimeTrials().getPlayerTopListPosition(tPlayer);
                return pos > 0
                    ? Component.text("Position: ").color(theme.getSecondary()).append(Component.text("#" + pos).color(theme.getSecondary()).decorate(TextDecoration.BOLD))
                    : Component.text("Position: (-)").color(theme.getSecondary());
            })
            // Spacer
            .addBlankLine()
            // Header for checkpoint table (indicates columns)
            .addDynamicLine(() -> {
                boolean compareRecord = tPlayer.getSettings().isLeaderboardCompareRecord();
                String ref = compareRecord ? "WR" : "PB";

                // Build header with alignment matching rows: "CP <spaces>: <REF> | CUR"
                Component cpHeader = Component.text("CP " + repeat(' ', cpDigits) + ": ").color(theme.getSecondary());
                // compute column width based on max formatted split length for the reference finish (PB/WR)
                TimeTrialFinish compareFinishForWidth;
                var topForWidth = track.getTimeTrials().getTopList(1);
                compareFinishForWidth = compareRecord ? (topForWidth.isEmpty() ? null : topForWidth.get(0)) : timeTrial.getBestFinish();
                int refColWidth = Math.max(ref.length(), getMaxFormattedSplitLength(compareFinishForWidth, totalCheckpoints));

                String refLabelPadded = padRight(ref, refColWidth);
                Component refHeader = Component.text(refLabelPadded).color(theme.getSecondary());
                Component sep = Component.text(" | ").color(theme.getSecondary());
                Component curHeader = Component.text("CUR").color(theme.getSecondary());
                return cpHeader.append(refHeader).append(sep).append(curHeader);
            });

        //  Checkpoint list (best vs current)
        int total = totalCheckpoints;
        for (int i = 1; i <= total; i++) {
            final int cpIndex = i;
            linesBuilder.addDynamicLine(() -> {
                boolean compareRecord = tPlayer.getSettings().isLeaderboardCompareRecord();
                var top = track.getTimeTrials().getTopList(1);
                TimeTrialFinish compareFinish = compareRecord ? (top.isEmpty() ? null : top.get(0)) : timeTrial.getBestFinish();

                int latest = timeTrial.getLatestCheckpoint();
                Long currentSplit = cpIndex <= latest ? timeTrial.getCheckpointTime(cpIndex) : null;
                Long compareSplit = (compareFinish != null && compareFinish.hasCheckpointTimes()) ? compareFinish.getCheckpointTime(cpIndex) : null;

                // Determine color of current split based on comparison with reference
                var currentComp = Component.text(currentSplit == null ? "-" : ApiUtilities.formatAsTime(currentSplit));
                if (currentSplit != null && compareSplit != null) {
                    long a = ApiUtilities.getRoundedToTick(compareSplit);
                    long b = ApiUtilities.getRoundedToTick(currentSplit);
                    if (b > a) {
                        currentComp = currentComp.color(theme.getError()); // slower
                    } else if (b == a) {
                        currentComp = currentComp.color(theme.getWarning()); // equal
                    } else {
                        currentComp = currentComp.color(theme.getSuccess()); // faster
                    }
                } else if (currentSplit != null) {
                    currentComp = currentComp.color(theme.getSecondary());
                } else {
                    currentComp = currentComp.color(theme.getSecondary());
                }

                // compute column width to align the separator with header
                int refColWidth = Math.max((compareRecord ? "WR" : "PB").length(), getMaxFormattedSplitLength(compareFinish, total));

                String compareStr = (compareSplit == null ? "-" : ApiUtilities.formatAsTime(compareSplit));
                String compareStrPadded = padRight(compareStr, refColWidth);

                String cpLabel = "CP " + padLeft(Integer.toString(cpIndex), cpDigits) + ": ";
                return Component.text(cpLabel).color(theme.getSecondary())
                    .append(Component.text(compareStrPadded).color(theme.getSecondary()))
                    .append(Component.text(" | ").color(theme.getSecondary()))
                    .append(currentComp);
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

        SidebarComponent titleComponent = SidebarComponent.dynamicLine(() -> shimmerTitle("elytrarush.com"));

        SidebarComponent.Builder linesBuilder = SidebarComponent.builder();

        if (player.getLocation().getWorld().getName().equals("lobby")) {
            // Lobby summary: completed maps and placement counts
            linesBuilder.addStaticLine(Component.text("Lobby").color(theme.getSecondary()).decorate(TextDecoration.BOLD));

            PlayerStats stats = getPlayerStatsCached(tPlayer);

            linesBuilder
                .addDynamicLine(() -> Component.text("Completed: ").color(theme.getSecondary())
                    .append(Component.text(stats.completed + "/" + stats.totalMaps).color(theme.getSecondary())))
                .addDynamicLine(() -> Component.text("1st: ").color(theme.getSecondary())
                    .append(Component.text(stats.firsts).color(GOLD).decorate(TextDecoration.BOLD)))
                .addDynamicLine(() -> Component.text("2nd: ").color(theme.getSecondary())
                    .append(Component.text(stats.seconds).color(SILVER).decorate(TextDecoration.BOLD)))
                .addDynamicLine(() -> Component.text("3rd: ").color(theme.getSecondary())
                    .append(Component.text(stats.thirds).color(BRONZE).decorate(TextDecoration.BOLD)))
                .addDynamicLine(() -> Component.text("Top 10: ").color(theme.getSecondary())
                    .append(Component.text(stats.top10).color(theme.getSecondary())));
        } else {
            // Track summary: name, PB, WR, Position
            Track finalTrack = track;
            int totalCheckpoints2 = finalTrack.getNumberOfCheckpoints();
            int cpDigits2 = Math.max(1, String.valueOf(totalCheckpoints2).length());
            linesBuilder
                .addStaticLine(
                    Component.text("Current Track: ").color(theme.getSecondary())
                        .append(Component.text(finalTrack.getDisplayName()).color(theme.getSecondary()).decorate(TextDecoration.BOLD))
                )
                .addDynamicLine(() -> {
                    long pb = Optional.ofNullable(finalTrack.getTimeTrials().getBestFinish(tPlayer))
                        .map(TimeTrialFinish::getTime).orElse(-1L);
                    var top = finalTrack.getTimeTrials().getTopList(1);
                    long wr = top.isEmpty() ? -1 : top.get(0).getTime();
                    return Component.text("PB: ").color(theme.getSecondary())
                        .append(Component.text(pb == -1 ? "(-)" : ApiUtilities.formatAsTime(pb)).color(theme.getSuccess()).decorate(TextDecoration.BOLD))
                        .append(Component.text("  WR: ").color(theme.getSecondary()))
                        .append(Component.text(wr == -1 ? "(-)" : ApiUtilities.formatAsTime(wr)).color(theme.getSecondary()).decorate(TextDecoration.BOLD));
                })
                .addDynamicLine(() -> {
                    int pos = finalTrack.getTimeTrials().getPlayerTopListPosition(tPlayer);
                    return pos > 0
                        ? Component.text("Position: ").color(theme.getSecondary()).append(Component.text("#" + pos).color(theme.getSecondary()).decorate(TextDecoration.BOLD))
                        : Component.text("Position: (-)").color(theme.getSecondary());
                })
                // Spacer and header for PB/WR checkpoint table
                .addBlankLine()
                .addDynamicLine(() -> {
                    Component cpHeader = Component.text("CP " + repeat(' ', cpDigits2) + ": ").color(theme.getSecondary());
                    // compute PB column width to align header and rows
                    TimeTrialFinish pbFinishForWidth = finalTrack.getTimeTrials().getBestFinish(tPlayer);
                    int pbColWidth = Math.max("PB".length(), getMaxFormattedSplitLength(pbFinishForWidth, totalCheckpoints2));
                    Component pbHeader = Component.text(padRight("PB", pbColWidth)).color(theme.getSuccess());
                    Component sep = Component.text(" | ").color(theme.getSecondary());
                    Component wrHeader = Component.text("WR").color(theme.getSecondary());
                    return cpHeader.append(pbHeader).append(sep).append(wrHeader);
                });

            // PB/WR checkpoint splits
            int total = totalCheckpoints2;
            for (int i = 1; i <= total; i++) {
                final int cpIndex = i;
                linesBuilder.addDynamicLine(() -> {
                    TimeTrialFinish pbFinish = finalTrack.getTimeTrials().getBestFinish(tPlayer);
                    var top = finalTrack.getTimeTrials().getTopList(1);
                    TimeTrialFinish wrFinish = top.isEmpty() ? null : top.get(0);

                    Long pbSplit = (pbFinish != null && pbFinish.hasCheckpointTimes()) ? pbFinish.getCheckpointTime(cpIndex) : null;
                    Long wrSplit = (wrFinish != null && wrFinish.hasCheckpointTimes()) ? wrFinish.getCheckpointTime(cpIndex) : null;

                    // compute PB column width to align separator with header
                    TimeTrialFinish pbFinishForWidth2 = finalTrack.getTimeTrials().getBestFinish(tPlayer);
                    int pbColWidth2 = Math.max("PB".length(), getMaxFormattedSplitLength(pbFinishForWidth2, total));

                    String pbStr = (pbSplit == null ? "-" : ApiUtilities.formatAsTime(pbSplit));
                    String pbStrPadded = padRight(pbStr, pbColWidth2);

                    String cpLabel = "CP " + padLeft(Integer.toString(cpIndex), cpDigits2) + ": ";
                    return Component.text(cpLabel).color(theme.getSecondary())
                        .append(Component.text(pbStrPadded).color(theme.getSuccess()))
                        .append(Component.text(" | ").color(theme.getSecondary()))
                        .append(Component.text(wrSplit == null ? "-" : ApiUtilities.formatAsTime(wrSplit)).color(theme.getSecondary()));
                });
            }
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

    /**
     * Builds an animated shimmer title component using a moving highlight
     * that blends between SHIMMER_BASE and SHIMMER_HIGHLIGHT.
     */
    private static Component shimmerTitle(String text) {
        // Animation settings
        long now = System.currentTimeMillis();
        int len = text.length();
        if (len == 0) {
            return Component.empty();
        }

        // One full sweep across the text in this many milliseconds
        final double periodMs = 2000.0;
        // Width of the highlight in characters (triangular falloff)
        final double spread = 2.5;

        // Compute center position moving over time
        double phase = (now % (long) periodMs) / periodMs; // [0,1)
        double center = phase * (len + spread) - (spread * 0.5);

        Component result = Component.empty();
        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            // Triangular falloff around the moving center
            double dist = Math.abs(i - center);
            double t = Math.max(0.0, 1.0 - (dist / spread)); // [0,1]
            TextColor c = lerpColor(SHIMMER_BASE, SHIMMER_HIGHLIGHT, t);
            result = result.append(Component.text(String.valueOf(ch)).color(c));
        }
        return result.decorate(TextDecoration.BOLD);
    }

    private static TextColor lerpColor(TextColor a, TextColor b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(a.red() + (b.red() - a.red()) * t);
        int g = (int) Math.round(a.green() + (b.green() - a.green()) * t);
        int bl = (int) Math.round(a.blue() + (b.blue() - a.blue()) * t);
        return TextColor.color(r, g, bl);
    }

    private static String padLeft(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        return " ".repeat(width - s.length()) + s;
    }

    private static String repeat(char c, int n) {
        if (n <= 0) return "";
        char[] arr = new char[n];
        Arrays.fill(arr, c);
        return new String(arr);
    }

    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }

    private static int getMaxFormattedSplitLength(TimeTrialFinish finish, int totalCheckpoints) {
        int maxLen = 1; // at least length of "-"
        if (finish != null && finish.hasCheckpointTimes()) {
            for (int i = 1; i <= totalCheckpoints; i++) {
                Long split = finish.getCheckpointTime(i);
                String s = (split == null) ? "-" : ApiUtilities.formatAsTime(split);
                if (s.length() > maxLen) {
                    maxLen = s.length();
                }
            }
        }
        return maxLen;
    }
}
