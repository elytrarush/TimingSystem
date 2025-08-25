package me.makkuusen.timing.system.track.medals;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrackMedalsData {
    private long time;
    private double pos;
    private String text;

    TrackMedalsData(double pos, String text) {
        this.pos = pos;
        this.text = text;
    }
}
