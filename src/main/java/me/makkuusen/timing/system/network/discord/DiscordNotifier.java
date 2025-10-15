package me.makkuusen.timing.system.network.discord;

import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Discord webhook sender
 */
public final class DiscordNotifier {
    private static boolean enabled;
    private static String webhookUrl;
    private static String recordMessageTemplate;

    private static final HttpClient http = HttpClient.newHttpClient();

    private DiscordNotifier() {}

    public static void enable(TimingSystem plugin) {
        enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        webhookUrl = plugin.getConfig().getString("discord.webhookUrl", "");
        recordMessageTemplate = plugin.getConfig().getString("discord.message", ":trophy: New record on {track}! {player} — {time} (−{delta})");

        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            enabled = false;
            return;
        }
    }

    public static void disable() {
        enabled = false;
        webhookUrl = null;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void sendNewRecord(String track, String player, String time, String delta) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) return;

        String content = recordMessageTemplate
            .replace("{track}", track)
            .replace("{player}", player)
            .replace("{time}", time)
            .replace("{delta}", delta == null ? "N/A" : delta);

        // Prepare JSON payload
        String json = "{\"content\": " + quote(content) + "}";

        // Run asynchronously to avoid blocking the server thread
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
                http.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                // Keep failures silent except a single log line
                var logger = TimingSystem.getPlugin() != null ? TimingSystem.getPlugin().getLogger() : null;
                if (logger != null) logger.warning("Discord webhook post failed: " + e.getMessage());
            }
        });
    }

    private static String quote(String s) {
        // Very small JSON string escaper
        String escaped = s
            .replace("\\", "\\\\")
            .replace("\u001b", "\\u001b")
            .replace("\"", "\\\"")
            .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }
}
