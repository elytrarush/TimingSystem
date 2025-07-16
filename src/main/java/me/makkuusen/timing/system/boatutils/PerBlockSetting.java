package me.makkuusen.timing.system.boatutils;

import lombok.Getter;

@Getter
public class PerBlockSetting {
    private final short type;
    private final Object value;
    private final String blockId;
    private final Class<?> valueType;

    public PerBlockSetting(short type, Object value, String blockId) {
        this.type = type;
        this.value = value;
        this.blockId = blockId;
        this.valueType = value != null ? value.getClass() : null;
    }

    public boolean getAsBoolean() {
        return (boolean) value;
    }

    public float getAsFloat() {
        return ((Number) value).floatValue();
    }

    public int getAsInt() {
        return ((Number) value).intValue();
    }

    public double getAsDouble() {
        return ((Number) value).doubleValue();
    }
}
