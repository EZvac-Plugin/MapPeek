package com.github.blade.mappeek;

import com.github.blade.hvac.api.AirQuality;

import java.awt.Color;
import java.util.Locale;
import java.util.Objects;

/**
 * The outdoor air of one square of the map (0.2.0), shaded in its AQI band's
 * colour as the US EPA draws air quality maps. Pure, so every map backend
 * draws the same thing.
 *
 * @param x       the square's centre x
 * @param z       the square's centre z
 * @param size    how many blocks across it is
 * @param aqi     its AQI, 0 to 500
 * @param band    the AQI's band, which colours it
 * @param tooltip a line of plain text: {@code AQI 42 (Good) · Pollen low}
 */
public record AirCell(double x, double z, int size, int aqi, AirQuality.Band band, String tooltip) {
    public AirCell {
        Objects.requireNonNull(band, "band");
        Objects.requireNonNull(tooltip, "tooltip");
    }

    public static AirCell of(AirQuality air, double x, double z, int size) {
        return new AirCell(x, z, size, air.aqi(), air.band(), "AQI " + air.aqi() + " (" + bandName(air.band())
                + ") · Pollen " + pollenName(air.pollenLevel()));
    }

    /** The EPA's colour for a band. */
    public static Color color(AirQuality.Band band) {
        return switch (band) {
            case GOOD -> new Color(0x00E400);
            case MODERATE -> new Color(0xFFFF00);
            case UNHEALTHY_FOR_SENSITIVE_GROUPS -> new Color(0xFF7E00);
            case UNHEALTHY -> new Color(0xFF0000);
            case VERY_UNHEALTHY -> new Color(0x8F3F97);
            case HAZARDOUS -> new Color(0x7E0023);
        };
    }

    public Color color() { return color(band); }

    static String bandName(AirQuality.Band band) {
        return switch (band) {
            case GOOD -> "Good";
            case MODERATE -> "Moderate";
            case UNHEALTHY_FOR_SENSITIVE_GROUPS -> "Unhealthy for sensitive groups";
            case UNHEALTHY -> "Unhealthy";
            case VERY_UNHEALTHY -> "Very unhealthy";
            case HAZARDOUS -> "Hazardous";
        };
    }

    static String pollenName(AirQuality.PollenLevel level) {
        String lower = level.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return lower;
    }
}
