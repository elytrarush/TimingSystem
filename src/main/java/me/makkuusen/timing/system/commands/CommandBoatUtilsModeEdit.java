package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.boatutils.CustomBoatUtilsMode;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandAlias("boatutilsmodeedit|bume|bumode")
public class CommandBoatUtilsModeEdit extends BaseCommand {

    private static final Map<Player, CustomBoatUtilsMode> modeEditSessions = new HashMap<>();

    @Subcommand("create")
    @CommandCompletion("name")
    @CommandPermission("%permissionboatutilsmode_create")
    public static void onCreate(Player player, String name) {
        CustomBoatUtilsMode mode = new CustomBoatUtilsMode();
        mode.setName(name);
        modeEditSessions.put(player, mode);
        Text.send(player, Success.CREATED, "%mode%", name);
    }

    @Subcommand("edit")
    @CommandCompletion("name")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onEdit(Player player, String name) {
        CustomBoatUtilsMode mode = TSDatabase.getCustomBoatUtilsModeFromName(name);
        if (mode == null) {
            Text.send(player, Error.GENERIC);
            return;
        }
        modeEditSessions.put(player, mode);
        Text.send(player, Success.GHOSTING_OFF);
    }

    @Subcommand("save")
    @CommandPermission("%permissionboatutilsmode_save")
    public static void onSave(Player player) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) {
            Text.send(player, Error.GENERIC);
            return;
        }
        boolean success = TSDatabase.saveOrUpdateCustomBoatUtilsMode(mode);
        if (success) {
            Text.send(player, Success.GHOSTING_OFF);
        } else {
            Text.send(player, Error.GENERIC);
        }
    }

    @Subcommand("set")
    @CommandCompletion("name|stepHeight|defaultSlipperiness|boatJumpForce|yawAcceleration|forwardAcceleration|backwardAcceleration|turningForwardAcceleration|swimForce|gravity|boatFallDamage|boatWaterElevation|boatAirControl|airStepping|allowAccelerationStacking|underwaterControl|surfaceWaterControl|waterJumping|coyoteTime <value>")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onSet(Player player, String property, String value) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) return;
        try {
            switch (property.toLowerCase()) {
                case "name" -> mode.setName(value);
                case "stepheight" -> mode.setStepHeight(Float.parseFloat(value));
                case "defaultslipperiness" -> mode.setDefaultSlipperiness(Float.parseFloat(value));
                case "boatjumpforce" -> mode.setBoatJumpForce(Float.parseFloat(value));
                case "yawacceleration" -> mode.setYawAcceleration(Float.parseFloat(value));
                case "forwardacceleration" -> mode.setForwardAcceleration(Float.parseFloat(value));
                case "backwardacceleration" -> mode.setBackwardAcceleration(Float.parseFloat(value));
                case "turningforwardacceleration" -> mode.setTurningForwardAcceleration(Float.parseFloat(value));
                case "swimforce" -> mode.setSwimForce(Float.parseFloat(value));
                case "gravity" -> mode.setGravity(Double.parseDouble(value));
                case "boatfalldamage" -> mode.setBoatFallDamage(Boolean.parseBoolean(value));
                case "boatwaterelevation" -> mode.setBoatWaterElevation(Boolean.parseBoolean(value));
                case "boataircontrol" -> mode.setBoatAirControl(Boolean.parseBoolean(value));
                case "airstepping" -> mode.setAirStepping(Boolean.parseBoolean(value));
                case "allowaccelerationstacking" -> mode.setAllowAccelerationStacking(Boolean.parseBoolean(value));
                case "underwatercontrol" -> mode.setUnderwaterControl(Boolean.parseBoolean(value));
                case "surfacewatercontrol" -> mode.setSurfaceWaterControl(Boolean.parseBoolean(value));
                case "waterjumping" -> mode.setWaterJumping(Boolean.parseBoolean(value));
                case "coyotetime" -> mode.setCoyoteTime(Integer.parseInt(value));
            }
        } catch (Exception ignored) {}
    }

    @Subcommand("addblockslip")
    @CommandCompletion("<slipperiness> <blockIds>")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onAddBlockSlip(Player player, float slipperiness, String blockIds) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) return;
        mode.setBlocksSlipperiness(slipperiness, blockIds);
    }

    @Subcommand("clearblockslip")
    @CommandCompletion("<blockIds>")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onClearBlockSlip(Player player, String blockIds) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) return;
        mode.clearBlocksSlipperiness(blockIds);
    }

    @Subcommand("clearallblockslip")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onClearAllBlockSlip(Player player) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) return;
        mode.clearAllSlipperiness();
    }

    @Subcommand("addperblock")
    @CommandCompletion("<settingType> <value> <blockIds>")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onAddPerBlock(Player player, short settingType, float value, String blockIds) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) return;
        mode.setPerBlockSetting(settingType, value, blockIds);
    }

    @Subcommand("clearperblock")
    @CommandCompletion("<blockIds>")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onClearPerBlock(Player player, String blockIds) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) return;
        mode.clearPerBlockSettings(blockIds);
    }

    @Subcommand("clearallperblock")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onClearAllPerBlock(Player player) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) return;
        mode.clearAllPerBlockSettings();
    }

    @Subcommand("reset")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onReset(Player player) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) return;
        mode.resetToVanilla();
    }

    @Subcommand("info")
    @CommandPermission("%permissionboatutilsmode_edit")
    public static void onInfo(Player player) {
        CustomBoatUtilsMode mode = modeEditSessions.get(player);
        if (mode == null) {
            Text.send(player, Error.GENERIC, "%message%", "You are not currently editing a boatutils mode.");
            return;
        }

        player.sendMessage("-----------------------------------------------------");
        player.sendMessage("Editing BoatUtils Mode: &e" + mode.getName());
        player.sendMessage("-----------------------------------------------------");

        Map<String, List<String>> nonDefaultSettings = mode.getNonDefaultSettings();

        if (nonDefaultSettings.isEmpty()) {
            player.sendMessage("All settings are at vanilla default values.");
        } else {
            player.sendMessage("Customized Settings (non-vanilla):");
            nonDefaultSettings.forEach((category, settings) -> {
                player.sendMessage(category);
                settings.forEach(player::sendMessage);
            });
        }

        player.sendMessage("-----------------------------------------------------");
    }
}
