package com.github.blade.mappeek;

import com.github.blade.hvac.api.WeatherStationView;

import java.util.Locale;
import java.util.Objects;

/**
 * A weather station as a map shows it: its coverage, a circle of the area it
 * measures, and what a viewer reads on hovering and clicking it. Pure, so
 * every map backend draws the same thing.
 *
 * @param label    its label
 * @param x        its sign's x
 * @param z        its sign's z
 * @param radius   how far each way it measures, in blocks
 * @param state    working, switched off, or damaged
 * @param hover    a line of plain text for hovering
 * @param details  HTML for clicking: the label, today's weather, wind and air, and its lightning strikes
 * @param wind     its wind right now as an arrow at its centre (0.2.0); calm when it has none to show
 * @param y        its sign's height, for 3D maps (0.2.0)
 */
public record StationMarker(String label, int x, int z, int radius, State state, String hover, String details,
                            WindArrow wind, int y) {

    /** What a station is doing, which colours its circle. */
    public enum State { WORKING, OFF, DAMAGED }

    public StationMarker {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(hover, "hover");
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(wind, "wind");
    }

    /** A station's marker, its temperatures in Celsius when {@code celsius}. */
    public static StationMarker of(WeatherStationView station, boolean celsius) {
        State state = !station.intact() ? State.DAMAGED : station.working() ? State.WORKING : State.OFF;
        String status = switch (state) {
            case WORKING -> "";
            case OFF -> " (off)";
            case DAMAGED -> " (damaged)";
        };
        StringBuilder details = new StringBuilder("<b>").append(escape(station.label())).append("</b>")
                .append(" <i>weather station</i>");
        if (state == State.WORKING) {
            WeatherStationView.ForecastDay today = station.forecast().get(0);
            details.append("<br>Today: ").append(sky(today.sky())).append(" · Hi ").append(degrees(today.highF(), celsius))
                    .append(" Lo ").append(degrees(today.lowF(), celsius));
            if (!today.note().isBlank()) details.append("<br>").append(escape(today.note()));
            // Its wind and air right now (0.2.0, EZvac API 1.8): shown only where EZvac gives them.
            if (station.wind().hasDirection())
                details.append("<br>Wind: ").append(WindArrow.describe(station.wind(), celsius));
            station.airQuality().ifPresent(air -> details.append("<br>Air: ").append(badge(air.aqi(), air.band()))
                    .append(" ").append(AirCell.bandName(air.band())));
            // The days ahead, a line each (0.2.0).
            int shown = 0;
            for (WeatherStationView.ForecastDay day : station.forecast()) {
                if (day.daysAhead() < 1 || shown == OUTLOOK_DAYS) continue;
                if (shown++ == 0) details.append("<br><i>Outlook</i>");
                details.append("<br>").append(day.daysAhead() == 1 ? "Tomorrow" : "In " + day.daysAhead() + " days")
                        .append(": ").append(sky(day.sky())).append(" · ").append(degrees(day.highF(), celsius))
                        .append("/").append(degrees(day.lowF(), celsius));
                day.wind().filter(wind -> wind.level() != com.github.blade.hvac.api.Wind.Level.CALM)
                        .ifPresent(wind -> details.append(" · ").append(WindArrow.name(wind.level()))
                                .append(wind.compass().isEmpty() ? "" : " " + wind.compass()));
                if (day.aqi() >= 0) details.append(" · ").append(badge(day.aqi(), AirCell.band(day.aqi())));
            }
        } else {
            details.append("<br>").append(state == State.OFF ? "Switched off" : "Damaged: rebuild its tower");
        }
        details.append("<br>Measures ").append(station.areaRadius()).append(" blocks around");
        long strikes = station.lightningStrikes();
        if (strikes > 0) details.append("<br>Struck by lightning ").append(strikes)
                .append(strikes == 1 ? " time" : " times");
        int x = station.position().x(), z = station.position().z();
        WindArrow wind = WindArrow.of(state == State.WORKING ? station.wind() : com.github.blade.hvac.api.Wind.CALM,
                x + 0.5, z + 0.5, celsius);
        return new StationMarker(station.label(), x, z, station.areaRadius(), state,
                station.label() + " · weather station" + status, details.toString(), wind, station.position().y());
    }

    /** How many days ahead a station's popup gives (0.2.0). */
    static final int OUTLOOK_DAYS = 3;

    /** {@code AQI 42} on its band's colour, in dark or light text to stand out (0.2.0). */
    static String badge(int aqi, com.github.blade.hvac.api.AirQuality.Band band) {
        java.awt.Color color = AirCell.color(band);
        boolean light = color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114 > 120;
        return String.format(Locale.US, "<span style=\"background:#%06X;color:%s;padding:0 4px;border-radius:3px\">"
                + "AQI %d</span>", color.getRGB() & 0xFFFFFF, light ? "#000" : "#fff", aqi);
    }

    /** {@code 60°}: whole degrees, as a station's own screen shows them. */
    static String degrees(double fahrenheit, boolean celsius) {
        double value = celsius ? (fahrenheit - 32.0) * 5.0 / 9.0 : fahrenheit;
        return String.format(Locale.US, "%.0f°", value);
    }

    static String sky(WeatherStationView.Sky sky) {
        return switch (sky) {
            case CLEAR -> "Clear";
            case RAIN -> "Rain";
            case SNOW -> "Snow";
            case THUNDER -> "Thunder";
        };
    }

    /** Text as HTML: a label is the admin's, and a map's popup shows HTML. */
    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
