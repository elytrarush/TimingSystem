package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import me.makkuusen.timing.system.loneliness.GhostingController;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Warning;
import org.bukkit.entity.Player;

@CommandAlias("ghost|g")
public class CommandGhost extends BaseCommand {
    @Default
    @CommandCompletion("@players")
    @CommandPermission("%permissiontimingsystem_ghost")
    public static void onGhost(Player player, String targetName) {
        Player target = player.getServer().getPlayer(targetName);
        GhostingController.toggleGhosted(target);
        Text.send(player, GhostingController.isGhosted(target) ? Success.GHOSTING_ON : Success.GHOSTING_OFF);
        Text.send(target, GhostingController.isGhosted(target) ? Warning.GHOSTING_TARGET_ON : Warning.GHOSTING_TARGET_OFF);
    }
}
