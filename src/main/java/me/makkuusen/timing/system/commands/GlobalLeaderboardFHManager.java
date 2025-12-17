package me.makkuusen.timing.system.commands;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.HologramData;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import me.makkuusen.timing.system.CheaterManager;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.database.TrackDatabase;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages global leaderboard holograms created via FancyHolograms.
 * Only referenced and used when FancyHolograms plugin is present to avoid NoClassDefFoundError.
 */
public final class GlobalLeaderboardFHManager {
    private static final Map<String, String> NAME_TO_TITLE = new ConcurrentHashMap<>();
    private static boolean updateTaskStarted = false;
    private static final String FILE_NAME = "global-leaderboards.yml";

    private GlobalLeaderboardFHManager() {}

    public static void register(String name, String title) {
        NAME_TO_TITLE.put(name, title == null || title.isBlank() ? "Global Leaderboard" : title);
        savePersisted();
    }

    public static void unregister(String name) {
        NAME_TO_TITLE.remove(name);
        savePersisted();
    }

    public static void startUpdateTask() {
        if (updateTaskStarted) return;
        updateTaskStarted = true;

        long period = Math.max(20L, TimingSystem.configuration.getLeaderboardsUpdateTick());
        Bukkit.getScheduler().runTaskTimer(TimingSystem.getPlugin(), GlobalLeaderboardFHManager::updateAll, 60L * 20L, period);
    }

    public static void loadPersisted() {
        File dataFolder = TimingSystem.getPlugin().getDataFolder();
        if (!dataFolder.exists()) {
            // Nothing to load
            return;
        }
        File file = new File(dataFolder, FILE_NAME);
        if (!file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> entries = cfg.getMapList("entries");
        NAME_TO_TITLE.clear();
        for (Map<?, ?> entry : entries) {
            Object n = entry.get("name");
            Object t = entry.get("title");
            if (n instanceof String sName) {
                NAME_TO_TITLE.put(sName, t instanceof String sTitle ? sTitle : "Global Leaderboard");
            }
        }
    }

    private static void savePersisted() {
        try {
            File dataFolder = TimingSystem.getPlugin().getDataFolder();
            if (!dataFolder.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dataFolder.mkdirs();
            }
            File file = new File(dataFolder, FILE_NAME);
            YamlConfiguration cfg = new YamlConfiguration();
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map.Entry<String, String> e : NAME_TO_TITLE.entrySet()) {
                Map<String, Object> m = new HashMap<>();
                m.put("name", e.getKey());
                m.put("title", e.getValue());
                list.add(m);
            }
            cfg.set("entries", list);
            cfg.save(file);
        } catch (IOException ignored) {}
    }

    public static void updateAll() {
        if (NAME_TO_TITLE.isEmpty()) return;

        var hm = FancyHologramsPlugin.get().getHologramManager();
        for (Map.Entry<String, String> entry : NAME_TO_TITLE.entrySet()) {
            String name = entry.getKey();
            String title = entry.getValue();

            Optional<Hologram> hologramOpt = hm.getHologram(name);
            if (hologramOpt.isEmpty()) continue;

            Hologram hologram = hologramOpt.get();
            HologramData data = hologram.getData();
            if (!(data instanceof TextHologramData textData)) continue;

            List<Map.Entry<TPlayer, Double>> top = computeGlobalPointsTop(10);
            List<String> lines = new ArrayList<>();
            lines.add("§6§l" + (title == null || title.isBlank() ? "Global Leaderboard" : title));
            lines.add(" ");
            int i = 1;
            for (Map.Entry<TPlayer, Double> e : top) {
                TPlayer tp = e.getKey();
                double pts = e.getValue();
                lines.add(String.format("§6%2d. §f%s  §6%s", i, tp.getName(), formatPoints(pts)));
                i++;
            }

            textData.setText(lines);
            // Apply update
            try {
                hologram.forceUpdate();
            } catch (Throwable ignored) {}
            try {
                hologram.queueUpdate();
            } catch (Throwable ignored) {}
        }
    }

    public static List<Map.Entry<TPlayer, Double>> computeGlobalPointsTop(int limit) {
        Map<TPlayer, Double> points = new HashMap<>();
        List<Track> tracks = new ArrayList<>(TrackDatabase.tracks);
        for (Track track : tracks) {
            track.getTimeTrials().getTopList(-1);
            for (TPlayer p : TimingSystem.players.values()) {
                if (CheaterManager.isCheater(p.getUniqueId())) {
                    continue;
                }
                Integer pos = track.getTimeTrials().getPlayerTopListPosition(p);
                if (pos != null && pos > 0) {
                    double add = 1000.0 / Math.pow(pos.doubleValue(), 0.5);
                    points.merge(p, add, Double::sum);
                }
            }
        }
        return points.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static String formatPoints(double pts) {
        long rounded = Math.round(pts);
        return String.valueOf(rounded);
    }
}
