package me.makkuusen.timing.system.boat;

public class BoatSpawnManager {

	private static BoatSpawner boatSpawner = new DefaultBoatSpawner();
	
	public static void setBoatSpawner(BoatSpawner alternateBoatSpawner) {
		boatSpawner = alternateBoatSpawner;
	}
	
	public static BoatSpawner getBoatSpawner() {
		return boatSpawner;
	}
	
}
