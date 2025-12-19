package me.makkuusen.timing.system.network.discord.bot;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class DiscordLinkService {

    private static final String FILE_NAME = "discord-links.yml";

    private final SecureRandom random = new SecureRandom();

    private final File file;

    // Persistent
    private final ConcurrentHashMap<UUID, Long> discordIdByUuid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, UUID> uuidByDiscordId = new ConcurrentHashMap<>();

    // Ephemeral (not persisted)
    private final ConcurrentHashMap<String, PendingLink> pendingByCode = new ConcurrentHashMap<>();

    DiscordLinkService(File dataFolder) {
        this.file = new File(dataFolder, FILE_NAME);
    }

    void load() {
        discordIdByUuid.clear();
        uuidByDiscordId.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        var section = cfg.getConfigurationSection("links");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String raw = section.getString(key);
                if (raw == null || raw.isBlank()) continue;
                long discordId = Long.parseUnsignedLong(raw);
                discordIdByUuid.put(uuid, discordId);
                uuidByDiscordId.put(discordId, uuid);
            } catch (Exception ignored) {
            }
        }
    }

    void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Long> e : discordIdByUuid.entrySet()) {
            cfg.set("links." + e.getKey(), Long.toUnsignedString(e.getValue()));
        }
        try {
            cfg.save(file);
        } catch (IOException ignored) {
        }
    }

    Optional<Long> getDiscordId(UUID uuid) {
        if (uuid == null) return Optional.empty();
        return Optional.ofNullable(discordIdByUuid.get(uuid));
    }

    Optional<UUID> getUuid(long discordId) {
        return Optional.ofNullable(uuidByDiscordId.get(discordId));
    }

    Map<UUID, Long> snapshotLinks() {
        return Map.copyOf(discordIdByUuid);
    }

    /**
     * Generates a one-time code for the given Minecraft UUID.
     */
    String createLinkCode(UUID uuid, int length, long expiresInMillis) {
        long expiresAt = Instant.now().toEpochMilli() + Math.max(10_000L, expiresInMillis);

        int safeLen = Math.max(4, Math.min(16, length));
        String code;
        do {
            code = randomCode(safeLen);
        } while (pendingByCode.containsKey(code));

        pendingByCode.put(code, new PendingLink(uuid, expiresAt));
        return code;
    }

    LinkResult tryLink(String codeRaw, long discordUserId) {
        if (codeRaw == null) return LinkResult.invalid("Missing code.");
        String code = codeRaw.trim().toUpperCase(Locale.ROOT);
        PendingLink pending = pendingByCode.remove(code);
        if (pending == null) return LinkResult.invalid("Invalid or expired code.");
        if (Instant.now().toEpochMilli() > pending.expiresAtMillis()) return LinkResult.invalid("Invalid or expired code.");

        UUID uuid = pending.uuid();
        if (uuid == null) return LinkResult.invalid("Invalid code.");

        // Prevent a Discord account linking to multiple MC accounts
        UUID existingUuid = uuidByDiscordId.get(discordUserId);
        if (existingUuid != null && !existingUuid.equals(uuid)) {
            return LinkResult.invalid("This Discord account is already linked.");
        }

        // Link / relink
        Long previousDiscord = discordIdByUuid.put(uuid, discordUserId);
        uuidByDiscordId.put(discordUserId, uuid);
        if (previousDiscord != null && previousDiscord != discordUserId) {
            uuidByDiscordId.remove(previousDiscord, uuid);
        }

        save();
        return LinkResult.linked(uuid);
    }

    boolean unlinkByUuid(UUID uuid) {
        if (uuid == null) return false;
        Long discordId = discordIdByUuid.remove(uuid);
        if (discordId == null) return false;
        uuidByDiscordId.remove(discordId, uuid);
        save();
        return true;
    }

    boolean unlinkByDiscordId(long discordId) {
        UUID uuid = uuidByDiscordId.remove(discordId);
        if (uuid == null) return false;
        discordIdByUuid.remove(uuid, discordId);
        save();
        return true;
    }

    private String randomCode(int length) {
        // A-Z0-9 (no ambiguous chars removal for now)
        final char[] alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = alphabet[random.nextInt(alphabet.length)];
        }
        return new String(out);
    }

    private record PendingLink(UUID uuid, long expiresAtMillis) {}

    sealed interface LinkResult {
        static LinkResult linked(UUID uuid) { return new Linked(uuid); }
        static LinkResult invalid(String message) { return new Invalid(message); }

        record Linked(UUID uuid) implements LinkResult {}
        record Invalid(String message) implements LinkResult {}
    }
}
