package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import me.makkuusen.timing.system.ApiUtilities;
import me.makkuusen.timing.system.api.TimingSystemAPI;
import me.makkuusen.timing.system.participant.Driver;
import me.makkuusen.timing.system.participant.DriverState;
import me.makkuusen.timing.system.round.FinalRound;
import me.makkuusen.timing.system.round.QualificationRound;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import org.bukkit.entity.Player;

import static me.makkuusen.timing.system.heat.QualifyHeat.timeIsOver;

@CommandAlias("reset|re")
public class CommandReset extends BaseCommand {

    @Default
    @CommandPermission("%permissiontimingsystem_reset")
    public static void onReset(Player player) {
        var maybeDriver = TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId());

        if (maybeDriver.isEmpty()) {
            ApiUtilities.resetPlayerTimeTrial(player);
            return;
        }

        Driver driver = maybeDriver.get();

        if (!driver.isRunning()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        var round = driver.getHeat().getRound();
        if (round instanceof QualificationRound && driver.getState() == DriverState.RUNNING) {
            if (timeIsOver(driver)) {
                Text.send(player, Error.NOT_NOW);
                return;
            }
            if (driver.getHeat().getReset()) {
                ApiUtilities.teleportPlayerAndSpawnBoat(driver.getTPlayer().getPlayer(), driver.getHeat().getEvent().getTrack(), driver.getHeat().getEvent().getTrack().getSpawnLocation());
                driver.setState(DriverState.RESET);
            } else {
                Text.send(player, Error.NOT_NOW);
            }
        } else if (round instanceof FinalRound) {
            // reset to the last checkpoint
        } else {
            Text.send(player, Error.NOT_NOW);
        }
    }
}
