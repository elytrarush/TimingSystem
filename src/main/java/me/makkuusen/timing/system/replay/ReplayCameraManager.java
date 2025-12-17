package me.makkuusen.timing.system.replay;

import me.jumper251.replay.replaysystem.Replay;
import me.jumper251.replay.replaysystem.replaying.Replayer;
import me.jumper251.replay.replaysystem.replaying.ReplayHelper;
import me.jumper251.replay.utils.ReplayManager;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReplayCameraManager {

    private static final ReplayCameraManager INSTANCE = new ReplayCameraManager();

    private final Map<UUID, ViewerState> viewerStates = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> followTasks = new ConcurrentHashMap<>();

    private ReplayCameraManager() {
    }

    public static ReplayCameraManager getInstance() {
        return INSTANCE;
    }

    public void ensureWatching(Player viewer) {
        if (viewer == null) {
            return;
        }

        UUID viewerId = viewer.getUniqueId();
        if (viewerStates.containsKey(viewerId)) {
            return;
        }

        if (findReplayer(viewer) == null) {
            return;
        }

        viewerStates.put(viewerId, new ViewerState(viewer.getGameMode()));
        sendHint(viewer);
        ensureFollowTask(viewer);
    }

    public boolean isWatching(Player viewer) {
        if (viewer == null) {
            return false;
        }
        if (viewerStates.containsKey(viewer.getUniqueId())) {
            return true;
        }
        return findReplayer(viewer) != null;
    }

    public ReplayCameraMode getMode(Player viewer) {
        if (viewer == null) {
            return ReplayCameraMode.FOLLOW;
        }
        ViewerState state = viewerStates.get(viewer.getUniqueId());
        return state == null ? ReplayCameraMode.FOLLOW : state.mode;
    }

    public boolean setMode(Player viewer, ReplayCameraMode mode) {
        if (viewer == null || mode == null) {
            return false;
        }

        ensureWatching(viewer);
        ViewerState state = viewerStates.get(viewer.getUniqueId());
        if (state == null) {
            return false;
        }

        if (state.mode == mode) {
            return true;
        }

        state.mode = mode;
        TimingSystemReplayHook.clearCameraPosition(viewer.getUniqueId());

        if (mode == ReplayCameraMode.FREE) {
            enterSpectator(viewer, state);
        } else {
            exitSpectator(viewer, state);
            ensureFollowTask(viewer);
        }

        return true;
    }

    public void onReplayEnd(Player viewer) {
        if (viewer == null) {
            return;
        }

        ViewerState state = viewerStates.remove(viewer.getUniqueId());
        cancelFollowTask(viewer.getUniqueId());
        TimingSystemReplayHook.clearCameraPosition(viewer.getUniqueId());
        if (state == null) {
            return;
        }

        exitSpectator(viewer, state);
    }

    private void ensureFollowTask(Player viewer) {
        UUID viewerId = viewer.getUniqueId();
        if (followTasks.containsKey(viewerId)) {
            return;
        }

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(TimingSystem.getPlugin(), () -> {
            Player onlineViewer = Bukkit.getPlayer(viewerId);
            if (onlineViewer == null || !onlineViewer.isOnline()) {
                cancelFollowTask(viewerId);
                viewerStates.remove(viewerId);
                TimingSystemReplayHook.clearCameraPosition(viewerId);
                return;
            }

            ViewerState state = viewerStates.get(viewerId);
            if (state == null) {
                cancelFollowTask(viewerId);
                TimingSystemReplayHook.clearCameraPosition(viewerId);
                return;
            }

            Replayer replayer = findReplayer(onlineViewer);
            if (replayer == null) {
                cancelFollowTask(viewerId);
                TimingSystemReplayHook.clearCameraPosition(viewerId);
                return;
            }

            if (state.mode == ReplayCameraMode.FOLLOW) {
                TimingSystemReplayHook.updateFollowCamera(onlineViewer, replayer);
            }
        }, 1L, 1L);

        followTasks.put(viewerId, taskId);
    }

    private void cancelFollowTask(UUID viewerId) {
        Integer taskId = followTasks.remove(viewerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private Replayer findReplayer(Player viewer) {
        try {
            if (ReplayHelper.replaySessions != null) {
                Replayer session = ReplayHelper.replaySessions.get(viewer.getName());
                if (session != null) {
                    return session;
                }
            }

            Map<String, Replay> active = ReplayManager.activeReplays;
            if (active == null || active.isEmpty()) {
                return null;
            }
            for (Replay replay : active.values()) {
                if (replay == null || !replay.isPlaying()) {
                    continue;
                }
                Replayer replayer = replay.getReplayer();
                if (replayer == null) {
                    continue;
                }
                Player watching = replayer.getWatchingPlayer();
                if (watching != null && watching.getUniqueId().equals(viewer.getUniqueId())) {
                    return replayer;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void enterSpectator(Player viewer, ViewerState state) {
        if (!viewer.isOnline()) {
            return;
        }

        if (isSpectatorPluginAvailable() && viewer.hasPermission("spectator.commands.spectatehere")) {
            state.usedSpectatorPlugin = true;
            viewer.performCommand("spectatehere");
            return;
        }

        state.usedSpectatorPlugin = false;
        if (viewer.getGameMode() != GameMode.SPECTATOR) {
            state.changedGameMode = true;
            viewer.setGameMode(GameMode.SPECTATOR);
        }
    }

    private void exitSpectator(Player viewer, ViewerState state) {
        if (!viewer.isOnline()) {
            return;
        }

        if (state.usedSpectatorPlugin && isSpectatorPluginAvailable() && viewer.hasPermission("spectator.commands.spectatehere")) {
            viewer.performCommand("spectatehere");
            state.usedSpectatorPlugin = false;
            state.changedGameMode = false;
            return;
        }

        if (state.changedGameMode && viewer.getGameMode() == GameMode.SPECTATOR) {
            viewer.setGameMode(state.originalGameMode);
        }
        state.changedGameMode = false;
    }

    private boolean isSpectatorPluginAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("Spectator");
    }

    private void sendHint(Player viewer) {
        if (!viewer.isOnline()) {
            return;
        }
        viewer.sendMessage("Replay camera: /ts replay camera follow|free");
    }

    private static final class ViewerState {
        private final GameMode originalGameMode;
        private ReplayCameraMode mode;
        private boolean usedSpectatorPlugin;
        private boolean changedGameMode;

        private ViewerState(GameMode originalGameMode) {
            this.originalGameMode = originalGameMode;
            this.mode = ReplayCameraMode.FOLLOW;
            this.usedSpectatorPlugin = false;
            this.changedGameMode = false;
        }
    }
}
