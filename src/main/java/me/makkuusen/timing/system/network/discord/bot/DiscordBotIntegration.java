package me.makkuusen.timing.system.network.discord.bot;

import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.TimingSystem;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional Discord bot integration:
 * - Players link accounts via /ts discord link (MC) + /link <code> (Discord slash command)
 * - Sync Top10 + Rank #1..#10 roles based on global points leaderboard.
 */
public final class DiscordBotIntegration {

    private static TimingSystem plugin;

    private static volatile boolean enabled;
    private static volatile JDA jda;

    private static DiscordLinkService links;
    private static DiscordRoleSync roleSync;

    private static volatile long configuredGuildId;

    private static BukkitTask syncTask;

    // Activity threads
    private static volatile boolean activityThreadsEnabled;
    private static volatile long activityChannelId;
    private static boolean updateTopic;
    private static boolean postFinishes;
    private static boolean postPersonalBests;
    private static String joinMessageTemplate;
    private static String leaveMessageTemplate;
    private static String topicFormatTemplate;
    private static String finishMessageTemplate;
    private static String personalBestMessageTemplate;
    private static final ConcurrentHashMap<UUID, Long> activeThreads = new ConcurrentHashMap<>();

    private DiscordBotIntegration() {}

    public static void enable(TimingSystem plugin) {
        DiscordBotIntegration.plugin = plugin;

        boolean botEnabled = plugin.getConfig().getBoolean("discord.bot.enabled", false);
        String token = plugin.getConfig().getString("discord.bot.token", "");
        String guildIdRaw = plugin.getConfig().getString("discord.bot.guildId", "");

        if (!botEnabled || token == null || token.isBlank() || guildIdRaw == null || guildIdRaw.isBlank()) {
            enabled = false;
            return;
        }

        long guildId;
        try {
            guildId = Long.parseUnsignedLong(guildIdRaw.trim());
        } catch (Exception e) {
            plugin.getLogger().warning("discord.bot.guildId is not a valid number; discord bot disabled.");
            enabled = false;
            return;
        }

        configuredGuildId = guildId;

        boolean rankSyncEnabled = plugin.getConfig().getBoolean("discord.bot.rankSync.enabled", true);
        long rankSyncIntervalMs = getDurationMs("discord.bot.rankSync.interval", "15m");

        Long top10RoleId = parseOptionalRoleId(plugin.getConfig().getString("discord.bot.roles.top10", ""));
        Map<Integer, Long> rankRoleIds = new HashMap<>();
        for (int r = 1; r <= 10; r++) {
            Long roleId = parseOptionalRoleId(plugin.getConfig().getString("discord.bot.roles.rank" + r, ""));
            if (roleId != null) rankRoleIds.put(r, roleId);
        }

        links = new DiscordLinkService(plugin.getDataFolder());
        links.load();
        roleSync = new DiscordRoleSync(plugin, guildId, top10RoleId, rankRoleIds);

        try {
            jda = JDABuilder.createDefault(token)
                .addEventListeners(new SlashListener())
                .build();
            enabled = true;
        } catch (Exception e) {
            enabled = false;
            plugin.getLogger().warning("Failed to start Discord bot: " + e.getMessage());
            return;
        }

        if (rankSyncEnabled) {
            long ticks = Math.max(20L, rankSyncIntervalMs / 50L);
            syncTask = Bukkit.getScheduler().runTaskTimer(plugin, DiscordBotIntegration::syncNow, 20L * 30L, ticks);
        }

        // Activity threads config
        activityThreadsEnabled = plugin.getConfig().getBoolean("discord.bot.activityThreads.enabled", false);
        String channelIdRaw = plugin.getConfig().getString("discord.bot.activityThreads.channelId", "");
        if (activityThreadsEnabled && channelIdRaw != null && !channelIdRaw.isBlank()) {
            try {
                activityChannelId = Long.parseUnsignedLong(channelIdRaw.trim());
            } catch (Exception e) {
                plugin.getLogger().warning("discord.bot.activityThreads.channelId is not valid; activity threads disabled.");
                activityThreadsEnabled = false;
            }
        } else {
            activityThreadsEnabled = false;
        }
        updateTopic = plugin.getConfig().getBoolean("discord.bot.activityThreads.updateTopic", true);
        postFinishes = plugin.getConfig().getBoolean("discord.bot.activityThreads.postFinishes", true);
        postPersonalBests = plugin.getConfig().getBoolean("discord.bot.activityThreads.postPersonalBests", true);
        joinMessageTemplate = plugin.getConfig().getString("discord.bot.activityThreads.joinMessage",
            ":green_circle: **{player}** joined the server");
        leaveMessageTemplate = plugin.getConfig().getString("discord.bot.activityThreads.leaveMessage",
            ":red_circle: **{player}** left the server");
        topicFormatTemplate = plugin.getConfig().getString("discord.bot.activityThreads.topicFormat",
            "Online Players: {count}");
        finishMessageTemplate = plugin.getConfig().getString("discord.bot.activityThreads.finishMessage",
            ":checkered_flag: Finished **{track}** in **{time}**");
        personalBestMessageTemplate = plugin.getConfig().getString("discord.bot.activityThreads.personalBestMessage",
            ":star: New PB on **{track}**: **{time}** ({delta})");
    }

    public static void disable() {
        enabled = false;
        configuredGuildId = 0L;

        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }

        activeThreads.clear();
        activityThreadsEnabled = false;
        activityChannelId = 0L;

        if (jda != null) {
            try {
                jda.shutdownNow();
            } catch (Exception ignored) {
            }
            jda = null;
        }

        links = null;
        roleSync = null;
        plugin = null;
    }

    public static boolean isEnabled() {
        return enabled && jda != null;
    }

    public static String createLinkCode(UUID uuid) {
        if (!isEnabled() || links == null) return null;
        long linkExpiresMs = getDurationMs("discord.bot.linkCode.expires", "10m");
        int linkLen = Math.max(4, Math.min(16, plugin.getConfig().getInt("discord.bot.linkCode.length", 6)));
        return links.createLinkCode(uuid, linkLen, linkExpiresMs);
    }

    public static Long getLinkedDiscordId(UUID uuid) {
        if (!isEnabled() || links == null) return null;
        return links.getDiscordId(uuid).orElse(null);
    }

    public static boolean unlink(UUID uuid) {
        if (!isEnabled() || links == null) return false;
        Long discordId = links.getDiscordId(uuid).orElse(null);
        boolean unlinked = links.unlinkByUuid(uuid);
        if (unlinked && discordId != null) {
            removeRankRoles(discordId);
        }
        return unlinked;
    }

    public static void syncNow() {
        if (!isEnabled() || roleSync == null || links == null) return;
        try {
            roleSync.syncAllLinked(jda, links);
        } catch (Exception e) {
            if (plugin != null) plugin.getLogger().warning("Discord rank sync failed: " + e.getMessage());
        }
    }

    // ── Activity threads ──────────────────────────────────────────────

    public static void onPlayerJoin(String playerName, UUID playerUuid, int onlineCount) {
        if (!isEnabled() || !activityThreadsEnabled) return;
        JDA j = jda;
        if (j == null) return;

        TextChannel channel = j.getTextChannelById(activityChannelId);
        if (channel == null) {
            logActivityWarning("channel not found (" + activityChannelId + ")");
            return;
        }

        if (updateTopic) {
            String topic = topicFormatTemplate.replace("{count}", String.valueOf(onlineCount));
            channel.getManager().setTopic(topic).queue(ok -> {}, err -> {});
        }

        String joinMsg = joinMessageTemplate.replace("{player}", playerName);
        channel.createThreadChannel(playerName, false).queue(thread -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (Bukkit.getPlayer(playerUuid) == null) {
                    // Player already left before thread was created
                    String leaveMsg = leaveMessageTemplate.replace("{player}", playerName);
                    thread.sendMessage(joinMsg).queue(
                        ok -> thread.sendMessage(leaveMsg).queue(
                            ok2 -> thread.getManager().setArchived(true).queue(ok3 -> {}, err -> {}),
                            err -> thread.getManager().setArchived(true).queue(ok3 -> {}, err2 -> {})
                        ),
                        err -> thread.getManager().setArchived(true).queue(ok3 -> {}, err2 -> {})
                    );
                    return;
                }
                activeThreads.put(playerUuid, thread.getIdLong());
                thread.sendMessage(joinMsg).queue(ok -> {}, err -> {});
            });
        }, err -> logActivityWarning("failed to create thread for " + playerName + ": " + err.getMessage()));
    }

    public static void onPlayerLeave(String playerName, UUID playerUuid, int onlineCount) {
        if (!isEnabled() || !activityThreadsEnabled) return;
        JDA j = jda;
        if (j == null) return;

        if (updateTopic) {
            TextChannel channel = j.getTextChannelById(activityChannelId);
            if (channel != null) {
                String topic = topicFormatTemplate.replace("{count}", String.valueOf(onlineCount));
                channel.getManager().setTopic(topic).queue(ok -> {}, err -> {});
            }
        }

        Long threadId = activeThreads.remove(playerUuid);
        if (threadId == null) return;

        ThreadChannel thread = j.getThreadChannelById(threadId);
        if (thread == null) return;

        String msg = leaveMessageTemplate.replace("{player}", playerName);
        thread.sendMessage(msg).queue(
            ok -> thread.getManager().setArchived(true).queue(ok2 -> {}, err -> {}),
            err -> thread.getManager().setArchived(true).queue(ok2 -> {}, err2 -> {})
        );
    }

    public static void onMapFinish(UUID playerUuid, String trackName, String time, boolean isPersonalBest, String delta) {
        if (!isEnabled() || !activityThreadsEnabled) return;
        if (!postFinishes && !(isPersonalBest && postPersonalBests)) return;
        JDA j = jda;
        if (j == null) return;

        Long threadId = activeThreads.get(playerUuid);
        if (threadId == null) return;

        ThreadChannel thread = j.getThreadChannelById(threadId);
        if (thread == null) return;

        String template = (isPersonalBest && postPersonalBests) ? personalBestMessageTemplate : finishMessageTemplate;
        String msg = template
            .replace("{track}", trackName)
            .replace("{time}", time)
            .replace("{delta}", delta == null ? "N/A" : delta);

        thread.sendMessage(msg).queue(ok -> {}, err -> {});
    }

    private static void logActivityWarning(String message) {
        if (plugin != null) plugin.getLogger().warning("Discord activity threads: " + message);
    }

    private static void removeRankRoles(long discordUserId) {
        if (!isEnabled() || roleSync == null) return;
        try {
            roleSync.removeRankRoles(jda, discordUserId);
        } catch (Exception ignored) {
        }
    }

    private static long getDurationMs(String path, String def) {
        String raw = plugin.getConfig().getString(path, def);
        Integer ms = raw == null ? null : ApiUtilities.parseDurationToMillis(raw);
        if (ms == null) ms = ApiUtilities.parseDurationToMillis(def);
        return ms == null ? 900_000L : ms.longValue();
    }

    private static Long parseOptionalRoleId(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        try {
            return Long.parseUnsignedLong(t);
        } catch (Exception e) {
            return null;
        }
    }

    private static final class SlashListener extends ListenerAdapter {

        @Override
        public void onReady(ReadyEvent event) {
            if (configuredGuildId == 0L) return;

            Guild guild = event.getJDA().getGuildById(configuredGuildId);
            if (guild == null) {
                if (plugin != null) plugin.getLogger().warning("Discord bot: guildId not found (is the bot in the server?).");
                return;
            }

            guild.updateCommands().addCommands(
                Commands.slash("link", "Link your Minecraft account").addOption(OptionType.STRING, "code", "One-time code from /ts discord link", true),
                Commands.slash("unlink", "Unlink your Minecraft account")
            ).queue(
                ok -> {},
                err -> {
                    if (plugin != null) plugin.getLogger().warning("Discord bot: failed to register slash commands: " + err.getMessage());
                }
            );
        }

        @Override
        public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
            if (event.getUser().isBot()) return;
            if (links == null) return;

            if (configuredGuildId != 0L && (event.getGuild() == null || event.getGuild().getIdLong() != configuredGuildId)) {
                event.reply("This command can only be used in the configured server.").setEphemeral(true).queue();
                return;
            }

            switch (event.getName()) {
                case "link" -> {
                    String code = event.getOption("code") == null ? null : event.getOption("code").getAsString();
                    var result = links.tryLink(code, event.getUser().getIdLong());
                    if (result instanceof DiscordLinkService.LinkResult.Linked linked) {
                        UUID uuid = linked.uuid();
                        event.reply("Linked successfully.").setEphemeral(true).queue();

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) p.sendMessage("§aDiscord linked successfully.");
                        });

                        // Sync roles immediately
                        Bukkit.getScheduler().runTask(plugin, DiscordBotIntegration::syncNow);
                        return;
                    }
                    if (result instanceof DiscordLinkService.LinkResult.Invalid invalid) {
                        event.reply(invalid.message()).setEphemeral(true).queue();
                        return;
                    }
                }
                case "unlink" -> {
                    boolean ok = links.unlinkByDiscordId(event.getUser().getIdLong());
                    if (ok) {
                        event.reply("Unlinked successfully.").setEphemeral(true).queue();
                        Bukkit.getScheduler().runTask(plugin, () -> removeRankRoles(event.getUser().getIdLong()));
                    } else {
                        event.reply("No linked Minecraft account found.").setEphemeral(true).queue();
                    }
                    return;
                }
                default -> {
                }
            }
        }
    }
}
