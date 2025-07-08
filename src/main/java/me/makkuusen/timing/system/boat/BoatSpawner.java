package me.makkuusen.timing.system.boat;

import org.bukkit.Location;
import org.bukkit.entity.Boat;

public interface BoatSpawner {

	public Boat spawnBoat(Location location, String woodType, Boolean chestBoat);

}