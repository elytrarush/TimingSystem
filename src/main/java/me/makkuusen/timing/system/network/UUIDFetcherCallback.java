package me.makkuusen.timing.system.network;

import java.util.Optional;
import java.util.UUID;

public interface UUIDFetcherCallback {

    void runSync(Optional<UUID> uuid, Optional<String> lastName, String originalMcUsernameOrUuid);
}
