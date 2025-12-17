package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.Track;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("replayts|rts")
public class CommandReplayTs extends BaseCommand {

    @Default
    @CommandCompletion("@track @ts_players")
    public static void onReplayTs(Player viewer, Track track, String targetName) {
        if (viewer == null) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("AdvancedReplay")) {
            viewer.sendMessage(Component.text("AdvancedReplay is not installed."));
            return;
        }
        if (track == null || targetName == null || targetName.isBlank()) {
            viewer.sendMessage(Component.text("Usage: /replayts <map> <player>"));
            return;
        }

        TPlayer target = TSDatabase.getPlayer(targetName);
        if (target == null) {
            viewer.sendMessage(Component.text("Player not found."));
            return;
        }

        String replayId = "timingsystem-" + track.getId() + "-" + target.getUniqueId();
        viewer.performCommand("replay play " + replayId);
    }
}
