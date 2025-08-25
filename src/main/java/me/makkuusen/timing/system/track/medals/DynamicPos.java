package me.makkuusen.timing.system.track.medals;

import lombok.Getter;

@Getter
public class DynamicPos {
    private final int min;
    private final int max;
    private final double A;
    private final double p;

    public DynamicPos(int min, int max, double A, double p) {
        this.min = min;
        this.max = max;
        this.A = A;
        this.p = p;
    }

    public double getPos(int totalPositions, double defaultValue) {
        if (totalPositions < 1) return defaultValue;
        return A * Math.pow(totalPositions, p - 1);
    }
}
