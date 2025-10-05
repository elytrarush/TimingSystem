package me.makkuusen.timing.system.replay;

import me.jumper251.replay.api.IReplayHook;
import me.jumper251.replay.replaysystem.data.ActionData;
import me.jumper251.replay.replaysystem.data.types.PacketData;
import me.jumper251.replay.replaysystem.replaying.Replayer;

import java.util.Collections;
import java.util.List;

/**
 * Lightweight hook implementation – we do not inject additional packet data yet,
 * but registering the hook keeps the integration ready for future extensions.
 */
public class TimingSystemReplayHook implements IReplayHook {

    @Override
    public List<PacketData> onRecord(String playerName) {
        return Collections.emptyList();
    }

    @Override
    public void onPlay(ActionData data, Replayer replayer) {
        // No-op for now – replays are played back with their original data.
    }
}
