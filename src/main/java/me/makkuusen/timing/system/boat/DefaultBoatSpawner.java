package me.makkuusen.timing.system.boat;

import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;

public class DefaultBoatSpawner implements BoatSpawner {

	@Override
	public Boat spawnBoat(Location location) {
		Boat boat;
		boat = (Boat) location.getWorld().spawnEntity(location, EntityType.BOAT);
		return boat;
	}

	@Override
	public Boat spawnChestBoat(Location location) {
		Boat boat;
		boat = (Boat) location.getWorld().spawnEntity(location, EntityType.CHEST_BOAT);
		return boat;
	}

}
