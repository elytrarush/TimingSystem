package me.makkuusen.timing.system.track.medals;

import lombok.Getter;
import lombok.Setter;

@Getter
public class TrackMedalsData {
    @Setter
    private long time;
    private final double pos;
    private final String text;

    TrackMedalsData(double pos, String text) {
        this.pos = pos;
        this.text = text;
    }
}
