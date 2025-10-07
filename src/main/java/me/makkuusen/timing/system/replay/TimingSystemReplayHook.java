package me.makkuusen.timing.system.replay;

import me.jumper251.replay.api.IReplayHook;
import me.jumper251.replay.replaysystem.data.ActionData;
import me.jumper251.replay.replaysystem.data.types.MetadataUpdate;
import me.jumper251.replay.replaysystem.data.types.PacketData;
import me.jumper251.replay.replaysystem.replaying.Replayer;
import me.makkuusen.timing.system.timetrial.TimeTrial;
import me.makkuusen.timing.system.timetrial.TimeTrialController;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.options.TrackOption;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Replay hook used to enrich recordings with metadata such as the player's gliding state.
 */
public class TimingSystemReplayHook implements IReplayHook {

    @Override
    public List<PacketData> onRecord(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) {
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

        if (!gliding && !burning && !blocking && !swimming) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new MetadataUpdate(burning, blocking, gliding, swimming));
    }

    @Override
    public void onPlay(ActionData data, Replayer replayer) {
        // No-op for now – replays are played back with their original data.
    }
}
