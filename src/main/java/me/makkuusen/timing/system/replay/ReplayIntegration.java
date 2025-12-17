package me.makkuusen.timing.system.replay;

import me.jumper251.replay.api.ReplayAPI;
import me.jumper251.replay.filesystem.saving.IReplaySaver;
import me.jumper251.replay.replaysystem.Replay;
import me.jumper251.replay.api.ReplaySessionFinishEvent;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ReplayIntegration implements Listener {

    private static final ReplayIntegration INSTANCE = new ReplayIntegration();

    private final Map<UUID, AttemptContext> activeAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptContext> contextsByReplayId = new ConcurrentHashMap<>();

    private boolean enabled;

    private ReplayIntegration() {
    }

    public static ReplayIntegration getInstance() {
        return INSTANCE;
    }

    public void enable(Plugin plugin) {
        if (enabled) {
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("AdvancedReplay")) {
            TimingSystem.getPlugin().getLogger().info("AdvancedReplay not detected – replay integration remains disabled.");
            return;
        }

        try {
            IReplaySaver saver = new TimingSystemReplaySaver(plugin.getDataFolder());
            ReplayAPI.getInstance().registerReplaySaver(saver);
            ReplayAPI.getInstance().registerHook(new TimingSystemReplayHook());
            Bukkit.getPluginManager().registerEvents(this, plugin);
            enabled = true;
            TimingSystem.getPlugin().getLogger().info("AdvancedReplay integration enabled.");
        } catch (Throwable throwable) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to initialize AdvancedReplay integration", throwable);
        }
    }

    public void startRecording(Player player, Track track) {
        if (!enabled || player == null || track == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        AttemptContext previous = activeAttempts.remove(playerId);
        if (previous != null) {
            stopRecording(previous, false);
        }

        String replayId = buildReplayId(track, playerId);
        contextsByReplayId.remove(replayId);
    ReplayAPI.getInstance().recordReplay(replayId, player);
    AttemptContext context = new AttemptContext(replayId, playerId, player.getName(), track.getId(), track.getDisplayName(), Instant.now());
        activeAttempts.put(playerId, context);
        contextsByReplayId.put(replayId, context);
    }

    public void completeAttempt(Player player, Track track, long finishTime, boolean personalBest) {
        if (!enabled || player == null) {
            return;
        }
        AttemptContext context = activeAttempts.get(player.getUniqueId());
        if (context == null) {
            return;
        }
        context.trackName = track.getDisplayName();
        context.finishTime = finishTime;
        context.personalBest = personalBest;
        stopRecording(context, personalBest);
    }

    public void abandonAttempt(UUID playerId) {
        if (!enabled) {
            return;
        }
        AttemptContext context = activeAttempts.get(playerId);
        if (context == null) {
            return;
        }
        context.personalBest = false;
        stopRecording(context, false);
    }

    public Optional<AttemptContext> pollContext(String replayId) {
        return Optional.ofNullable(contextsByReplayId.remove(replayId));
    }

    @EventHandler
    public void onReplaySessionFinished(ReplaySessionFinishEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();

        ReplayCameraManager.getInstance().onReplayEnd(player);
        restoreViewerState(player);

        Replay replay = event.getReplay();
        AttemptContext context = contextsByReplayId.get(replay.getId());
        if (context == null || !context.personalBest) {
            return;
        }
        if (player != null && player.isOnline()) {
            player.sendMessage("§aYour personal best replay was saved. Use /replay play " + context.storageFileName() + " to watch it!");
        }
    }

    private void restoreViewerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!player.hasGravity()) {
            player.setGravity(true);
        }

        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return;
        }

        if (player.isFlying()) {
            player.setFlying(false);
        }
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
        }
    }

    private void stopRecording(AttemptContext context, boolean shouldPersist) {
        activeAttempts.remove(context.playerId);
        contextsByReplayId.put(context.replayId, context);
        try {
            ReplayAPI.getInstance().stopReplay(context.replayId, true, !shouldPersist);
        } catch (Throwable throwable) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to stop replay " + context.replayId, throwable);
        }
    }

    private String buildReplayId(Track track, UUID playerId) {
        return "timingsystem-" + track.getId() + "-" + playerId;
    }

    public static final class AttemptContext {
        private final String replayId;
        private final UUID playerId;
        private final String playerName;
        private final int trackId;
        private String trackName;
        private final Instant startedAt;
        private boolean personalBest;
        private long finishTime;

    private AttemptContext(String replayId, UUID playerId, String playerName, int trackId, String trackName, Instant startedAt) {
            this.replayId = replayId;
            this.playerId = playerId;
            this.playerName = playerName;
            this.trackId = trackId;
            this.trackName = trackName;
            this.startedAt = startedAt;
            this.finishTime = -1L;
            this.personalBest = false;
        }

        public String storageFileName() {
            return replayId;
        }

        public UUID playerId() {
            return playerId;
        }

        public String playerName() {
            return playerName;
        }

        public int trackId() {
            return trackId;
        }

        public String trackName() {
            return trackName;
        }

        public long finishTime() {
            return finishTime;
        }

        public boolean personalBest() {
            return personalBest;
        }

        public long startedAt() {
            return startedAt.toEpochMilli();
        }
    }
}
