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

    // Player join/leave webhook settings
    private static boolean playerJoinLeaveEnabled;
    private static String playerJoinLeaveWebhookUrl;
    private static String playerJoinMessageTemplate;
    private static String playerLeaveMessageTemplate;

    private static final HttpClient http = HttpClient.newHttpClient();

    private DiscordNotifier() {}

    public static void enable(TimingSystem plugin) {
        enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        webhookUrl = plugin.getConfig().getString("discord.webhookUrl", "");
        recordMessageTemplate = plugin.getConfig().getString("discord.message", ":trophy: New record on {track}! {player} — {time} (−{delta})");

        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            enabled = false;
        }

        // Load player join/leave settings
        playerJoinLeaveEnabled = plugin.getConfig().getBoolean("discord.playerJoinLeave.enabled", false);
        playerJoinLeaveWebhookUrl = plugin.getConfig().getString("discord.playerJoinLeave.webhookUrl", "");
        playerJoinMessageTemplate = plugin.getConfig().getString("discord.playerJoinLeave.joinMessage", ":green_circle: **{player}** joined the server");
        playerLeaveMessageTemplate = plugin.getConfig().getString("discord.playerJoinLeave.leaveMessage", ":red_circle: **{player}** left the server");

        if (!playerJoinLeaveEnabled || playerJoinLeaveWebhookUrl == null || playerJoinLeaveWebhookUrl.isBlank()) {
            playerJoinLeaveEnabled = false;
        }
    }

    public static void disable() {
        enabled = false;
        webhookUrl = null;
        playerJoinLeaveEnabled = false;
        playerJoinLeaveWebhookUrl = null;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isPlayerJoinLeaveEnabled() {
        return playerJoinLeaveEnabled;
    }

    public static void sendNewRecord(String track, String player, String time, String delta) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) return;

        String content = recordMessageTemplate
            .replace("{track}", track)
            .replace("{player}", player)
            .replace("{time}", time)
            .replace("{delta}", delta == null ? "N/A" : delta);

        sendWebhookMessage(webhookUrl, content);
    }

    public static void sendPlayerJoin(String player) {
        if (!playerJoinLeaveEnabled || playerJoinLeaveWebhookUrl == null || playerJoinLeaveWebhookUrl.isBlank()) return;

        String content = playerJoinMessageTemplate.replace("{player}", player);
        sendWebhookMessage(playerJoinLeaveWebhookUrl, content);
    }

    public static void sendPlayerLeave(String player) {
        if (!playerJoinLeaveEnabled || playerJoinLeaveWebhookUrl == null || playerJoinLeaveWebhookUrl.isBlank()) return;

        String content = playerLeaveMessageTemplate.replace("{player}", player);
        sendWebhookMessage(playerJoinLeaveWebhookUrl, content);
    }

    private static void sendWebhookMessage(String url, String content) {
        // Prepare JSON payload
        String json = "{\"content\": " + quote(content) + "}";

        // Run asynchronously to avoid blocking the server thread
        Bukkit.getScheduler().runTaskAsynchronously(TimingSystem.getPlugin(), () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
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
