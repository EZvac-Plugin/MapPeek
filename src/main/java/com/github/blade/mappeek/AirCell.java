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
                + ") · PM2.5 " + Math.round(air.pm25()) + " · PM10 " + Math.round(air.pm10()) + " µg/m³ · Pollen "
                + pollenName(air.pollenLevel()));
    }

    /** The US EPA band an AQI falls in: Good to 50, Moderate to 100, and so on. */
    public static AirQuality.Band band(int aqi) {
        if (aqi <= 50) return AirQuality.Band.GOOD;
        if (aqi <= 100) return AirQuality.Band.MODERATE;
        if (aqi <= 150) return AirQuality.Band.UNHEALTHY_FOR_SENSITIVE_GROUPS;
        if (aqi <= 200) return AirQuality.Band.UNHEALTHY;
        if (aqi <= 300) return AirQuality.Band.VERY_UNHEALTHY;
        return AirQuality.Band.HAZARDOUS;
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

    /**
     * The square's shade: the EPA's colours on a smooth scale, each at the
     * middle of its band and blended between, so neighbouring squares a few
     * points apart look alike instead of flipping at a band's edge (0.2.0).
     * Popups keep the exact band colour.
     */
    public Color color() { return shade(aqi); }

    private static final int[] SHADE_AT = {25, 75, 125, 175, 250, 400};
    private static final int[] SHADES = {0x00E400, 0xFFFF00, 0xFF7E00, 0xFF0000, 0x8F3F97, 0x7E0023};

    /** An AQI on the smooth scale. */
    public static Color shade(int aqi) {
        if (aqi <= SHADE_AT[0]) return new Color(SHADES[0]);
        for (int i = 1; i < SHADE_AT.length; i++) {
            if (aqi > SHADE_AT[i]) continue;
            double t = (aqi - SHADE_AT[i - 1]) / (double) (SHADE_AT[i] - SHADE_AT[i - 1]);
            Color a = new Color(SHADES[i - 1]), b = new Color(SHADES[i]);
            return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
        }
        return new Color(SHADES[SHADES.length - 1]);
    }

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
