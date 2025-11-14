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

    private final Map<String, MetadataSnapshot> lastSnapshots = new ConcurrentHashMap<>();

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
        npc.setData(watcher.getMetadata(metadataBuilder));
        npc.updateMetadata();
    }

    private void updateViewerPosition(Replayer replayer) {
        if (replayer == null) {
            return;
        }

        Player viewer = replayer.getWatchingPlayer();
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        INPC npc = getFirstNpc(replayer);
        if (npc == null) {
            return;
        }

        Location npcLocation = npc.getLocation();
        if (npcLocation == null) {
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

        if (viewerLocation.distanceSquared(target) > 0.04) {
            viewer.teleport(target);
        }

        if (!viewer.getAllowFlight()) {
            viewer.setAllowFlight(true);
        }
        if (!viewer.isFlying()) {
            viewer.setFlying(true);
        }
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
