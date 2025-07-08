package me.makkuusen.timing.system.boat;

import org.bukkit.Location;
import org.bukkit.entity.Boat;

public interface BoatSpawner {

	public Boat spawnBoat(Location location);
	
	public Boat spawnChestBoat(Location location);
	
}
