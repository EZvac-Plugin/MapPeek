package com.github.blade.mappeek;

import com.github.blade.hvac.api.AirQuality;
import com.github.blade.hvac.api.Position;
import com.github.blade.hvac.api.WeatherStationView;
import com.github.blade.hvac.api.Wind;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 0.2.0: layers over the whole explored map, the menu's steps, richer popups, icons. */
class MapWideTest {
    private static Set<Long> square(int from, int to) {
        Set<Long> regions = new HashSet<>();
        for (int x = from; x <= to; x++) for (int z = from; z <= to; z++) regions.add(Coverage.key(x, z));
        return regions;
    }

    @Test
    void theGridCoversEveryExploredRegionAndNothingElse() {
        Set<Long> regions = Set.of(Coverage.key(0, 0), Coverage.key(-1, 0), Coverage.key(5, -3));
        Coverage.Points grid = Coverage.points(regions, 128, Settings.MAXIMUM_POINTS);
        assertEquals(128, grid.spacing());
        assertEquals(3 * 16, grid.points().size(), "4 by 4 squares in each region");
        for (double[] point : grid.points()) {
            long region = Coverage.key(Math.floorDiv((int) point[0], 512), Math.floorDiv((int) point[1], 512));
            assertTrue(regions.contains(region), "only over explored land: " + point[0] + ", " + point[1]);
            assertEquals(0.0, Math.floorMod((long) point[0], 128), 1e-9, "lined up on the spacing");
        }
        assertEquals(Coverage.regionX(Coverage.key(-7, 9)), -7);
        assertEquals(Coverage.regionZ(Coverage.key(-7, 9)), 9);
    }

    @Test
    void aBigMapSpreadsTheGridToStayUnderTheCap() {
        Coverage.Points grid = Coverage.points(square(-10, 10), 128, Settings.MAXIMUM_POINTS);
        assertTrue(grid.points().size() <= Settings.MAXIMUM_POINTS, grid.points().size() + " points");
        assertTrue(grid.spacing() > 128 && grid.spacing() % 128 == 0, "a whole multiple wider: " + grid.spacing());
        assertTrue(grid.points().size() > Settings.MAXIMUM_POINTS / 4, "but not needlessly sparse");
    }

    @Test
    void theMenuStepsThroughTheRefreshPeriods() {
        assertEquals(120, Settings.step(Settings.REFRESH_STEPS, 60, false));
        assertEquals(30, Settings.step(Settings.REFRESH_STEPS, 60, true));
        assertEquals(10, Settings.step(Settings.REFRESH_STEPS, 300, false), "wraps round");
        assertEquals(300, Settings.step(Settings.REFRESH_STEPS, 10, true));
        assertEquals(60, Settings.step(Settings.REFRESH_STEPS, 45, false), "an odd value goes to the next step");
        assertEquals(30, Settings.step(Settings.REFRESH_STEPS, 45, true));
        assertEquals(30, Settings.step(Settings.WIND_REFRESH_STEPS, 15, false));
    }

    @Test
    void settingsReadTheWindRefreshAndTheArea() {
        Settings stock = Settings.defaults();
        assertEquals(15, stock.windRefreshSeconds());
        assertEquals(Settings.Area.EXPLORED, stock.windLayer().area());
        YamlConfiguration config = new YamlConfiguration();
        config.set("wind.refresh-seconds", 2);
        config.set("air.layer.area", "spawn");
        List<String> warnings = new java.util.ArrayList<>();
        Settings read = Settings.load(config, warnings::add);
        assertEquals(15, read.windRefreshSeconds(), "too often: the default");
        assertEquals(1, warnings.size());
        assertEquals(Settings.Area.SPAWN, read.airLayer().area());
        assertEquals(Settings.Area.EXPLORED, read.windLayer().area());
    }

    @Test
    void aStationsPopupHasAnAqiBadgeAndTheDaysAhead() {
        UUID world = UUID.randomUUID();
        List<WeatherStationView.ForecastDay> days = List.of(
                new WeatherStationView.ForecastDay(0, 74, 55, WeatherStationView.Sky.CLEAR, 0.1, 0, Optional.empty(), 30, "", 42),
                new WeatherStationView.ForecastDay(1, 70, 52, WeatherStationView.Sky.RAIN, 0.8, 0.1,
                        Optional.of(new Wind.Day(2, Wind.Level.WINDY, 9, 225)), 30, "", 20),
                new WeatherStationView.ForecastDay(2, 72, 54, WeatherStationView.Sky.CLEAR, 0.1, 0,
                        Optional.of(new Wind.Day(3, Wind.Level.CALM, 1, 270)), 30, "", 160));
        AirQuality air = new AirQuality(10, 20, 50, 42, AirQuality.Band.GOOD, AirQuality.Pollutant.PM25,
                AirQuality.PollenLevel.LOW);
        StationMarker marker = StationMarker.of(new WeatherStationView("Harbour", new Position(world, 1, 70, 2), true,
                true, 0, 70, days, 100, 20, new Wind(4, Wind.Level.BREEZY, 270), Optional.of(air)), false);
        String details = marker.details();
        assertTrue(details.contains("background:#00E400") && details.contains("AQI 42</span> Good"), details);
        assertTrue(details.contains("<br>Tomorrow: Rain · 70°/52° · Windy SW · "), details);
        assertTrue(details.contains("<br>In 2 days: Clear · 72°/54° · <span"), "a calm day names no wind: " + details);
        assertTrue(details.contains("background:#FF0000;color:#fff"), "an unhealthy day's badge is red, white text");
        assertEquals(70, marker.y());
        assertEquals(AirQuality.Band.MODERATE, AirCell.band(51));
        assertEquals(AirQuality.Band.HAZARDOUS, AirCell.band(301));
    }

    @Test
    void theWindAnimationGoesInSquaremapsPageOnceAndComesOutAgain() {
        String page = "<!doctype html>\n<html>\n    <head>\n        <title></title>\n"
                + "      <script type=\"module\" src=\"./assets/index.js\"></script>\n    </head>\n</html>";
        String with = WindFlow.inject(page);
        assertTrue(with.indexOf(WindFlow.TAG) < with.indexOf("type=\"module\""), "before squaremap's own script");
        assertEquals(with, WindFlow.inject(with), "only once");
        assertEquals(page, WindFlow.remove(with), "and out again, leaving the page as it was");
        assertEquals("<html>no head</html>", WindFlow.inject("<html>no head</html>"));
    }

    @Test
    void theWindFieldGivesEachPointsWindTowardEastAndSouth() {
        String json = WindFlow.field(List.of(
                WindArrow.of(new Wind(10, Wind.Level.WINDY, 270), 192, -384, false),   // from the W: toward the east
                WindArrow.of(new Wind(0.5, Wind.Level.CALM, Double.NaN), 0, 0, false)),
                192, new String[]{"#8A8F98", "#3FBF7F", "#E0A030", "#E04040"}, 63);
        assertTrue(json.startsWith("{\"spacing\":192,\"y\":63,\"colors\":[\"#8A8F98\",\"#3FBF7F\",\"#E0A030\",\"#E04040\"]"), json);
        assertTrue(json.contains("[192,-384,10.00,0.00,2]"), json);
        assertTrue(json.contains("[0,0,0.00,0.00,0]"), "no direction, no flow: " + json);
    }

    @Test
    void stationIconsAreDrawnInTheirStatesColour() {
        BufferedImage icon = StationIcon.image(new java.awt.Color(0x4FA3FF));
        assertEquals(StationIcon.SIZE, icon.getWidth());
        assertEquals(0x4FA3FF, icon.getRGB(6, 15) & 0xFFFFFF, "the disc is the station's colour");
        assertEquals(0, icon.getRGB(0, 0) >>> 24, "the corners are clear");
        byte[] png = StationIcon.png(new java.awt.Color(0x8A8F98));
        assertEquals((byte) 0x89, png[0], "a PNG for BlueMap");
    }
}
