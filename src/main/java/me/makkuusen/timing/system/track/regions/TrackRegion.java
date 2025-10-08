package me.makkuusen.timing.system.track.regions;

import co.aikar.idb.DbRow;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Location;

@Getter
@Setter
public abstract class TrackRegion {


    @Getter
    private final int id;
    @Getter
    private final int trackId;
    @Getter
    private final int regionIndex;
    @Getter
    private final RegionType regionType;
    private RegionShape shape;
    @Getter
    private Location spawnLocation;
    private Location minP;
    private Location maxP;
    @Getter
    private int rocketReward = 0;
    private boolean highlightEnabled = false;

    public TrackRegion(DbRow data) {
        id = data.getInt("id");
        trackId = data.getInt("trackId");
        regionIndex = data.getInt("regionIndex");
        regionType = data.getString("regionType") == null ? null : TrackRegion.RegionType.valueOf(data.getString("regionType"));
        spawnLocation = ApiUtilities.stringToLocation(data.getString("spawn"));
        minP = ApiUtilities.stringToLocation(data.getString("minP"));
        maxP = ApiUtilities.stringToLocation(data.getString("maxP"));
        try {
            rocketReward = data.getInt("rocketReward");
        } catch (Exception ignored) {
            rocketReward = 0;
        }

        Object highlight = data.get("highlight");
        if (highlight instanceof Boolean bool) {
            highlightEnabled = bool;
        } else if (highlight instanceof Number number) {
            highlightEnabled = number.intValue() != 0;
        } else if (highlight instanceof String string) {
            highlightEnabled = string.equalsIgnoreCase("true") || string.equals("1");
        }
    }

    public TrackRegion(long id, long trackId, int regionIndex, RegionType regionType, Location spawnLocation, Location minP, Location maxP) {
        this.id = (int) id;
        this.trackId = (int) trackId;
        this.regionIndex = regionIndex;
        this.regionType = regionType;
        this.spawnLocation = spawnLocation;
        this.minP = minP;
        this.maxP = maxP;
    }

    public abstract boolean contains(Location loc);

    public abstract boolean isDefined();

    public String getWorldName() {
        if (!spawnLocation.isWorldLoaded()) {
            return "Unknown";
        }
        return spawnLocation.getWorld().getName();
    }

    public void setMinP(Location minP) {
        this.minP = minP;
        TimingSystem.getTrackDatabase().trackRegionSet(id, "minP", ApiUtilities.locationToString(minP));
    }

    public void setMaxP(Location maxP) {
        this.maxP = maxP;
        TimingSystem.getTrackDatabase().trackRegionSet(id, "maxP", ApiUtilities.locationToString(maxP));
    }

    public void setSpawn(Location spawn) {
        this.spawnLocation = spawn;
        TimingSystem.getTrackDatabase().trackRegionSet(id, "spawn", ApiUtilities.locationToString(spawn));
    }

    public void setRocketReward(int amount) {
        if (amount < 0) amount = 0;
        this.rocketReward = amount;
        TimingSystem.getTrackDatabase().trackRegionSet(id, "rocketReward", amount);
    }

    public boolean isHighlightEnabled() {
        return highlightEnabled;
    }

    public void setHighlightEnabled(boolean highlightEnabled) {
        this.highlightEnabled = highlightEnabled;
        TimingSystem.getTrackDatabase().trackRegionSet(id, "highlight", highlightEnabled ? 1 : 0);
        RocketAreaHighlighter.refresh(this);
    }

    abstract boolean hasEqualBounds(TrackRegion other);

    public enum RegionType {
        START, END, PIT, CHECKPOINT, RESET, INPIT, LAGSTART, LAGEND, ROCKET_AREA
    }

    public enum RegionShape {
        POLY, CUBOID
    }

}
