package me.makkuusen.timing.system.timetrial;

import lombok.Getter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.LeaderboardManager;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.api.events.TimeTrialAttemptEvent;
import me.makkuusen.timing.system.api.events.TimeTrialFinishEvent;
import me.makkuusen.timing.system.api.events.TimeTrialStartEvent;
import me.makkuusen.timing.system.replay.ReplayIntegration;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Broadcast;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Info;
import me.makkuusen.timing.system.track.Track;
import me.makkuusen.timing.system.track.medals.Medals;
import me.makkuusen.timing.system.track.regions.TrackRegion;
import me.makkuusen.timing.system.network.discord.DiscordNotifier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeTrial {

    private final TPlayer tPlayer;
    @Getter
    private final Track track;
    private Instant startTime;
    private ArrayList<Instant> checkpoints;
    private boolean lagStart = false;
    private Instant lagStartTime = null;
    private boolean lagEnd = false;
    @Getter
    private TimeTrialFinish bestFinish;


    public TimeTrial(Track track, TPlayer player) {
        this(track, player, TimingSystem.currentTime);
    }

    public TimeTrial(Track track, TPlayer player, Instant startTime) {
        this.track = track;
        this.startTime = startTime;
        this.checkpoints = new ArrayList<>();
        this.bestFinish = track.getTimeTrials().getBestFinish(player);
        this.tPlayer = player;
    }


    public long getBestTime() {
        if (bestFinish == null) {
            return -1;
        }
        return bestFinish.getTime();
    }


    @SuppressWarnings("unused")
    //Used in third party plugin
    public void setBestFinish(TimeTrialFinish finish) {
        bestFinish = finish;
    }

    private boolean hasNotPassedAllCheckpoints() {
        return checkpoints.size() != track.getNumberOfCheckpoints();
    }

    public int getNextCheckpoint() {
        if (track.getNumberOfCheckpoints() >= checkpoints.size()) {
            return checkpoints.size() + 1;
        }
        return checkpoints.size();
    }

    public void passNextCheckpoint(Instant timeStamp) {
        checkpoints.add(timeStamp);
    }

    public int getLatestCheckpoint() {
        return checkpoints.size();
    }

    public long getCheckpointTime(int checkpoint) {
        if (checkpoints.isEmpty() || checkpoint == 0) {
            return 0;
        }
        return getTimeSinceStart(checkpoints.get(checkpoint - 1));
    }

    public List<Long> getCheckpointTimes() {
        List<Long> checkpointTimes = new ArrayList<>();
        checkpoints.forEach(checkpoint -> checkpointTimes.add(getTimeSinceStart(checkpoint)));
        return checkpointTimes;
    }

    public long getCurrentTime() {
        return getTimeSinceStart(TimingSystem.currentTime);
    }

    public long getTimeSinceStart(Instant time) {
        return Duration.between(startTime, time).toMillis();
    }

    public void setLagStartTrue() {
        this.lagStart = true;
        lagStartTime = TimingSystem.currentTime;
    }

    public Instant getLagStart() {
        return lagStartTime;
    }

    public void setLagEnd(boolean lagEnd) {
        this.lagEnd = lagEnd;
    }

    public boolean isLagEnd() {
        return lagEnd;
    }

    public boolean isLagStart() {
        return lagStart;
    }

    public void playerPassingLagStart() {
        Player player = tPlayer.getPlayer();
        if (tPlayer.getSettings().isVerbose() && (player.isOp() || player.hasPermission("timingsystem.packs.trackadmin"))) {
            Text.send(player, Info.TIME_TRIAL_LAG_START, "%time%", ApiUtilities.formatAsTime(getTimeSinceStart(TimingSystem.currentTime)));
        }
    }

    public void playerPassingLagEnd() {
        Player player = tPlayer.getPlayer();
        if (tPlayer.getSettings().isVerbose() && (player.isOp() || player.hasPermission("timingsystem.packs.trackadmin"))) {
            Text.send(player, Info.TIME_TRIAL_LAG_END, "%time%", ApiUtilities.formatAsTime(getTimeSinceStart(TimingSystem.currentTime)));
        }
    }

    public void playerPassingNextCheckpoint() {
        passNextCheckpoint(TimingSystem.currentTime);
        long timeSinceStart = getTimeSinceStart(TimingSystem.currentTime);
        if (tPlayer.getSettings().isVerbose()) {
            Component delta = getBestLapDelta(tPlayer.getTheme(), getLatestCheckpoint());
            tPlayer.getPlayer().sendMessage(Text.get(tPlayer.getPlayer(), Info.TIME_TRIAL_CHECKPOINT, "%checkpoint%", String.valueOf(getLatestCheckpoint()), "%time%", ApiUtilities.formatAsTime(timeSinceStart)).append(delta));
        }
        ApiUtilities.msgConsole(tPlayer.getName() + " passed checkpoint " + getLatestCheckpoint() + " on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeSinceStart));
    }

    public void playerPassingNextCheckpoint(Location from, Location to, TrackRegion checkpoint) {
        Instant preciseTime = TimingSystem.currentTime;
        double proportion = calculateRegionEntryProportion(from, to, checkpoint);
        long tickDurationNanos = 50_000_000L;
        long adjustmentNanos = (long) ((1.0 - proportion) * tickDurationNanos);
        preciseTime = preciseTime.minusNanos(adjustmentNanos);

        passNextCheckpoint(preciseTime);
        long timeSinceStart = getTimeSinceStart(preciseTime);
        if (tPlayer.getSettings().isVerbose()) {
            Component delta = getBestLapDelta(tPlayer.getTheme(), getLatestCheckpoint());
            tPlayer.getPlayer().sendMessage(Text.get(tPlayer.getPlayer(), Info.TIME_TRIAL_CHECKPOINT, "%checkpoint%", String.valueOf(getLatestCheckpoint()), "%time%", ApiUtilities.formatAsTime(timeSinceStart)).append(delta));
        }
        ApiUtilities.msgConsole(tPlayer.getName() + " passed checkpoint " + getLatestCheckpoint() + " on " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeSinceStart));
    }

    public void playerResetMap() {
        var timeTrial = TimeTrialController.timeTrials.get(tPlayer.getUniqueId());
        if (timeTrial == null) {
            return;
        }
        var time = getTimeSinceStart(TimingSystem.currentTime);
        if (time > 1000) {
            var attempt = getTrack().getTimeTrials().newAttempt(time, tPlayer.getUniqueId());
            var eventTimeTrialAttempt = new TimeTrialAttemptEvent(tPlayer.getPlayer(), attempt);
            Bukkit.getServer().getPluginManager().callEvent(eventTimeTrialAttempt);
        }
        ReplayIntegration.getInstance().abandonAttempt(tPlayer.getUniqueId());
        TimeTrialController.timeTrials.remove(tPlayer.getUniqueId());
        ApiUtilities.teleportPlayerAndSpawnBoat(tPlayer.getPlayer(), track, track.getSpawnLocation());
        ApiUtilities.msgConsole(tPlayer.getName() + " has been reset on " + track.getDisplayName());

    }

    public void playerStartingTimeTrial() {
        Player player = tPlayer.getPlayer();

        if (!tPlayer.getSettings().isTimeTrial()) {
            return;
        }

        if (!track.isOpen() && !tPlayer.getSettings().isOverride()) {
            return;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack()) {
            return;
        }

        if (track.isBoatTrack() && !(player.getVehicle() instanceof Boat)) {
            return;
        }

        var eventTimeTrialStart = new TimeTrialStartEvent(tPlayer.getPlayer(), this);
        Bukkit.getServer().getPluginManager().callEvent(eventTimeTrialStart);

        TimeTrialController.timeTrials.put(tPlayer.getUniqueId(), this);
        ReplayIntegration.getInstance().startRecording(player, track);
        ApiUtilities.msgConsole(tPlayer.getName() + " started on " + track.getDisplayName());
    }

    public void playerEndedMap() {
        Instant endTime = TimingSystem.currentTime;
        Player player = tPlayer.getPlayer();

        boolean validFinish = validateFinish(player);
        if (validFinish) {
            long timeTrialTime = getTimeSinceStart(endTime);
            saveAndAnnounceFinish(player, timeTrialTime);
            ApiUtilities.msgConsole(player.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrialTime));
        } else {
            ReplayIntegration.getInstance().abandonAttempt(player.getUniqueId());
        }
        TimeTrialController.timeTrials.remove(player.getUniqueId());
    }

    public void playerEndedMap(Location from, Location to, TrackRegion endRegion) {
        Instant preciseEndTime = TimingSystem.currentTime;
        double proportion = calculateRegionEntryProportion(from, to, endRegion);
        long tickDurationNanos = 50_000_000L;
        long adjustmentNanos = (long) ((1.0 - proportion) * tickDurationNanos);
        preciseEndTime = preciseEndTime.minusNanos(adjustmentNanos);

        Player player = tPlayer.getPlayer();
        if (validateFinish(player)) {
            long timeTrialTime = getTimeSinceStart(preciseEndTime);
            saveAndAnnounceFinish(player, timeTrialTime);
            ApiUtilities.msgConsole(player.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrialTime));
        } else {
            ReplayIntegration.getInstance().abandonAttempt(player.getUniqueId());
        }
        TimeTrialController.timeTrials.remove(player.getUniqueId());
    }

    public void playerRestartMap() {
        Instant endTime = TimingSystem.currentTime;
        Player player = tPlayer.getPlayer();

        boolean validFinish = validateFinish(player);
        if (validFinish){
            long timeTrialTime = getTimeSinceStart(endTime);
            saveAndAnnounceFinish(player, timeTrialTime);
            ApiUtilities.msgConsole(player.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrialTime));
        } else {
            ReplayIntegration.getInstance().abandonAttempt(player.getUniqueId());
        }

        if (!track.isOpen() && !tPlayer.getSettings().isOverride()) {
            TimeTrialController.timeTrials.remove(player.getUniqueId());
        } else {
            resetTimeTrial();
        }
    }

    public void playerRestartMap(Location from, Location to, TrackRegion startRegion) {
        Instant preciseEndTime = TimingSystem.currentTime;
        double proportion = calculateRegionEntryProportion(from, to, startRegion);
        long tickDurationNanos = 50_000_000L;
        long adjustmentNanos = (long) ((1.0 - proportion) * tickDurationNanos);
        preciseEndTime = preciseEndTime.minusNanos(adjustmentNanos);

        Player player = tPlayer.getPlayer();

        boolean validFinish = validateFinish(player);
        if (validFinish) {
            long timeTrialTime = getTimeSinceStart(preciseEndTime);
            saveAndAnnounceFinish(player, timeTrialTime);
            ApiUtilities.msgConsole(player.getName() + " finished " + track.getDisplayName() + " with a time of " + ApiUtilities.formatAsTime(timeTrialTime));
        } else {
            ReplayIntegration.getInstance().abandonAttempt(player.getUniqueId());
        }

        if (!track.isOpen() && !tPlayer.getSettings().isOverride()) {
            TimeTrialController.timeTrials.remove(player.getUniqueId());
        } else {
            resetTimeTrial();
        }
    }

    private boolean validateFinish(Player player) {
        if (hasNotPassedAllCheckpoints()) {
            Text.send(player, Error.MISSED_CHECKPOINTS);
            return false;
        }

        if (track.getTrackRegions().hasRegion(TrackRegion.RegionType.LAGSTART) && !lagStart) {
            Text.send(player, Error.LAG_DETECTED);
            return false;
        }

        if (track.getTrackRegions().hasRegion(TrackRegion.RegionType.LAGEND) && !lagEnd) {
            Text.send(player, Error.LAG_DETECTED);
            return false;
        }

        if (!player.isInsideVehicle() && track.isBoatTrack()) {
            return false;
        }

        return true;
    }

    private void resetTimeTrial() {
        ApiUtilities.msgConsole(tPlayer.getName() + " started on " + track.getDisplayName());
        this.startTime = TimingSystem.currentTime;
        this.checkpoints = new ArrayList<>();
        this.lagStart = false;
        this.lagEnd = false;
        this.lagStartTime = null;
        ReplayIntegration.getInstance().startRecording(tPlayer.getPlayer(), track);
    }

    private void saveAndAnnounceFinish(Player player, long timeTrialTime) {

        Component finishMessage;
        Component medalMessage = null;
        TimeTrialFinish finish;
        boolean newPersonalBest = false;
        // Capture track top time before potentially inserting a new best for this player
        Long beforeTopTime = null;
        try {
            var beforeTop = track.getTimeTrials().getTopList(1);
            if (!beforeTop.isEmpty()) beforeTopTime = beforeTop.get(0).getTime();
        } catch (Exception ignored) {}
        if (bestFinish == null) {
            //First finish
            finish = newBestFinish(player, timeTrialTime, -1);
            finishMessage = Text.get(player, Info.TIME_TRIAL_FIRST_FINISH,"%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%pos%", String.valueOf(track.getTimeTrials().getPlayerTopListPosition(tPlayer)));
            finishMessage = tPlayer.getTheme().getCheckpointHovers(finish, finishMessage);
            if (TimingSystem.configuration.isMedalsAddOnEnabled()) {
                Medals prevMedal = track.getTrackMedals().getMedal(0);
                medalMessage = track.getTrackMedals().getMedalMessage(track.getTimeTrials(), player.hasResourcePack(), prevMedal, timeTrialTime, track.getDisplayName());
            }
            newPersonalBest = true;
        } else if (timeTrialTime < bestFinish.getTime()) {

            // Temporary fix to make TimingSystemTrackMerge integrate a little better.
            if (bestFinish.getTrack() != track.getId()) {
                var recordTrack = TimingSystemAPI.getTrackById(bestFinish.getTrack()).get();
                var oldPos = recordTrack.getTimeTrials().getCachedPlayerPosition(tPlayer);
                var oldFinish = bestFinish;
                finish = newBestFinish(player, timeTrialTime, oldFinish.getTime());
                finishMessage = Text.get(player, Info.TIME_TRIAL_NEW_RECORD, "%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%delta%", ApiUtilities.formatAsPersonalGap(oldFinish.getTime() - timeTrialTime), "%oldPos%", oldPos.toString(), "%pos%", recordTrack.getTimeTrials().getPlayerTopListPosition(tPlayer).toString());
                finishMessage = tPlayer.getTheme().getCheckpointHovers(finish, oldFinish, finishMessage);
                newPersonalBest = true;
            } else {
                //New personal best
                var oldPos = track.getTimeTrials().getCachedPlayerPosition(tPlayer);
                var oldFinish = bestFinish;
                Medals prevMedal = Medals.NO_MEDAL;
                if (TimingSystem.configuration.isMedalsAddOnEnabled()) { prevMedal = track.getTrackMedals().getMedal(oldFinish.getTime()); }
                finish = newBestFinish(player, timeTrialTime, oldFinish.getTime());
                finishMessage = Text.get(player, Info.TIME_TRIAL_NEW_RECORD, "%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%delta%", ApiUtilities.formatAsPersonalGap(oldFinish.getTime() - timeTrialTime), "%oldPos%", oldPos.toString(), "%pos%", track.getTimeTrials().getPlayerTopListPosition(tPlayer).toString());
                finishMessage = tPlayer.getTheme().getCheckpointHovers(finish, oldFinish, finishMessage);
                if (TimingSystem.configuration.isMedalsAddOnEnabled()) {
                    medalMessage = track.getTrackMedals().getMedalMessage(track.getTimeTrials(), player.hasResourcePack(), prevMedal, timeTrialTime, track.getDisplayName());
                }
                newPersonalBest = true;
            }
        } else {
            //Finish no improvement
            finish = callTimeTrialFinishEvent(player, timeTrialTime, bestFinish.getTime(), false);
            finishMessage = Text.get(player, Info.TIME_TRIAL_FINISH, "%track%", track.getDisplayName(), "%time%", ApiUtilities.formatAsTime(timeTrialTime), "%delta%", ApiUtilities.formatAsPersonalGap(timeTrialTime - bestFinish.getTime()));
            finishMessage = tPlayer.getTheme().getCheckpointHovers(finish, track.getTimeTrials().getBestFinish(tPlayer), finishMessage);

        }

        ReplayIntegration.getInstance().completeAttempt(player, track, timeTrialTime, newPersonalBest);

        // If this finish resulted in a new global track record, announce to server and post to Discord
        try {
            var afterTop = track.getTimeTrials().getTopList(1);
            if (!afterTop.isEmpty()) {
                var top = afterTop.get(0);
                boolean isNewRecord = (beforeTopTime == null) || (top.getTime() < beforeTopTime);
                if (isNewRecord && top.getTime() == timeTrialTime) {
                    String delta = beforeTopTime == null ? null : ApiUtilities.formatAsPersonalGap(beforeTopTime - timeTrialTime);
                    // Broadcast to all online players using i18n
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (delta == null) {
                            p.sendMessage(Text.get(p, Broadcast.NEW_TRACK_RECORD,
                                "%track%", track.getDisplayName(),
                                "%player%", player.getName(),
                                "%time%", ApiUtilities.formatAsTime(timeTrialTime)
                            ));
                        } else {
                            p.sendMessage(Text.get(p, Broadcast.NEW_TRACK_RECORD_DELTA,
                                "%track%", track.getDisplayName(),
                                "%player%", player.getName(),
                                "%time%", ApiUtilities.formatAsTime(timeTrialTime),
                                "%delta%", delta
                            ));
                        }
                    }
                    DiscordNotifier.sendNewRecord(track.getDisplayName(), player.getName(), ApiUtilities.formatAsTime(timeTrialTime), delta);
                }
            }
        } catch (Exception ignored) {}

        player.sendMessage(finishMessage);
        if (TimingSystem.configuration.isMedalsAddOnEnabled() && medalMessage != null) {
            player.sendMessage(medalMessage);
            if (TimingSystem.configuration.isMedalsShowEveryone()) {
                Medals medal = getTrack().getTrackMedals().getMedal(timeTrialTime);
                if (medal == Medals.EMERALD_CUP || medal == Medals.NETHERITE_CUP) {
                    Component text = Component.text("§7" + player.getName() + " unlocked " + medal.getColor() + "§l" + medal.getName() + "§r§7 on " + track.getDisplayName() + "!");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p != player) {
                            p.sendMessage(text);
                        }
                    }
                }
            }
        }
    }

    private TimeTrialFinish newBestFinish(Player p, long mapTime, long oldTime) {
        var finish = callTimeTrialFinishEvent(p, mapTime, oldTime, true);
        this.bestFinish = track.getTimeTrials().getBestFinish(tPlayer);
        if (tPlayer.getSettings().isSound()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 1);
        }
        LeaderboardManager.updateFastestTimeLeaderboard(track);

        return finish;
    }

    private TimeTrialFinish callTimeTrialFinishEvent(Player player, long time, long oldBestTime, boolean newBestFinish) {
        var finish = track.getTimeTrials().newFinish(time, player.getUniqueId());
        Map<Integer, Long> checkpointTimes = new HashMap<>();
        for(int i = 1; i <= checkpoints.size(); i++) {
            checkpointTimes.put(i, getCheckpointTime(i));
        }
        if (newBestFinish) {
            //only store checkpoints on personal best
            finish.insertCheckpoints(checkpointTimes);
        } else {
            finish.updateCheckpointTimes(checkpointTimes);
        }

        TimeTrialFinishEvent eventTimeTrialFinish = new TimeTrialFinishEvent(player, this, finish, oldBestTime, newBestFinish);
        Bukkit.getServer().getPluginManager().callEvent(eventTimeTrialFinish);
        return finish;
    }

    public Component getBestLapDelta(Theme theme, int latestCheckpoint) {
        if (latestCheckpoint > 0) {
            if (getBestFinish() != null && getBestFinish().hasCheckpointTimes() && getBestFinish().getCheckpointTime(latestCheckpoint) != null) {
                if (getBestFinish().getDate() > getTrack().getDateChanged()) {
                    var bestCheckpoint = getBestFinish().getCheckpointTime(latestCheckpoint);
                    var currentCheckpoint = getCheckpointTime(latestCheckpoint);
                    if (bestCheckpoint < currentCheckpoint) {
                        return Component.text(" +" + ApiUtilities.formatAsPersonalGap(currentCheckpoint - bestCheckpoint)).color(theme.getError());
                    } else if (bestCheckpoint == currentCheckpoint) {
                        return Component.text(" =" + ApiUtilities.formatAsPersonalGap(currentCheckpoint - bestCheckpoint)).color(theme.getWarning());
                    } else {
                        return Component.text(" -" + ApiUtilities.formatAsPersonalGap(bestCheckpoint - currentCheckpoint)).color(theme.getSuccess());
                    }
                }
            }
        }
        return Component.empty();
    }

    private static double calculateRegionEntryProportion(Location from, Location to, TrackRegion region) {
        double low = 0.0;
        double high = 1.0;

        for (int i = 0; i < 15; i++) {
            double mid = (low + high) / 2.0;
            Location midLocation = interpolateLocation(from, to, mid);

            if (region.contains(midLocation)) {
                high = mid;
            } else {
                low = mid;
            }
        }

        return (low + high) / 2.0;
    }

    private static Location interpolateLocation(Location from, Location to, double proportion) {
        double x = from.getX() + (to.getX() - from.getX()) * proportion;
        double y = from.getY() + (to.getY() - from.getY()) * proportion;
        double z = from.getZ() + (to.getZ() - from.getZ()) * proportion;

        return new Location(from.getWorld(), x, y, z);
    }
}
