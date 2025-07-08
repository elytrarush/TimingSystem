package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.loneliness.LonelinessController;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.entity.Player;

@CommandAlias("ghost|gh")
public class CommandGhost extends BaseCommand {
    @Default
    @CommandCompletion("@players")
    @CommandPermission("%permissiontimingsystem_ghost")
    public static void onGhost(Player player, String targetName) {
        Player target = player.getServer().getPlayer(targetName);
        TPlayer tPlayer = TSDatabase.getPlayer(target.getUniqueId());
        Boolean isGhosted = LonelinessController.isGhosted(target.getUniqueId());
        LonelinessController.ghost(tPlayer, !isGhosted);

        Text.send(player, !isGhosted ? Success.GHOSTING_ON : Success.GHOSTING_OFF);
        Text.send(target, !isGhosted ? Warning.GHOSTING_TARGET_ON : Warning.GHOSTING_TARGET_OFF);
    }
}
