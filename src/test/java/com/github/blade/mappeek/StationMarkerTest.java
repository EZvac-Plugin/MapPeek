package com.github.blade.mappeek;

import com.github.blade.hvac.api.AirQuality;
import com.github.blade.hvac.api.Position;
import com.github.blade.hvac.api.WeatherStationView;
import com.github.blade.hvac.api.Wind;
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
    void itNeedsApiOnePointEightOrNewer() {
        // 0.2.0: the wind's direction and the air anywhere came in API 1.8 (EZvac BETA 1.0 Pilot 3).
        assertTrue(MapPeekPlugin.supported("1.8"));
        assertTrue(MapPeekPlugin.supported("1.12"));
        assertFalse(MapPeekPlugin.supported("1.7"));
        assertFalse(MapPeekPlugin.supported("2.0"));
        assertFalse(MapPeekPlugin.supported("x"));
    }

    @Test
    void aWorkingStationShowsItsWindAndAir() {
        // 0.2.0.
        List<WeatherStationView.ForecastDay> forecast = List.of(new WeatherStationView.ForecastDay(0, 74.0, 55.0,
                WeatherStationView.Sky.CLEAR, 0.2, 0.0, Optional.empty(), 30.0, "", 42));
        AirQuality air = new AirQuality(10.0, 20.0, 50.0, 42, AirQuality.Band.GOOD, AirQuality.Pollutant.PM25,
                AirQuality.PollenLevel.LOW);
        WeatherStationView view = new WeatherStationView("Harbour", new Position(WORLD, 631, -58, -20), true, true, 0,
                70.0, forecast, 100, 20, new Wind(8.0, Wind.Level.WINDY, 270.0), Optional.of(air));
        StationMarker marker = StationMarker.of(view, true);
        assertTrue(marker.details().contains("<br>Wind: Windy 29 km/h from the W<br>Air: AQI 42 (Good)"),
                marker.details());
        assertTrue(marker.wind().drawn());
        assertEquals(90.0, marker.wind().towardDegrees(), 1e-9, "a west wind blows toward the east");
        StationMarker off = StationMarker.of(station("Hill", false, true, 0, ""), false);
        assertFalse(off.wind().drawn(), "a station that's off has no arrow");
        assertFalse(off.details().contains("Wind:"));
    }

    @Test
    void anArrowPointsWhereTheWindBlowsAndGrowsWithIt() {
        WindArrow west = WindArrow.of(new Wind(16.0, Wind.Level.GALE, 270.0), 100, 200, false);
        List<double[]> outline = west.outline(80);
        assertEquals(7, outline.size(), "a shaft and a head");
        double[] tip = outline.get(3);
        assertEquals(140.0, tip[0], 1e-9, "a gale's arrow is the longest: its tip 40 blocks east");
        assertEquals(200.0, tip[1], 1e-9);
        assertEquals("Gale 36 mph from the W", west.tooltip());
        double[] northTip = WindArrow.of(new Wind(16.0, Wind.Level.GALE, 180.0), 0, 0, false).outline(80).get(3);
        assertEquals(-40.0, northTip[1], 1e-9, "from the south it points north, toward -z");
        double breezyTip = WindArrow.of(new Wind(4.0, Wind.Level.BREEZY, 270.0), 0, 0, false).outline(80).get(3)[0];
        assertTrue(breezyTip > 0 && breezyTip < 40, "a breeze's is shorter: " + breezyTip);
        assertTrue(WindArrow.of(new Wind(1.0, Wind.Level.CALM, 270.0), 0, 0, false).outline(80).isEmpty(), "calm");
        assertTrue(WindArrow.of(Wind.CALM, 0, 0, false).outline(80).isEmpty(), "no wind");
    }

    @Test
    void airIsShadedInTheEpasColours() {
        AirQuality smoky = new AirQuality(60.0, 80.0, 10.0, 153, AirQuality.Band.UNHEALTHY, AirQuality.Pollutant.PM25,
                AirQuality.PollenLevel.MODERATE);
        AirCell cell = AirCell.of(smoky, 128, -256, 256);
        assertEquals(new Color(0xFF0000), cell.color());
        assertEquals("AQI 153 (Unhealthy) · Pollen moderate", cell.tooltip());
        assertEquals(new Color(0x00E400), AirCell.color(AirQuality.Band.GOOD));
        assertEquals(new Color(0x7E0023), AirCell.color(AirQuality.Band.HAZARDOUS));
    }

    @Test
    void aLayerDrawsAtMostTwentyFiveHundredPoints() {
        Settings.Grid wide = new Settings.Grid(true, "Wind", true, 64, 8192, 0.8);
        List<double[]> points = wide.points(10, -10);
        assertTrue(points.size() <= Settings.MAXIMUM_POINTS, points.size() + " points");
        assertTrue(wide.spacing() > 64, "spread further apart: " + wide.spacing());
        Settings.Grid stock = Settings.defaults().windLayer();
        assertEquals(192, stock.spacing());
        assertEquals(441, stock.points(0, 0).size(), "the default: 192 apart, out to 2,048 blocks: 21 by 21");
        assertEquals(1089, Settings.defaults().airLayer().points(0, 0).size(), "33 by 33 squares of air, 128 across");
        double[] first = stock.points(300, 300).get(0);
        assertEquals(0.0, first[0] % 192, 1e-9, "lined up on whole multiples of the spacing");
    }
}
