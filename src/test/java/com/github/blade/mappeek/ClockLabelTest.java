package com.github.blade.mappeek;

import com.github.blade.hvac.api.SeasonView;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** The time of day and season in the map's layer list (0.2.0). */
class ClockLabelTest {
    @Test
    void theClockReadsAsEzvacsHudDoes() {
        assertEquals("06:00", ClockLabel.clock(0), "sunrise");
        assertEquals("00:00", ClockLabel.clock(18_000), "midnight");
        assertEquals("14:30", ClockLabel.clock(8_500));
        assertEquals("05:59", ClockLabel.clock(24_000L * 77 + 23_999));
    }

    @Test
    void theSeasonFollowsTheClock() {
        String summer = ClockLabel.season(Optional.of(new SeasonView(SeasonView.Season.SUMMER, 3, 15, false)));
        assertEquals("Summer, day 3 of 15", summer);
        assertEquals("14:30 · Summer, day 3 of 15", ClockLabel.label(8_500, summer));
        assertEquals("Winter, day 1 of 15 (paused)",
                ClockLabel.season(Optional.of(new SeasonView(SeasonView.Season.WINTER, 1, 15, true))));
        assertEquals("14:30", ClockLabel.label(8_500, ClockLabel.season(Optional.empty())), "no seasons");
    }
}
