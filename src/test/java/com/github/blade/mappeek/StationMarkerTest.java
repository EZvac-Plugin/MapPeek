package com.github.blade.mappeek;

import com.github.blade.hvac.api.Position;
import com.github.blade.hvac.api.WeatherStationView;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** What a weather station looks like on the map, and MapPeek's settings. */
class StationMarkerTest {
    private static final UUID WORLD = UUID.randomUUID();

    private static WeatherStationView station(String label, boolean enabled, boolean intact, long strikes, String note) {
        List<WeatherStationView.ForecastDay> forecast = !enabled || !intact ? List.of() : List.of(
                new WeatherStationView.ForecastDay(0, 74.0, 55.0, WeatherStationView.Sky.CLEAR, 0.2, 0.0,
                        Optional.empty(), 30.0, note));
        return new WeatherStationView(label, new Position(WORLD, 631, -58, -20), enabled, intact, strikes, 70.0,
                forecast, 100, 20);
    }

    @Test
    void aWorkingStationIsItsCoverageWithTodaysWeather() {
        StationMarker marker = StationMarker.of(station("Harbour", true, true, 3, "Rain from 15:00"), false);
        assertEquals(631, marker.x());
        assertEquals(-20, marker.z());
        assertEquals(100, marker.radius(), "the area it measures");
        assertEquals(StationMarker.State.WORKING, marker.state());
        assertEquals("Harbour · weather station", marker.hover());
        assertEquals("<b>Harbour</b> <i>weather station</i><br>Today: Clear · Hi 74° Lo 55°<br>Rain from 15:00"
                + "<br>Measures 100 blocks around<br>Struck by lightning 3 times", marker.details());
    }

    @Test
    void offAndDamagedStationsSaySoInTheirOwnColour() {
        assertEquals(StationMarker.State.OFF, StationMarker.of(station("Hill", false, true, 0, ""), false).state());
        StationMarker damaged = StationMarker.of(station("Hill", true, false, 1, ""), false);
        assertEquals(StationMarker.State.DAMAGED, damaged.state());
        assertTrue(damaged.details().contains("Damaged: rebuild its tower"), damaged.details());
        assertTrue(damaged.details().endsWith("Struck by lightning 1 time"), damaged.details());
        assertTrue(damaged.hover().endsWith("(damaged)"));
    }

    @Test
    void aLabelIsShownAsTextNeverAsHtml() {
        StationMarker marker = StationMarker.of(station("<script>x</script>&", true, true, 0, ""), false);
        assertTrue(marker.details().startsWith("<b>&lt;script&gt;x&lt;/script&gt;&amp;</b>"), marker.details());
    }

    @Test
    void temperaturesFollowTheUnitsSetting() {
        assertTrue(StationMarker.of(station("Harbour", true, true, 0, ""), true).details().contains("Hi 23° Lo 13°"));
    }

    @Test
    void settingsKeepTheirDefaultsWhenOutOfRange() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("refresh-seconds", 2);
        config.set("units", "K");
        config.set("stations.fill-opacity", 3.0);
        config.set("stations.colors.working", "blue");
        config.set("stations.colors.off", "#112233");
        List<String> warnings = new ArrayList<>();
        Settings settings = Settings.load(config, warnings::add);
        assertEquals(60, settings.refreshSeconds());
        assertFalse(settings.celsius());
        assertEquals(0.12, settings.fillOpacity());
        assertEquals(new Color(0x4FA3FF), settings.color(StationMarker.State.WORKING));
        assertEquals(new Color(0x112233), settings.color(StationMarker.State.OFF));
        assertEquals(4, warnings.size(), String.valueOf(warnings));
    }

    @Test
    void itNeedsApiOnePointSevenOrNewer() {
        assertTrue(MapPeekPlugin.supported("1.7"));
        assertTrue(MapPeekPlugin.supported("1.12"));
        assertFalse(MapPeekPlugin.supported("1.6"));
        assertFalse(MapPeekPlugin.supported("2.0"));
        assertFalse(MapPeekPlugin.supported("x"));
    }
}
