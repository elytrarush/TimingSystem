package me.makkuusen.timing.system.track.regions;

import me.makkuusen.timing.system.TimingSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RocketAreaHighlighter {

    private static final double OFFSET = 0.02D;
    private static final Color HIGHLIGHT_COLOR = Color.fromARGB(120, 255, 140, 0);
    private static final byte OPACITY = (byte) 140;

    private static final Map<Integer, HighlightHandle> ACTIVE = new ConcurrentHashMap<>();

    private RocketAreaHighlighter() {
    }

    static void refresh(TrackRegion region) {
        if (TimingSystem.getPlugin() == null) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> refresh(region));
            return;
        }
        remove(region);
        if (!shouldHighlight(region)) {
            return;
        }
        spawn(region);
    }

    static void remove(TrackRegion region) {
        if (TimingSystem.getPlugin() == null) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> remove(region));
            return;
        }
        if (region == null) {
            return;
        }
        HighlightHandle handle = ACTIVE.remove(region.getId());
        if (handle == null) {
            return;
        }
        World world = Bukkit.getWorld(handle.worldName());
        if (world == null) {
            return;
        }
        for (UUID uuid : handle.entityIds()) {
            var entity = world.getEntity(uuid);
            if (entity instanceof TextDisplay textDisplay) {
                textDisplay.remove();
            }
        }
    }

    public static void clearAll() {
        if (TimingSystem.getPlugin() == null) {
            ACTIVE.clear();
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), RocketAreaHighlighter::clearAll);
            return;
        }
        ACTIVE.values().forEach(handle -> {
            World world = Bukkit.getWorld(handle.worldName());
            if (world == null) {
                return;
            }
            for (UUID uuid : handle.entityIds()) {
                var entity = world.getEntity(uuid);
                if (entity instanceof TextDisplay td) {
                    td.remove();
                }
            }
        });
        ACTIVE.clear();
    }

    private static boolean shouldHighlight(TrackRegion region) {
        if (region == null) {
            return false;
        }
        if (TimingSystem.getPlugin() == null) {
            return false;
        }
        if (!region.isHighlightEnabled()) {
            return false;
        }
        if (!region.getRegionType().equals(TrackRegion.RegionType.ROCKET_AREA)) {
            return false;
        }
        if (region.getShape() != TrackRegion.RegionShape.CUBOID) {
            return false;
        }
        if (!region.isDefined()) {
            return false;
        }
        if (region.getMinP() == null || region.getMaxP() == null) {
            return false;
        }
        var spawn = region.getSpawnLocation();
        return spawn != null && spawn.isWorldLoaded();
    }

    private static void spawn(TrackRegion region) {
        var world = region.getSpawnLocation().getWorld();
        if (world == null) {
            return;
        }
        var min = region.getMinP();
        var max = region.getMaxP();
        if (min == null || max == null) {
            return;
        }

        double minX = Math.min(min.getX(), max.getX());
        double minY = Math.min(min.getY(), max.getY());
        double minZ = Math.min(min.getZ(), max.getZ());
        double maxX = Math.max(min.getX(), max.getX()) + 1.0D;
        double maxY = Math.max(min.getY(), max.getY()) + 1.0D;
        double maxZ = Math.max(min.getZ(), max.getZ()) + 1.0D;

        double width = maxX - minX;
        double height = maxY - minY;
        double depth = maxZ - minZ;

        double centerX = (minX + maxX) / 2.0D;
        double centerY = (minY + maxY) / 2.0D;
        double centerZ = (minZ + maxZ) / 2.0D;

        List<UUID> displays = new ArrayList<>(6);
    displays.add(spawnFace(world, centerX, centerY, minZ - OFFSET, 180f, 0f, width, height));
    displays.add(spawnFace(world, centerX, centerY, maxZ + OFFSET, 0f, 0f, width, height));
    displays.add(spawnFace(world, minX - OFFSET, centerY, centerZ, 90f, 0f, depth, height));
    displays.add(spawnFace(world, maxX + OFFSET, centerY, centerZ, -90f, 0f, depth, height));
    displays.add(spawnFace(world, centerX, maxY + OFFSET, centerZ, 0f, -90f, width, depth));
    displays.add(spawnFace(world, centerX, minY - OFFSET, centerZ, 0f, 90f, width, depth));

        ACTIVE.put(region.getId(), new HighlightHandle(world.getName(), displays));
    }

    private static UUID spawnFace(World world, double x, double y, double z, float yaw, float pitch, double scaleX, double scaleY) {
        TextDisplay display = world.spawn(new Location(world, x, y, z), TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.FIXED);
            td.setShadowed(false);
            td.setSeeThrough(true);
            td.setTextOpacity(OPACITY);
            td.setBackgroundColor(HIGHLIGHT_COLOR);
            td.setBrightness(new Display.Brightness(15, 15));
            td.setPersistent(true);
            td.setViewRange(48f);
            td.setInterpolationDuration(0);
            td.text(Component.text("\u2588").color(TextColor.color(0xFF8C00)));
            td.setLineWidth(1);
            td.setRotation(yaw, pitch);
            td.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f((float) Math.max(scaleX, 0.01D), (float) Math.max(scaleY, 0.01D), 1f),
                    new Quaternionf()
            ));
        });
        return display.getUniqueId();
    }

    private record HighlightHandle(String worldName, List<UUID> entityIds) {
    }
}
