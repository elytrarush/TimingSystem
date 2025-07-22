package me.makkuusen.timing.system.boatutils;

import com.google.gson.annotations.Expose;
import lombok.Getter;

@Getter
public class PerBlockSetting {
    @Expose
    private final short type;
    @Expose
    private final Object value;
    @Expose
    private final String blockId;

    public PerBlockSetting(short type, Object value, String blockId) {
        this.type = type;
        this.value = value;
        this.blockId = blockId;
    }

    public float getAsFloat() {
        return ((Number) value).floatValue();
    }
}
