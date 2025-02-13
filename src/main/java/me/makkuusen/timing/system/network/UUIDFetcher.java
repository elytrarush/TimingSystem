package me.makkuusen.timing.system.network;

import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Bukkit;
import org.shanerx.mojang.Mojang;

import java.util.Optional;
import java.util.UUID;

public class UUIDFetcher {

    private static Mojang api;

    private static void init() {
        api = new Mojang().connect();
    }

    /**
     * Contact the Mojang API and run a callback with the username and UUID for the given input.
     * Keep in mind that the Mojang API is heavily rate limited to 600 requests per 10 minutes.
     * @param mcUsernameOrUuid
     * @param callback
     */
    @SuppressWarnings("deprecation")
    public static void getAsyncFromMojangAndRunCallback(final String mcUsernameOrUuid, final UUIDFetcherCallback callback) {
        Bukkit.getScheduler().scheduleAsyncDelayedTask(TimingSystem.getPlugin(), new Runnable() {

            public void run() {
                Optional<UUID> playerUuid;
                Optional<String> lastName;

                if (mcUsernameOrUuid.length() <= 16) {
                    // Probably username
                    final Optional<UUID> possibleUuid = UUIDFetcher.getUuid(mcUsernameOrUuid);

                    if (possibleUuid.isPresent()) {
                        // Actual player with real uuid
                        Bukkit.getScheduler().scheduleSyncDelayedTask(TimingSystem.getPlugin(), new Runnable() {
                            public void run() {
                                final Optional<String> finalLastName = Optional.of(mcUsernameOrUuid);
                                callback.runSync(possibleUuid, finalLastName, mcUsernameOrUuid);
                            }
                        });
                        return;
                    } else {
                        // Not actual player
                        Bukkit.getScheduler().scheduleSyncDelayedTask(TimingSystem.getPlugin(), new Runnable() {
                            public void run() {
                                Optional<UUID> emptyUuid = Optional.empty();
                                Optional<String> emptyLastName = Optional.empty();
                                callback.runSync(emptyUuid, emptyLastName, mcUsernameOrUuid);
                            }
                        });
                        return;
                    }
                } else {
                    // Probably UUID
                    try {
                        playerUuid = Optional.of(UUID.fromString(mcUsernameOrUuid));
                        if (playerUuid.isPresent()) {
                            lastName = getLastName(playerUuid.get());
                        } else {
                            lastName = Optional.empty();
                        }

                        final Optional<UUID> finalUuid = playerUuid;
                        final Optional<String> finalLastName = lastName;

                        Bukkit.getScheduler().scheduleSyncDelayedTask(TimingSystem.getPlugin(), new Runnable() {
                            public void run() {
                                callback.runSync(finalUuid, finalLastName, mcUsernameOrUuid);
                            }
                        });
                        return;
                    } catch (IllegalArgumentException e) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(TimingSystem.getPlugin(), new Runnable() {
                            public void run() {
                                Optional<UUID> emptyUuid = Optional.empty();
                                Optional<String> emptyLastName = Optional.empty();
                                callback.runSync(emptyUuid, emptyLastName, mcUsernameOrUuid);
                            }
                        });
                        return;
                    }
                }

            }
        });
    }

    /**
     * This method will contact the mojang api to get the last name for this player.
     * @param uuid
     * @return An Optional<String> containing the last playername for the uuid. The optional will be empty if mojang doesn't know it.
     */
    private static Optional<String> getLastName(UUID uuid) {
        if (api == null) {
            init();
        }

        String mojangPlayerName;
        try {
            mojangPlayerName = api.getPlayerProfile(uuid.toString()).getUsername();
            TimingSystem.getPlugin().getLogger().info("[UUIDFetcher] Got LastName of '" + mojangPlayerName + "' for UUID '" + uuid.toString() + "' from Mojang.");
        } catch (Exception e) {
            return Optional.empty();
        }

        return Optional.of(mojangPlayerName);
    }

    /**
     * This method will attempt to get the UUID from cache first.
     * If the UUID is not cached, it will contact the Mojang API.
     * @param playerUserName
     * @return
     */
    private static Optional<UUID> getUuid(String playerUserName) {
        if (api == null) {
            init();
        }

        String mojangUuid;
        try {
            mojangUuid = api.getUUIDOfUsername(playerUserName);
        } catch (Exception e) {
            return Optional.empty();
        }

        mojangUuid = nonDashedToDashedUuid(mojangUuid);
        TimingSystem.getPlugin().getLogger().info("[UUIDFetcher] Got UUID of '" + mojangUuid + "' for player name '" + playerUserName + "' from Mojang.");

        return Optional.of(UUID.fromString(mojangUuid));
    }

    private static String nonDashedToDashedUuid(String nonDashedUuid) {
        StringBuilder sb = new StringBuilder(nonDashedUuid);
        sb.insert(8, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(13, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(18, "-");
        sb = new StringBuilder(sb.toString());
        sb.insert(23, "-");

        return sb.toString();
    }

}
