package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Default;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.network.discord.DiscordNotifier;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.timetrial.TimeTrialFinish;
import me.makkuusen.timing.system.track.Track;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("report")
public class CommandReport extends BaseCommand {

    @Default
    @CommandCompletion("@ts_players @track")
    public static void onReport(Player reporter, String targetName, Track track) {
        if (reporter == null) {
            return;
        }
        if (targetName == null || targetName.isBlank() || track == null) {
            Text.send(reporter, Error.SYNTAX);
            return;
        }

        TPlayer target = TSDatabase.getPlayer(targetName);
        if (target == null) {
            Text.send(reporter, Error.PLAYER_NOT_FOUND);
            return;
        }

        TimeTrialFinish finish = track.getTimeTrials().getBestFinish(target);
        String timeStr = finish == null ? "None" : ApiUtilities.formatAsTime(finish.getTime());

        List<TimeTrialFinish> top3 = track.getTimeTrials().getTopList(3);
        StringBuilder top3Str = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (i < top3.size()) {
                TimeTrialFinish f = top3.get(i);
                top3Str.append(i + 1)
                    .append(". ")
                    .append(f.getPlayer().getName())
                    .append(" — ")
                    .append(ApiUtilities.formatAsTime(f.getTime()));
            } else {
                top3Str.append(i + 1).append(". Empty");
            }
            if (i < 2) top3Str.append("\n");
        }

        String replayCommand = "/replay play timingsystem-" + track.getId() + "-" + target.getUniqueId();

        String threadName = target.getName() + " (" + track.getDisplayName() + " - " + timeStr + ")";
        if (threadName.length() > 100) {
            threadName = threadName.substring(0, 100);
        }

        String content = "**Report**\n"
            + "Player: **" + target.getName() + "**\n"
            + "Map: **" + track.getDisplayName() + "**\n"
            + "Time: **" + timeStr + "**\n"
            + "Top 3:\n" + top3Str + "\n"
            + "Replay: `" + replayCommand + "`";

        DiscordNotifier.sendReport(content, threadName);
        Text.send(reporter, Success.SAVED);
    }
}
