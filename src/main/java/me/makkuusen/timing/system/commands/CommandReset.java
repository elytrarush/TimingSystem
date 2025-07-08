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
        TimingSystemAPI.getDriverFromRunningHeat(player.getUniqueId())
                .ifPresentOrElse(
                        driver -> handleDriverReset(player, driver),
                        () -> ApiUtilities.resetPlayerTimeTrial(player)
                );
    }

    private static void handleDriverReset(Player player, Driver driver) {
        if (canResetDriver(driver)) {
            performInHeatReset(driver);
        } else {
            Text.send(player, Error.NOT_NOW);
        }
    }

        if (maybeDriver.isEmpty()) {
            ApiUtilities.resetPlayerTimeTrial(player);
            return;
        }

        Driver driver = maybeDriver.get();

        if (!driver.isRunning()) {
            Text.send(player, Error.NOT_NOW);
            return;
        }

        return !isPlayerInPit(driver);
    }

    private static boolean canResetInFinal(Driver driver) {
        return (driver.getState() == DriverState.RUNNING || driver.getState() == DriverState.STARTING)
                && !isPlayerInPit(driver);
    }

    private static boolean isValidStateForQualificationReset(DriverState state) {
        return state == DriverState.RUNNING ||
                state == DriverState.LAPRESET ||
                state == DriverState.RESET ||
                state == DriverState.STARTING;
    }

    private static boolean isPlayerInPit(Driver driver) {
        return driver.getHeat().getEvent().getTrack().getTrackRegions()
                .getRegions(TrackRegion.RegionType.INPIT)
                .stream()
                .anyMatch(region -> region.contains(driver.getTPlayer().getPlayer().getLocation()));
    }

    public static void performInHeatReset(Driver driver) {
        if (driver.getState() == DriverState.RUNNING) {
            resetToCheckpoint(driver);
        } else if (driver.getState() == DriverState.STARTING) {
            resetToGrid(driver);
        } else if (driver.getState() == DriverState.RESET) {
            resetToTrackSpawn(driver);
        }
    }
}
