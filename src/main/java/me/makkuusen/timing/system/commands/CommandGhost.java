package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.heat.Heat;
import me.makkuusen.timing.system.loneliness.LonelinessController;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.theme.messages.Warning;
import me.makkuusen.timing.system.tplayer.TPlayer;
import org.bukkit.entity.Player;

import static me.makkuusen.timing.system.loneliness.LonelinessController.ghost;

@CommandAlias("ghost|gh")
public class CommandGhost extends BaseCommand {
    @Default
    @CommandCompletion("@players")
    @CommandPermission("%permissiontimingsystem_ghost")
    public static void onGhost(Player player, String targetName) {
        Player target = player.getServer().getPlayer(targetName);
        TPlayer tPlayer = TSDatabase.getPlayer(target.getUniqueId());
        Boolean isGhosted = LonelinessController.isGhosted(target.getUniqueId());

        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());
        if (!maybeDriver.isPresent()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        Driver driver = maybeDriver.get();
        Heat heat = driver.getHeat();

        if (heat.getLonely()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        ghost(player.getUniqueId());

        Text.send(player, Success.GHOSTING_ON);
        Text.send(target, Warning.GHOSTING_TARGET_ON);
    }
}
