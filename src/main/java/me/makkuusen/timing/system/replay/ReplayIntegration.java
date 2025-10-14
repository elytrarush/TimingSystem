package me.makkuusen.timing.system.replay;

import me.jumper251.replay.api.ReplayAPI;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Minimal integration with AdvancedReplay: only records and stops replays via ReplayAPI.
 */
public final class ReplayIntegration {

    private static final ReplayIntegration INSTANCE = new ReplayIntegration();

    // Keep track of the current replay id per player so we can stop the right session
    private final Map<UUID, String> activeReplayIds = new ConcurrentHashMap<>();

    private boolean enabled;

    private ReplayIntegration() {
    }

    public static ReplayIntegration getInstance() {
        return INSTANCE;
    }

    public void enable(Plugin plugin) {
        if (enabled) return;

        if (!Bukkit.getPluginManager().isPluginEnabled("AdvancedReplay")) {
            TimingSystem.getPlugin().getLogger().info("AdvancedReplay not detected – replay integration remains disabled.");
            return;
        }

        enabled = true;
        TimingSystem.getPlugin().getLogger().info("AdvancedReplay minimal integration enabled.");
    }

    public void startRecording(Player player, Track track) {
        if (!enabled || player == null || track == null) return;

        UUID playerId = player.getUniqueId();

        // Stop any previous recording for this player (do not save it)
        String previousId = activeReplayIds.remove(playerId);
        if (previousId != null) {
            try {
                ReplayAPI.getInstance().stopReplay(previousId, false);
            } catch (Throwable t) {
                TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to stop previous replay " + previousId, t);
            }
        }

        String replayId = buildReplayId(track, playerId);
        try {
            ReplayAPI.getInstance().recordReplay(replayId, player);
            activeReplayIds.put(playerId, replayId);
        } catch (Throwable t) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to start replay recording for " + player.getName(), t);
        }
    }

    public void completeAttempt(Player player, Track track, long finishTime, boolean personalBest) {
        if (!enabled || player == null) return;

        UUID playerId = player.getUniqueId();
        String replayId = activeReplayIds.remove(playerId);
        if (replayId == null) return;

        try {
            // Save the replay on completion
            ReplayAPI.getInstance().stopReplay(replayId, true);
        } catch (Throwable t) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to stop replay " + replayId, t);
        }
    }

    public void abandonAttempt(UUID playerId) {
        if (!enabled) return;

        String replayId = activeReplayIds.remove(playerId);
        if (replayId == null) return;

        try {
            // Do not save the replay when abandoning
            ReplayAPI.getInstance().stopReplay(replayId, false);
        } catch (Throwable t) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to stop (discard) replay " + replayId, t);
        }
    }

    private String buildReplayId(Track track, UUID playerId) {
        return "timingsystem-" + track.getId() + "-" + playerId;
    }
}
