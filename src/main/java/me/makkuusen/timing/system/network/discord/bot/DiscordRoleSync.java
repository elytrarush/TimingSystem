package me.makkuusen.timing.system.network.discord.bot;

import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.leaderboard.GlobalPointsLeaderboard;
import me.makkuusen.timing.system.tplayer.TPlayer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.*;

final class DiscordRoleSync {

    private final TimingSystem plugin;

    private final long guildId;
    private final Long top10RoleId;
    private final Map<Integer, Long> rankRoleIds;

    DiscordRoleSync(TimingSystem plugin, long guildId, Long top10RoleId, Map<Integer, Long> rankRoleIds) {
        this.plugin = plugin;
        this.guildId = guildId;
        this.top10RoleId = top10RoleId;
        this.rankRoleIds = Map.copyOf(rankRoleIds);
    }

    void syncAllLinked(JDA jda, DiscordLinkService links) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        List<Map.Entry<TPlayer, Double>> top = GlobalPointsLeaderboard.computeTop(10);
        Map<UUID, Integer> rankByUuid = new HashMap<>();
        int rank = 1;
        for (var e : top) {
            if (e.getKey() == null || e.getKey().getUniqueId() == null) continue;
            rankByUuid.put(e.getKey().getUniqueId(), rank);
            rank++;
        }

        // Only touch linked players
        for (var entry : links.snapshotLinks().entrySet()) {
            UUID uuid = entry.getKey();
            long discordId = entry.getValue();
            Integer currentRank = rankByUuid.get(uuid);

            syncMember(guild, discordId, currentRank);
        }
    }

    void removeRankRoles(JDA jda, long discordUserId) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;
        syncMember(guild, discordUserId, null);
    }

    private void syncMember(Guild guild, long discordUserId, Integer rankOrNull) {
        guild.retrieveMemberById(discordUserId).queue(
            member -> applyRoles(guild, member, rankOrNull),
            failure -> {
                // ignore missing member
            }
        );
    }

    private void applyRoles(Guild guild, Member member, Integer rankOrNull) {
        List<Role> add = new ArrayList<>();
        List<Role> remove = new ArrayList<>();

        // Top10 role
        Role top10 = (top10RoleId == null) ? null : guild.getRoleById(top10RoleId);
        boolean shouldHaveTop10 = rankOrNull != null && rankOrNull >= 1 && rankOrNull <= 10;
        if (top10 != null) {
            if (shouldHaveTop10 && !member.getRoles().contains(top10)) add.add(top10);
            if (!shouldHaveTop10 && member.getRoles().contains(top10)) remove.add(top10);
        }

        // Rank #1-10 roles
        for (int r = 1; r <= 10; r++) {
            Long roleId = rankRoleIds.get(r);
            if (roleId == null) continue;
            Role role = guild.getRoleById(roleId);
            if (role == null) continue;

            boolean shouldHave = rankOrNull != null && rankOrNull == r;
            boolean has = member.getRoles().contains(role);

            if (shouldHave && !has) add.add(role);
            if (!shouldHave && has) remove.add(role);
        }

        if (add.isEmpty() && remove.isEmpty()) return;

        guild.modifyMemberRoles(member, add, remove).queue(
            ok -> {},
            err -> plugin.getLogger().warning("Discord role sync failed for " + member.getUser().getAsTag() + ": " + err.getMessage())
        );
    }

}
