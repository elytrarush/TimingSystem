package me.makkuusen.timing.system.track.options;

import com.sk89q.worldedit.math.BlockVector3;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.regions.TrackCuboidRegion;
import me.makkuusen.timing.system.track.regions.TrackPolyRegion;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the visual glow cue for the upcoming checkpoint when the associated track option is enabled.
 */
public final class CheckpointGlowManager {

    private static final Map<UUID, GlowTask> ACTIVE_TASKS = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<Integer, List<Location>>> LOCATION_CACHE = new ConcurrentHashMap<>();
    private static final long TASK_PERIOD_TICKS = 20L; // 1 second cadence keeps the effect readable without overloading clients.
    private static final int MAX_PARTICLE_LOCATIONS = 512;

    private CheckpointGlowManager() {
    }

    public static void updateGlow(Player player, Track track, int checkpointIndex) {
        if (player == null) {
            return;
        }

        if (track == null || checkpointIndex <= 0 || checkpointIndex > track.getNumberOfCheckpoints()) {
            clear(player.getUniqueId());
            return;
        }

        if (!track.getTrackOptions().hasOption(TrackOption.NEXT_CHECKPOINT_GLOWING)) {
            clear(player.getUniqueId());
            return;
        }

        var regions = track.getTrackRegions().getCheckpoints(checkpointIndex);
        if (regions.isEmpty()) {
            clear(player.getUniqueId());
            return;
        }

        var locations = getCachedLocations(track.getId(), checkpointIndex, regions);
        if (locations.isEmpty()) {
            clear(player.getUniqueId());
            return;
        }

        ACTIVE_TASKS.compute(player.getUniqueId(), (uuid, existing) -> {
            if (existing == null) {
                var task = new GlowTask(uuid, track);
                task.updateLocations(locations);
                return task;
            }
            existing.setTrack(track);
            existing.updateLocations(locations);
            return existing;
        });
    }

    public static void clear(Player player) {
        if (player == null) {
            return;
        }
        clear(player.getUniqueId());
    }

    public static void clear(UUID playerId) {
        var task = ACTIVE_TASKS.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public static void invalidateCache(int trackId) {
        LOCATION_CACHE.remove(trackId);
    }

    public static void shutdown() {
        List<GlowTask> tasks = new ArrayList<>(ACTIVE_TASKS.values());
        tasks.forEach(GlowTask::cancel);
        ACTIVE_TASKS.clear();
        LOCATION_CACHE.clear();
    }

    private static List<Location> getCachedLocations(int trackId, int checkpointIndex, List<TrackRegion> regions) {
        var perTrack = LOCATION_CACHE.computeIfAbsent(trackId, ignored -> new ConcurrentHashMap<>());
        return perTrack.computeIfAbsent(checkpointIndex, ignored -> Collections.unmodifiableList(limitLocations(computeLocations(regions))));
    }

    private static List<Location> computeLocations(List<TrackRegion> regions) {
        Set<Location> result = new LinkedHashSet<>();
        for (TrackRegion region : regions) {
            if (region == null || !region.isDefined()) {
                continue;
            }
            if (region.getSpawnLocation() == null || !region.getSpawnLocation().isWorldLoaded()) {
                continue;
            }

            if (region instanceof TrackPolyRegion polyRegion) {
                result.addAll(computePolyLocations(polyRegion));
            } else if (region instanceof TrackCuboidRegion cuboidRegion) {
                result.addAll(computeCuboidLocations(cuboidRegion));
            } else {
                result.addAll(computeCuboidLocations(region));
            }
        }
        return new ArrayList<>(result);
    }

    private static List<Location> limitLocations(List<Location> locations) {
        if (locations.size() <= MAX_PARTICLE_LOCATIONS) {
            return locations;
        }
        List<Location> limited = new ArrayList<>(MAX_PARTICLE_LOCATIONS);
        double step = (double) locations.size() / MAX_PARTICLE_LOCATIONS;
        double index = 0;
        for (int i = 0; i < MAX_PARTICLE_LOCATIONS; i++) {
            limited.add(locations.get((int) index));
            index += step;
        }
        return limited;
    }

    private static List<Location> computeCuboidLocations(TrackRegion region) {
        Location min = region.getMinP();
        Location max = region.getMaxP();
        if (min == null || max == null) {
            return List.of();
        }

        World world = min.getWorld();
        if (world == null) {
            return List.of();
        }

        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());
        int y = Math.max(min.getBlockY(), max.getBlockY());

        List<Location> locations = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                locations.add(new Location(world, x + 0.5, y + 0.5, z + 0.5));
            }
        }
        return locations;
    }

    private static List<Location> computePolyLocations(TrackPolyRegion polyRegion) {
        var poly = polyRegion.getPolygonal2DRegion();
        if (poly == null) {
            return List.of();
        }
        var world = polyRegion.getSpawnLocation().getWorld();
        if (world == null) {
            return List.of();
        }

        BlockVector3 min = poly.getMinimumPoint();
        BlockVector3 max = poly.getMaximumPoint();
        int y = polyRegion.getMaxP().getBlockY();

        List<Location> locations = new ArrayList<>();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                if (poly.contains(BlockVector3.at(x, y, z))) {
                    locations.add(new Location(world, x + 0.5, y + 0.5, z + 0.5));
                }
            }
        }
        return locations;
    }

    private static final class GlowTask implements Runnable {

        private final UUID playerId;
        private final BukkitTask task;
        private volatile List<Location> locations = List.of();
        private volatile Track track;

        private GlowTask(UUID playerId, Track track) {
            this.playerId = playerId;
            this.track = track;
            this.task = Bukkit.getScheduler().runTaskTimer(TimingSystem.getPlugin(), this, 0L, TASK_PERIOD_TICKS);
        }

        private void setTrack(Track track) {
            this.track = track;
        }

        private void updateLocations(List<Location> newLocations) {
            this.locations = newLocations;
        }

        @Override
        public void run() {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                cancel();
                return;
            }

            var currentTrack = this.track;
            if (currentTrack == null || !currentTrack.getTrackOptions().hasOption(TrackOption.NEXT_CHECKPOINT_GLOWING)) {
                cancel();
                return;
            }

            if (locations.isEmpty()) {
                return;
            }

            for (Location location : locations) {
                if (location.getWorld() == null || !location.getWorld().equals(player.getWorld())) {
                    continue;
                }
                player.spawnParticle(Particle.GLOW, location, 1, 0, 0, 0, 0);
            }
        }

        private void cancel() {
            task.cancel();
            ACTIVE_TASKS.remove(playerId, this);
        }
    }
}
