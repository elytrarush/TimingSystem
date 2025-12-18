package me.makkuusen.timing.system.leaderboard;

import me.makkuusen.timing.system.CheaterManager;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes and caches "global points" leaderboard used for global ranks.
 *
 * Points model matches the FancyHolograms global leaderboard:
 * for each track, players earn $1000 / sqrt(position)$ points.
 */
public final class GlobalPointsLeaderboard {

    private static volatile Map<UUID, Integer> RANK_BY_UUID = Map.of();
    private static volatile Map<UUID, Long> POINTS_BY_UUID = Map.of();

    private static boolean updateTaskStarted = false;

    private GlobalPointsLeaderboard() {}

    public static void startUpdateTask() {
        if (updateTaskStarted) return;
        updateTaskStarted = true;

        long period = Math.max(20L, TimingSystem.configuration.getLeaderboardsUpdateTick());
        Bukkit.getScheduler().runTaskTimer(TimingSystem.getPlugin(), GlobalPointsLeaderboard::refreshCache, 40L, period);
    }

    public static void refreshCache() {
        Map<TPlayer, Double> points = computePoints();

        List<Map.Entry<TPlayer, Double>> sorted = points.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Double.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    String an = a.getKey() == null ? "" : a.getKey().getName();
                    String bn = b.getKey() == null ? "" : b.getKey().getName();
                    return an.compareToIgnoreCase(bn);
                })
                .collect(Collectors.toList());

        Map<UUID, Integer> newRanks = new HashMap<>(sorted.size());
        Map<UUID, Long> newPoints = new HashMap<>(sorted.size());

        int rank = 1;
        for (Map.Entry<TPlayer, Double> e : sorted) {
            TPlayer tp = e.getKey();
            if (tp == null) continue;
            UUID uuid = tp.getUniqueId();
            if (uuid == null) continue;

            newRanks.put(uuid, rank);
            newPoints.put(uuid, Math.round(e.getValue()));
            rank++;
        }

        RANK_BY_UUID = Collections.unmodifiableMap(newRanks);
        POINTS_BY_UUID = Collections.unmodifiableMap(newPoints);
    }

    public static Integer getRank(UUID uuid) {
        if (uuid == null) return null;
        return RANK_BY_UUID.get(uuid);
    }

    public static Long getPoints(UUID uuid) {
        if (uuid == null) return null;
        return POINTS_BY_UUID.get(uuid);
    }

    public static List<Map.Entry<TPlayer, Double>> computeTop(int limit) {
        if (limit < 1) return List.of();
        Map<TPlayer, Double> points = computePoints();
        return points.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private static Map<TPlayer, Double> computePoints() {
        Map<TPlayer, Double> points = new HashMap<>();

        List<Track> tracks = new ArrayList<>(TrackDatabase.tracks);
        List<TPlayer> players = new ArrayList<>(TimingSystem.players.values());

        for (Track track : tracks) {
            // Ensure cached toplists are present
            track.getTimeTrials().getTopList(-1);

            for (TPlayer p : players) {
                if (p == null) continue;
                UUID uuid = p.getUniqueId();
                if (uuid == null) continue;

                if (CheaterManager.isCheater(uuid)) {
                    continue;
                }
                Integer pos = track.getTimeTrials().getPlayerTopListPosition(p);
                if (pos != null && pos > 0) {
                    double add = 1000.0 / Math.pow(pos.doubleValue(), 0.5);
                    points.merge(p, add, Double::sum);
                }
            }
        }

        return points;
    }
}
