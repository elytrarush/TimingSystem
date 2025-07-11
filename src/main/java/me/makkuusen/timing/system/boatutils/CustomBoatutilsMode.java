package me.makkuusen.timing.system.boatutils;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Triple;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Getter
@Setter
public class CustomBoatutilsMode {

    private static final short PACKET_ID_RESET = 0;
    private static final short PACKET_ID_SET_STEP_HEIGHT = 1;
    private static final short PACKET_ID_SET_DEFAULT_SLIPPERINESS = 2;
    private static final short PACKET_ID_SET_BLOCKS_SLIPPERINESS = 3;
    private static final short PACKET_ID_SET_BOAT_FALL_DAMAGE = 4;
    private static final short PACKET_ID_SET_BOAT_WATER_ELEVATION = 5;
    private static final short PACKET_ID_SET_BOAT_AIR_CONTROL = 6;
    private static final short PACKET_ID_SET_BOAT_JUMP_FORCE = 7;
    private static final short PACKET_ID_SET_GRAVITY = 9;
    private static final short PACKET_ID_SET_YAW_ACCELERATION = 10;
    private static final short PACKET_ID_SET_FORWARD_ACCELERATION = 11;
    private static final short PACKET_ID_SET_BACKWARD_ACCELERATION = 12;
    private static final short PACKET_ID_SET_TURNING_FORWARD_ACCELERATION = 13;
    private static final short PACKET_ID_ALLOW_ACCELERATION_STACKING = 14;
    private static final short PACKET_ID_SET_UNDERWATER_CONTROL = 16;
    private static final short PACKET_ID_SET_SURFACE_WATER_CONTROL = 17;
    private static final short PACKET_ID_SET_COYOTE_TIME = 19;
    private static final short PACKET_ID_SET_WATER_JUMPING = 20;
    private static final short PACKET_ID_SET_SWIM_FORCE = 21;
    private static final short PACKET_ID_CLEAR_BLOCKS_SLIPPERINESS = 22;
    private static final short PACKET_ID_CLEAR_ALL_SLIPPERINESS = 23;
    private static final short PACKET_ID_SET_PER_BLOCK_SETTING = 26;
    private static final short PACKET_ID_SET_AIR_STEPPING = 28;

    private static final Gson GSON = new Gson();

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    private float stepHeight;
    private float defaultSlipperiness;
    private List<Pair<Float, String>> blocksSlipperiness = new ArrayList<>();
    private boolean boatFallDamage;
    private boolean boatWaterElevation;
    private boolean boatAirControl;
    private float boatJumpForce;
    private boolean airStepping;
    private double gravity;
    private float yawAcceleration;
    private float forwardAcceleration;
    private float backwardAcceleration;
    private float turningForwardAcceleration;
    private boolean allowAccelerationStacking;
    private boolean underwaterControl;
    private boolean surfaceWaterControl;
    private int coyoteTime;
    private boolean waterJumping;
    private float swimForce;
    private List<Triple<Short, Float, String>> perBlockSettings = new ArrayList<>();

    /**
     * Creates a new CustomBoatutilsMode with default vanilla values
     */
    public CustomBoatutilsMode() {
        resetToVanilla();
    }

    /**
     * Resets all settings to vanilla defaults
     */
    public void resetToVanilla() {
        stepHeight = 0f;
        defaultSlipperiness = 0.6f;
        blocksSlipperiness.clear();
        boatFallDamage = true;
        boatWaterElevation = false;
        boatAirControl = false;
        boatJumpForce = 0f;
        airStepping = false;
        gravity = -0.03999999910593033;
        yawAcceleration = 1.0f;
        forwardAcceleration = 0.04f;
        backwardAcceleration = 0.005f;
        turningForwardAcceleration = 0.005f;
        allowAccelerationStacking = false;
        underwaterControl = false;
        surfaceWaterControl = false;
        coyoteTime = 0;
        waterJumping = false;
        swimForce = 0f;
        perBlockSettings.clear();
    }

    /**
     * Applies this custom mode to a player by sending a series of plugin messages.
     * This method constructs a separate packet for each non-vanilla setting.
     *
     * @param player The player to apply the mode to.
     */
    public void applyToPlayer(Player player) {
        // Send a reset packet first to ensure a clean slate.
        resetPlayer(player);

        // Basic movement settings
        if (this.stepHeight != 0f) sendShortAndFloatPacket(player, PACKET_ID_SET_STEP_HEIGHT, this.stepHeight);
        if (this.defaultSlipperiness != 0.6f) sendShortAndFloatPacket(player, PACKET_ID_SET_DEFAULT_SLIPPERINESS, this.defaultSlipperiness);
        if (!this.boatFallDamage) sendShortAndBooleanPacket(player, PACKET_ID_SET_BOAT_FALL_DAMAGE, this.boatFallDamage);
        if (this.boatWaterElevation) sendShortAndBooleanPacket(player, PACKET_ID_SET_BOAT_WATER_ELEVATION, this.boatWaterElevation);
        if (this.boatAirControl) sendShortAndBooleanPacket(player, PACKET_ID_SET_BOAT_AIR_CONTROL, this.boatAirControl);
        if (this.boatJumpForce != 0f) sendShortAndFloatPacket(player, PACKET_ID_SET_BOAT_JUMP_FORCE, this.boatJumpForce);
        if (this.airStepping) sendShortAndBooleanPacket(player, PACKET_ID_SET_AIR_STEPPING, this.airStepping);

        // Physics settings
        if (this.gravity != -0.03999999910593033) sendShortAndDoublePacket(player, PACKET_ID_SET_GRAVITY, this.gravity);
        if (this.yawAcceleration != 1.0f) sendShortAndFloatPacket(player, PACKET_ID_SET_YAW_ACCELERATION, this.yawAcceleration);
        if (this.forwardAcceleration != 0.04f) sendShortAndFloatPacket(player, PACKET_ID_SET_FORWARD_ACCELERATION, this.forwardAcceleration);
        if (this.backwardAcceleration != 0.005f) sendShortAndFloatPacket(player, PACKET_ID_SET_BACKWARD_ACCELERATION, this.backwardAcceleration);
        if (this.turningForwardAcceleration != 0.005f) sendShortAndFloatPacket(player, PACKET_ID_SET_TURNING_FORWARD_ACCELERATION, this.turningForwardAcceleration);
        if (this.allowAccelerationStacking) sendShortAndBooleanPacket(player, PACKET_ID_ALLOW_ACCELERATION_STACKING, this.allowAccelerationStacking);

        // Water control settings
        if (this.underwaterControl) sendShortAndBooleanPacket(player, PACKET_ID_SET_UNDERWATER_CONTROL, this.underwaterControl);
        if (this.surfaceWaterControl) sendShortAndBooleanPacket(player, PACKET_ID_SET_SURFACE_WATER_CONTROL, this.surfaceWaterControl);

        // Jump settings
        if (this.coyoteTime != 0) sendShortAndIntPacket(player, PACKET_ID_SET_COYOTE_TIME, this.coyoteTime);
        if (this.waterJumping) sendShortAndBooleanPacket(player, PACKET_ID_SET_WATER_JUMPING, this.waterJumping);
        if (this.swimForce != 0f) sendShortAndFloatPacket(player, PACKET_ID_SET_SWIM_FORCE, this.swimForce);

        // List-based settings
        for (Pair<Float, String> slipperinessPair : this.blocksSlipperiness) {
            sendShortAndFloatAndStringPacket(player, PACKET_ID_SET_BLOCKS_SLIPPERINESS, slipperinessPair.a, slipperinessPair.b);
        }

        for (Triple<Short, Float, String> perBlockSetting : this.perBlockSettings) {
            sendShortAndShortAndFloatAndStringPacket(player, PACKET_ID_SET_PER_BLOCK_SETTING, perBlockSetting.a, perBlockSetting.b, perBlockSetting.c);
        }
    }

    /**
     * Resets a player's boat settings to vanilla by sending a reset packet.
     *
     * @param player The player to reset.
     */
    public static void resetPlayer(Player player) {
        sendPacket(player, PACKET_ID_RESET);
    }

    //<editor-fold desc="Packet Sending Helpers">
    private static void sendPacket(Player player, short packetId) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndBooleanPacket(Player player, short packetId, boolean value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeBoolean(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndFloatPacket(Player player, short packetId, float value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeFloat(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndDoublePacket(Player player, short packetId, double value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeDouble(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndIntPacket(Player player, short packetId, int value) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeInt(value);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndFloatAndStringPacket(Player player, short packetId, float value, String stringValue) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeFloat(value);
            writeString(out, stringValue);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void sendShortAndShortAndFloatAndStringPacket(Player player, short packetId, short settingType, float value, String stringValue) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
            out.writeShort(settingType);
            out.writeFloat(value);
            writeString(out, stringValue);
            player.sendPluginMessage(TimingSystem.getPlugin(), "openboatutils:settings", byteStream.toByteArray());
        } catch (IOException e) {
            logPacketError(player, packetId, e);
        }
    }

    private static void logPacketError(Player player, short packetId, IOException e) {
        TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to serialize and send packet " + packetId + " for player " + player.getName(), e);
    }
    //</editor-fold>

    /**
     * Writes a String to a DataOutputStream in a format compatible with Minecraft's PacketByteBuf.
     * It first writes the string's byte length as a VarInt, then writes the UTF-8 encoded bytes.
     *
     * @param out          The stream to write to.
     * @param stringValue  The string to write.
     * @throws IOException If an I/O error occurs.
     */
    private static void writeString(DataOutputStream out, String stringValue) throws IOException {
        byte[] stringBytes = stringValue.getBytes(StandardCharsets.UTF_8);
        int length = stringBytes.length;

        // Write the length as a VarInt
        while (true) {
            if ((length & ~SEGMENT_BITS) == 0) {
                out.writeByte(length);
                break;
            }
            out.writeByte((length & SEGMENT_BITS) | CONTINUE_BIT);
            length >>>= 7;
        }

        // Write the UTF-8 bytes of the string
        out.write(stringBytes);
    }

    /**
     * Applies settings from another mode to this mode
     * @param other The mode to copy settings from
     */
    public void applySettingsFrom(CustomBoatutilsMode other) {
        if (other == null) return;

        if (other.stepHeight != 0f) this.stepHeight = other.stepHeight;
        if (other.defaultSlipperiness != 0.6f) this.defaultSlipperiness = other.defaultSlipperiness;
        this.blocksSlipperiness.addAll(other.blocksSlipperiness);
        this.boatFallDamage = other.boatFallDamage;
        this.boatWaterElevation = other.boatWaterElevation;
        this.boatAirControl = other.boatAirControl;
        if (other.boatJumpForce != 0f) this.boatJumpForce = other.boatJumpForce;
        if (other.gravity != -0.03999999910593033) this.gravity = other.gravity;
        if (other.yawAcceleration != 1.0f) this.yawAcceleration = other.yawAcceleration;
        if (other.forwardAcceleration != 0.04f) this.forwardAcceleration = other.forwardAcceleration;
        if (other.backwardAcceleration != 0.005f) this.backwardAcceleration = other.backwardAcceleration;
        if (other.turningForwardAcceleration != 0.005f) this.turningForwardAcceleration = other.turningForwardAcceleration;
        this.allowAccelerationStacking = other.allowAccelerationStacking;
        this.underwaterControl = other.underwaterControl;
        this.surfaceWaterControl = other.surfaceWaterControl;
        if (other.coyoteTime != 0) this.coyoteTime = other.coyoteTime;
        this.waterJumping = other.waterJumping;
        if (other.swimForce != 0f) this.swimForce = other.swimForce;
        this.perBlockSettings.addAll(other.perBlockSettings);
        this.airStepping = other.airStepping;
    }

    /**
     * Sets block slipperiness for specific blocks
     * @param slipperiness The slipperiness value
     * @param blockIds Comma-separated list of block IDs
     */
    public void setBlocksSlipperiness(float slipperiness, String blockIds) {
        blocksSlipperiness.add(new Pair<>(slipperiness, blockIds));
    }

    /**
     * Clears specific block slipperiness settings
     * @param blockIds Comma-separated list of block IDs to clear
     */
    public void clearBlocksSlipperiness(String blockIds) {
        blocksSlipperiness.removeIf(pair -> pair.b.equals(blockIds));
    }

    /**
     * Clears all block slipperiness settings
     */
    public void clearAllSlipperiness() {
        blocksSlipperiness.clear();
    }

    /**
     * Sets per-block settings for specific blocks
     * @param settingType The setting type (0=jumpForce, 1=forwardsAccel, etc.)
     * @param value The value to set
     * @param blockIds Comma-separated list of block IDs
     */
    public void setPerBlockSetting(short settingType, float value, String blockIds) {
        perBlockSettings.add(new Triple<>(settingType, value, blockIds));
    }

    /**
     * Clears per-block settings for specific blocks
     * @param blockIds Comma-separated list of block IDs to clear
     */
    public void clearPerBlockSettings(String blockIds) {
        perBlockSettings.removeIf(triple -> triple.c.equals(blockIds));
    }

    /**
     * Clears all per-block settings for the mode
     */
    public void clearAllPerBlockSettings() {
        perBlockSettings.clear();
    }

    /**
     * Converts this CustomBoatutilsMode object to a JSON string.
     * @return A JSON representation of this object.
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Creates a CustomBoatutilsMode object from a JSON string.
     * @param json The JSON string to parse.
     * @return A new CustomBoatutilsMode instance.
     */
    public static CustomBoatutilsMode fromJson(String json) {
        return GSON.fromJson(json, CustomBoatutilsMode.class);
    }
}