package me.makkuusen.timing.system.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.makkuusen.timing.system.TimingSystem;
import me.makkuusen.timing.system.TrackTagManager;
import me.makkuusen.timing.system.database.TSDatabase;
import me.makkuusen.timing.system.permissions.PermissionTimingSystem;
import me.makkuusen.timing.system.theme.TSColor;
import me.makkuusen.timing.system.theme.Text;
import me.makkuusen.timing.system.theme.Theme;
import me.makkuusen.timing.system.theme.messages.Error;
import me.makkuusen.timing.system.theme.messages.Success;
import me.makkuusen.timing.system.tplayer.TPlayer;
import me.makkuusen.timing.system.track.tags.TrackTag;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.hologram.Hologram;
import de.oliver.fancyholograms.api.data.TextHologramData;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

@CommandAlias("timingsystem|ts")
public class CommandTimingSystem extends BaseCommand {
    @Subcommand("tag create")
    @CommandCompletion("<tag>")
    @CommandPermission("%permissiontimingsystem_tag_create")
    public static void onCreateTag(CommandSender commandSender, String value) {
        if (!value.matches("[A-Za-zÅÄÖåäöØÆøæ0-9]+")) {
            Text.send(commandSender, Error.INVALID_NAME);
            return;
        }

        if (TrackTagManager.createTrackTag(value)) {
            Text.send(commandSender, Success.CREATED_TAG, "%tag%", value);
            return;
        }

        Text.send(commandSender, Error.FAILED_TO_CREATE_TAG);
    }

    @Subcommand("tag color")
    @CommandCompletion("@trackTag <hexcolorcode>")
    @CommandPermission("%permissiontimingsystem_tag_set_color")
    public static void onSetTagColor(CommandSender commandSender, TrackTag tag, String color) {
        if (!color.startsWith("#")) {
            color = "#" + color;
        }
        if (TextColor.fromHexString(color) == null) {
            Text.send(commandSender, Error.COLOR_FORMAT);
            return;
        }

        tag.setColor(Objects.requireNonNull(TextColor.fromHexString(color)));
        Text.send(commandSender, Success.SAVED);

    }

    @Subcommand("tag item")
    @CommandCompletion("@trackTag")
    @CommandPermission("%permissiontimingsystem_tag_set_item")
    public static void onSetTagItem(Player player, TrackTag tag) {
        var item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null) {
            Text.send(player, Error.ITEM_NOT_FOUND);
            return;
        }
        tag.setItem(item);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("tag weight")
    @CommandCompletion("@trackTag <value>")
    @CommandPermission("%permissiontimingsystem_tag_set_weight")
    public static void onSetTagItem(Player player, TrackTag tag, int weight) {
        tag.setWeight(weight);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("tag delete")
    @CommandCompletion("@trackTag <value>")
    @CommandPermission("%permissiontimingsystem_tag_delete")
    public static void onDeleteTag(Player player, TrackTag tag) {
        TrackTagManager.deleteTag(tag);
        Text.send(player, Success.SAVED);
    }

    @Subcommand("scoreboard maxrows")
    @CommandCompletion("<value>")
    @CommandPermission("%permissiontimingsystem_scoreboard_set_maxrows")
    public static void onMaxRowsScoreboardChange(CommandSender sender, int rows) {
        TimingSystem.configuration.setScoreboardMaxRows(rows);
        Text.send(sender, Success.SAVED);
    }

    @Subcommand("scoreboard interval")
    @CommandCompletion("<value in ms>")
    @CommandPermission("%permissiontimingsystem_scoreboard_set_interval")
    public static void onIntervalScoreboardChange(CommandSender sender, String value) {
        TimingSystem.configuration.setScoreboardInterval(value);
        Text.send(sender, Success.SAVED);
    }

    @Subcommand("shortname")
    @CommandCompletion("<shortname> @players")
    @CommandPermission("%permissiontimingsystem_shortname_others")
    public static void onShortNameOthers(CommandSender sender, @Single String shortName, String playerName) {
        TPlayer tPlayer = TSDatabase.getPlayer(playerName);
        if (tPlayer == null) {
            Text.send(sender, Error.PLAYER_NOT_FOUND);
            return;
        }

        int maxLength = 4;
        int minLength = 3;

        if (shortName.length() < minLength || shortName.length() > maxLength) {
            Text.send(sender, Error.INVALID_NAME);
            return;
        }

        if (!shortName.matches("[A-Za-z0-9]+")) {
            Text.send(sender, Error.INVALID_NAME);
            return;
        }

        tPlayer.getSettings().setShortName(shortName);
        Text.send(sender, Success.SAVED);
    }


    @Subcommand("hexcolor")
    @CommandCompletion("@tscolor <hexcolorcode>")
    @CommandPermission("%permissiontimingsystem_color_set_hex")
    public static void onColorChange(CommandSender sender, TSColor tsColor, String hex) {
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        TextColor color;
        Theme theme = Theme.getTheme(sender);
        if (isValidHexCode(hex)) {
            color = TextColor.fromHexString(hex);
            if (color == null) {
                Text.send(sender,Error.COLOR_FORMAT);
                return;
            }
            switch (tsColor) {
                case SECONDARY -> theme.setSecondary(color);
                case PRIMARY -> theme.setPrimary(color);
                case AWARD -> theme.setAward(color);
                case AWARD_SECONDARY -> theme.setAwardSecondary(color);
                case ERROR -> theme.setError(color);
                case BROADCAST -> theme.setBroadcast(color);
                case SUCCESS -> theme.setSuccess(color);
                case WARNING -> theme.setWarning(color);
                case TITLE -> theme.setTitle(color);
                case BUTTON -> theme.setButton(color);
                case BUTTON_ADD -> theme.setButtonAdd(color);
                case BUTTON_REMOVE -> theme.setButtonRemove(color);
                default -> {
                }
            }
            sender.sendMessage(Text.get(sender, Success.COLOR_UPDATED).color(color));
            return;
        }
        Text.send(sender,Error.COLOR_FORMAT);
    }

    @Subcommand("globalleaderboard create")
    @CommandCompletion("<name> [title]")
    @CommandPermission("%permissiontimingsystem_globalleaderboard_create")
    public static void onCreateGlobalLeaderboard(Player player, String name, String title) {
        if (player == null) {
            Text.send(player, Error.ONLY_PLAYERS);
            return;
        }
        if (player.getServer().getPluginManager().getPlugin("FancyHolograms") == null) {
            Text.send(player, Error.GENERIC);
            return;
        }
        if (name == null || name.isBlank()) {
            Text.send(player, Error.INVALID_NAME);
            return;
        }

        String finalTitle = (title == null || title.isBlank()) ? "Global Leaderboard" : title;

        List<Map.Entry<TPlayer, Double>> top = GlobalLeaderboardFHManager.computeGlobalPointsTop(10);

        // Build text lines (Strings with color codes)
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("§6§l" + finalTitle);
        lines.add(" ");
        int i = 1;
        for (Map.Entry<TPlayer, Double> e : top) {
            TPlayer tp = e.getKey();
            double pts = e.getValue();
            lines.add(String.format("§6%2d. §f%s  §6%s", i, tp.getName(), GlobalLeaderboardFHManager.formatPoints(pts)));
            i++;
        }

        Location loc = player.getLocation().clone();
        float yaw = loc.getYaw();
        float roundedYaw = Math.round(yaw / 90f) * 90f;
        loc.setYaw(roundedYaw);
        loc.setPitch(0f);

        var hm = FancyHologramsPlugin.get().getHologramManager();
        java.util.Optional<Hologram> existing = hm.getHologram(name);
        existing.ifPresent(hm::removeHologram);

        TextHologramData data = new TextHologramData(name, loc);
        data.setBillboard(Display.Billboard.FIXED);
        data.setTextAlignment(TextDisplay.TextAlignment.LEFT);
        data.setPersistent(true);
        data.setText(lines);

        Hologram hologram = hm.create(data);
        hm.addHologram(hologram);

        GlobalLeaderboardFHManager.register(name, finalTitle);
        Text.send(player, Success.SAVED);
    }

    // Overload without title (optional parameter)
    @Subcommand("globalleaderboard create")
    @CommandCompletion("<name>")
    @CommandPermission("%permissiontimingsystem_globalleaderboard_create")
    public static void onCreateGlobalLeaderboard(Player player, String name) {
        onCreateGlobalLeaderboard(player, name, "Global Leaderboard");
    }

    @Subcommand("color")
    @CommandCompletion("@tscolor @namedColor")
    @CommandPermission("%permissiontimingsystem_color_set_named")
    public static void onNamedColorChange(CommandSender sender, TSColor tsColor, NamedTextColor color) {
        if(sender instanceof Player player) {
            if (!player.hasPermission(PermissionTimingSystem.COLOR_SET_NAMED.getNode())) {
                Text.send(player, Error.PERMISSION_DENIED);
                return;
            }
        }

        if (color == null) {
            Text.send(sender,Error.NO_HEX_COLOR_IN_TS_COLOR);
            return;
        }

        Theme theme = Theme.getTheme(sender);
        switch (tsColor) {
            case SECONDARY -> theme.setSecondary(color);
            case PRIMARY -> theme.setPrimary(color);
            case AWARD -> theme.setAward(color);
            case AWARD_SECONDARY -> theme.setAwardSecondary(color);
            case ERROR -> theme.setError(color);
            case BROADCAST -> theme.setBroadcast(color);
            case SUCCESS -> theme.setSuccess(color);
            case WARNING -> theme.setWarning(color);
            case TITLE -> theme.setTitle(color);
            case BUTTON -> theme.setButton(color);
            case BUTTON_ADD -> theme.setButtonAdd(color);
            case BUTTON_REMOVE -> theme.setButtonRemove(color);
            default -> {
            }
        }
        sender.sendMessage(Text.get(sender, Success.COLOR_UPDATED).color(color));
    }

    public static boolean isValidHexCode(String str) {
        // Regex to check valid hexadecimal color code.
        String regex = "^#([A-Fa-f0-9]{6})$";

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        // If the string is empty
        // return false
        if (str == null) {
            return false;
        }

        // Pattern class contains matcher() method
        // to find matching between given string
        // and regular expression.
        Matcher m = p.matcher(str);

        // Return if the string
        // matched the ReGex
        return m.matches();
    }

}
