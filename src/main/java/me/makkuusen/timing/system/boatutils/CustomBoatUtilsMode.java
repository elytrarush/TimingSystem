package me.makkuusen.timing.system.boatutils;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import me.makkuusen.timing.system.TimingSystem;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Getter
@Setter
public class CustomBoatUtilsMode {

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
    private static final short PACKET_ID_SET_PER_BLOCK_SETTING = 26;
    private static final short PACKET_ID_SET_AIR_STEPPING = 28;

    private static final Gson GSON = new Gson();

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    private String name;
    private float stepHeight;
    private float defaultSlipperiness;
    private Map<String, Float> blocksSlipperiness = new HashMap<>();
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

    private Map<String, PerBlockSetting> perBlockSettings = new HashMap<>();

    /**
     * Creates a new CustomBoatutilsMode with default vanilla values
     */
    public CustomBoatUtilsMode() {
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

        // Block slipperiness settings
        for (Map.Entry<String, Float> entry : this.blocksSlipperiness.entrySet()) {
            sendShortAndFloatAndStringPacket(player, PACKET_ID_SET_BLOCKS_SLIPPERINESS, entry.getValue(), entry.getKey());
        }

        // Per-block settings
        for (PerBlockSetting setting : this.perBlockSettings.values()) {
            if (setting.getValue() instanceof Boolean) {
                sendShortAndBooleanPacket(player, setting.getType(), setting.getAsBoolean());
            } else if (setting.getValue() instanceof Float) {
                sendShortAndFloatPacket(player, setting.getType(), setting.getAsFloat());
            } else if (setting.getValue() instanceof Integer) {
                sendShortAndIntPacket(player, setting.getType(), setting.getAsInt());
            } else if (setting.getValue() instanceof Double) {
                sendShortAndDoublePacket(player, setting.getType(), setting.getAsDouble());
            }
            // Send block ID as a separate packet if needed
            if (setting.getBlockId() != null && !setting.getBlockId().isEmpty()) {
                sendShortAndStringPacket(player, PACKET_ID_SET_PER_BLOCK_SETTING, setting.getBlockId());
            }
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
    
    private static void sendShortAndStringPacket(Player player, short packetId, String stringValue) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(byteStream)) {
            out.writeShort(packetId);
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
    public void applySettingsFrom(CustomBoatUtilsMode other) {
        if (other == null) return;

        if (other.stepHeight != 0f) this.stepHeight = other.stepHeight;
        if (other.defaultSlipperiness != 0.6f) this.defaultSlipperiness = other.defaultSlipperiness;
        this.blocksSlipperiness.putAll(other.blocksSlipperiness);
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
        this.perBlockSettings.putAll(other.perBlockSettings);
        this.airStepping = other.airStepping;
    }

    /**
     * Sets block slipperiness for specific blocks
     * @param slipperiness The slipperiness value
     * @param blockId The block ID
     */
    public void setBlocksSlipperiness(float slipperiness, String blockId) {
        blocksSlipperiness.put(blockId, slipperiness);
    }

    /**
     * Clears specific block slipperiness settings
     * @param blockId The block ID to clear
     */
    public void clearBlocksSlipperiness(String blockId) {
        blocksSlipperiness.remove(blockId);
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
     * @param blockId The block ID
     */
    public void setPerBlockSetting(short settingType, Object value, String blockId) {
        perBlockSettings.put(blockId, new PerBlockSetting(settingType, value, blockId));
    }

    /**
     * Clears per-block settings for specific blocks
     * @param blockId The block ID to clear
     */
    public void clearPerBlockSettings(String blockId) {
        perBlockSettings.remove(blockId);
    }

    /**
     * Clears all per-block settings for the mode
     */
    public void clearAllPerBlockSettings() {
        perBlockSettings.clear();
    }

    /**
     * Converts this CustomBoatUtilsMode object to a JSON string.
     * @return A JSON representation of this object.
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Creates a CustomBoatUtilsMode object from a JSON string.
     * @param json The JSON string to parse.
     * @return A new CustomBoatUtilsMode instance.
     */
    public static CustomBoatUtilsMode fromJson(String json) {
        return GSON.fromJson(json, CustomBoatUtilsMode.class);
    }

    /**
     * Gathers all settings that are not at their vanilla default value.
     * The settings are grouped by category for cleaner display.
     *
     * @return A map where keys are category names and values are lists of formatted setting strings.
     */
    public Map<String, List<String>> getNonDefaultSettings() {
        Map<String, List<String>> nonDefaultSettings = new HashMap<>();

        List<String> numericSettings = new ArrayList<>();
        if (this.stepHeight != 0f) numericSettings.add("  &e- stepHeight: &f" + this.stepHeight);
        if (this.defaultSlipperiness != 0.6f) numericSettings.add("  &e- defaultSlipperiness: &f" + this.defaultSlipperiness);
        if (this.boatJumpForce != 0f) numericSettings.add("  &e- boatJumpForce: &f" + this.boatJumpForce);
        if (this.yawAcceleration != 1.0f) numericSettings.add("  &e- yawAcceleration: &f" + this.yawAcceleration);
        if (this.forwardAcceleration != 0.04f) numericSettings.add("  &e- forwardAcceleration: &f" + this.forwardAcceleration);
        if (this.backwardAcceleration != 0.005f) numericSettings.add("  &e- backwardAcceleration: &f" + this.backwardAcceleration);
        if (this.turningForwardAcceleration != 0.005f) numericSettings.add("  &e- turningForwardAcceleration: &f" + this.turningForwardAcceleration);
        if (this.swimForce != 0f) numericSettings.add("  &e- swimForce: &f" + this.swimForce);
        if (this.gravity != -0.03999999910593033) numericSettings.add("  &e- gravity: &f" + this.gravity);
        if (this.coyoteTime != 0) numericSettings.add("  &e- coyoteTime: &f" + this.coyoteTime);
        if (!numericSettings.isEmpty()) {
            nonDefaultSettings.put("&bNumeric Settings:", numericSettings);
        }

        List<String> booleanSettings = new ArrayList<>();
        if (!this.boatFallDamage) booleanSettings.add("  &e- boatFallDamage: &f" + this.boatFallDamage);
        if (this.boatWaterElevation) booleanSettings.add("  &e- boatWaterElevation: &f" + this.boatWaterElevation);
        if (this.boatAirControl) booleanSettings.add("  &e- boatAirControl: &f" + this.boatAirControl);
        if (this.airStepping) booleanSettings.add("  &e- airStepping: &f" + this.airStepping);
        if (this.allowAccelerationStacking) booleanSettings.add("  &e- allowAccelerationStacking: &f" + this.allowAccelerationStacking);
        if (this.underwaterControl) booleanSettings.add("  &e- underwaterControl: &f" + this.underwaterControl);
        if (this.surfaceWaterControl) booleanSettings.add("  &e- surfaceWaterControl: &f" + this.surfaceWaterControl);
        if (this.waterJumping) booleanSettings.add("  &e- waterJumping: &f" + this.waterJumping);
        if (!booleanSettings.isEmpty()) {
            nonDefaultSettings.put("&bBoolean Toggles:", booleanSettings);
        }

        if (this.blocksSlipperiness != null && !this.blocksSlipperiness.isEmpty()) {
            List<String> slipperinessList = new ArrayList<>();
            this.blocksSlipperiness.forEach((blockId, slipperiness) ->
                    slipperinessList.add("  &e- " + blockId + ": &f" + slipperiness)
            );
            nonDefaultSettings.put("&bBlock Slipperiness:", slipperinessList);
        }

        if (this.perBlockSettings != null && !this.perBlockSettings.isEmpty()) {
            Map<Short, List<String>> settingsByType = new HashMap<>();
            this.perBlockSettings.forEach((blockId, setting) -> {
                String valueStr = setting.getValue() != null ? 
                    (setting.getValue() instanceof Float ? String.format("%.3f", setting.getValue()) : 
                     String.valueOf(setting.getValue())) : "null";
                settingsByType.computeIfAbsent(setting.getType(), k -> new ArrayList<>())
                    .add("  &e- " + blockId + ": &f" + valueStr + " &7(" + 
                        (setting.getValue() != null ? setting.getValue().getClass().getSimpleName() : "null") + ")");
            });
            
            settingsByType.forEach((type, settings) -> {
                nonDefaultSettings.put("&bPer-Block Settings (Type " + type + "):", settings);
            });
        }

        return nonDefaultSettings;
    }
}