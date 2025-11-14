package me.makkuusen.timing.system.replay;

import me.jumper251.replay.api.IReplayHook;
import me.jumper251.replay.replaysystem.data.ActionData;
import me.jumper251.replay.replaysystem.data.ReplayData;
import me.jumper251.replay.replaysystem.data.types.MetadataUpdate;
import me.jumper251.replay.replaysystem.data.types.PacketData;
import me.jumper251.replay.replaysystem.recording.PlayerWatcher;
import me.jumper251.replay.replaysystem.replaying.Replayer;
import me.jumper251.replay.replaysystem.utils.MetadataBuilder;
import me.jumper251.replay.replaysystem.utils.entities.INPC;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.options.TrackOption;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replay hook used to enrich recordings with metadata such as the player's gliding state.
 */
public class TimingSystemReplayHook implements IReplayHook {

    private static final double FOLLOW_DISTANCE = 2.5D;
    private static final double FOLLOW_HEIGHT = 1.25D;
    private static final double SMOOTHING_FACTOR = 0.35D;
    private static final double SNAP_DISTANCE_SQUARED = 0.04D;
    private static final double TELEPORT_EPSILON_SQUARED = 0.0001D;

    private final Map<String, MetadataSnapshot> lastSnapshots = new ConcurrentHashMap<>();
    private final Map<String, Location> lastCameraPositions = new ConcurrentHashMap<>();

    @Override
    public List<PacketData> onRecord(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
            lastSnapshots.remove(playerName);
            return Collections.emptyList();
        }

        boolean burning = player.getFireTicks() > 0;
        boolean blocking = player.isBlocking();
        boolean swimming = player.isSwimming();
        boolean gliding = player.isGliding();

        TimeTrial timeTrial = TimeTrialController.timeTrials.get(player.getUniqueId());
        if (!gliding && timeTrial != null) {
            Track track = timeTrial.getTrack();
            if (track != null && (track.isElytraTrack() || track.getTrackOptions().hasOption(TrackOption.FORCE_ELYTRA))) {
                gliding = true;
            }
        }

        MetadataSnapshot currentSnapshot = new MetadataSnapshot(burning, blocking, gliding, swimming);
        MetadataSnapshot previousSnapshot = lastSnapshots.get(playerName);

        if (currentSnapshot.equals(previousSnapshot)) {
            return Collections.emptyList();
        }

        lastSnapshots.put(playerName, currentSnapshot);
        return Collections.singletonList(currentSnapshot.asPacket());
    }

    @Override
    public void onPlay(ActionData data, Replayer replayer) {
        updateViewerPosition(replayer);

        if (!(data.getPacketData() instanceof MetadataUpdate metadataUpdate)) {
            return;
        }

        INPC npc = replayer.getNPCList().get(data.getName());
        if (npc == null) {
            return;
        }

        PlayerWatcher watcher = getOrCreateWatcher(replayer, data.getName());
        watcher.setBurning(metadataUpdate.isBurning());
        watcher.setBlocking(metadataUpdate.isBlocking());
        watcher.setElytra(metadataUpdate.isGliding());
        watcher.setSwimming(metadataUpdate.isSwimming());

        MetadataBuilder metadataBuilder = new MetadataBuilder();
        watcher.getMetadata(metadataBuilder);

        if (metadataUpdate.isGliding() && VersionUtil.isAbove(VersionEnum.V1_14)) {
            metadataBuilder.setPoseField("FALL_FLYING");
        }

        npc.setData(metadataBuilder.getData());
        npc.updateMetadata();
    }



    private void updateViewerPosition(Replayer replayer) {
        if (replayer == null) {
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(TimingSystem.getPlugin(), () -> updateViewerPosition(replayer));
            return;
        }

        Player viewer = replayer.getWatchingPlayer();
        if (viewer == null || !viewer.isOnline()) {
            if (viewer != null) {
                lastCameraPositions.remove(viewer.getName());
            }
            return;
        }

        String viewerName = viewer.getName();

        INPC npc = getFirstNpc(replayer);
        if (npc == null) {
            lastCameraPositions.remove(viewerName);
            return;
        }

        Location npcLocation = npc.getLocation();
        if (npcLocation == null) {
            lastCameraPositions.remove(viewerName);
            return;
        }

        Location viewerLocation = viewer.getLocation();
        Vector direction = npcLocation.getDirection();
        if (direction.lengthSquared() == 0) {
            direction = viewerLocation.getDirection();
        }

        Vector backwards = direction.normalize().multiply(-FOLLOW_DISTANCE);
        Location target = npcLocation.clone().add(backwards).add(0, FOLLOW_HEIGHT, 0);
        target.setDirection(npcLocation.getDirection());

        Location nextLocation = smoothCameraPosition(viewerName, target);
        if (!viewerLocation.getWorld().equals(nextLocation.getWorld())) {
            viewer.teleport(nextLocation);
        } else if (viewerLocation.distanceSquared(nextLocation) > TELEPORT_EPSILON_SQUARED) {
            viewer.teleport(nextLocation);
        }

        if (!viewer.getAllowFlight()) {
            viewer.setAllowFlight(true);
        }
        if (!viewer.isFlying()) {
            viewer.setFlying(true);
        }
        if (viewer.hasGravity()) {
            viewer.setGravity(false);
        }
    }

    private Location smoothCameraPosition(String viewerName, Location target) {
        Location previous = lastCameraPositions.get(viewerName);
        if (previous == null || !previous.getWorld().equals(target.getWorld())) {
            Location clone = target.clone();
            lastCameraPositions.put(viewerName, clone);
            return clone;
        }

        if (previous.distanceSquared(target) <= SNAP_DISTANCE_SQUARED) {
            Location clone = target.clone();
            lastCameraPositions.put(viewerName, clone);
            return clone;
        }

        Location next = previous.clone();
        next.setX(lerp(previous.getX(), target.getX()));
        next.setY(lerp(previous.getY(), target.getY()));
        next.setZ(lerp(previous.getZ(), target.getZ()));
        next.setYaw(target.getYaw());
        next.setPitch(target.getPitch());

        lastCameraPositions.put(viewerName, next.clone());
        return next;
    }

    private double lerp(double start, double end) {
        return start + (end - start) * SMOOTHING_FACTOR;
    }

    private INPC getFirstNpc(Replayer replayer) {
        for (INPC npc : replayer.getNPCList().values()) {
            return npc;
        }
        return null;
    }

    private PlayerWatcher getOrCreateWatcher(Replayer replayer, String playerName) {
        ReplayData replayData = replayer.getReplay().getData();
        PlayerWatcher watcher = replayData.getWatcher(playerName);
        if (watcher == null) {
            watcher = new PlayerWatcher(playerName);
            replayData.getWatchers().put(playerName, watcher);
        }
        return watcher;
    }

    private record MetadataSnapshot(boolean burning, boolean blocking, boolean gliding, boolean swimming) {
        MetadataUpdate asPacket() {
            return new MetadataUpdate(burning, blocking, gliding, swimming);
        }
    }
}
