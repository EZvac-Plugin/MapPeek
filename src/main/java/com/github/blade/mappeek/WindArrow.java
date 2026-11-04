package com.github.blade.mappeek;

import com.github.blade.hvac.api.Wind;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The wind at a spot as an arrow on the map (0.2.0): it points the way the
 * wind blows, longer the stronger it is. A calm spot has no arrow. Pure, so
 * every map backend draws the same thing.
 *
 * @param x             the spot's x
 * @param z             the spot's z
 * @param towardDegrees where it blows to: 0 north (-z), 90 east; NaN when calm or not known
 * @param level         how it reads
 * @param tooltip       a line of plain text: {@code Breezy 18 km/h from the W}
 */
public record WindArrow(double x, double z, double towardDegrees, Wind.Level level, String tooltip) {
    public WindArrow {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(tooltip, "tooltip");
    }

    /** A spot's wind; speeds in km/h when {@code celsius}, else mph. */
    public static WindArrow of(Wind wind, double x, double z, boolean celsius) {
        return new WindArrow(x, z, wind.towardDegrees(), wind.level(), describe(wind, celsius));
    }

    /** {@code Calm}, or {@code Breezy 18 km/h from the W}. */
    static String describe(Wind wind, boolean celsius) {
        String level = name(wind.level());
        if (wind.level() == Wind.Level.CALM) return level;
        String speed = celsius ? Math.round(wind.kmh()) + " km/h" : Math.round(wind.mph()) + " mph";
        return level + " " + speed + (wind.hasDirection() ? " from the " + wind.compass() : "");
    }

    static String name(Wind.Level level) {
        String lower = level.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /** Whether there is an arrow to draw: some wind, with a direction. */
    public boolean drawn() {
        return level != Wind.Level.CALM && Double.isFinite(towardDegrees);
    }

    /** How long the arrow is, as a share of the longest: a gale's is the longest. */
    public double share() {
        return switch (level) {
            case CALM -> 0.0;
            case BREEZY -> 0.55;
            case WINDY -> 0.8;
            case GALE -> 1.0;
        };
    }

    /**
     * The arrow's outline, centred on its spot, as {x, z} corners in order:
     * a shaft and a head, {@code longest} blocks long for a gale. Empty when
     * there is no arrow.
     */
    public List<double[]> outline(double longest) {
        if (!drawn() || !(longest > 0.0)) return List.of();
        double length = longest * share();
        double radians = Math.toRadians(towardDegrees);
        // Where it points, and across it; north is -z.
        double dx = Math.sin(radians), dz = -Math.cos(radians);
        double px = Math.cos(radians), pz = Math.sin(radians);
        double shaft = length * 0.12, head = length * 0.38, wide = length * 0.34;
        double tipX = x + dx * length / 2.0, tipZ = z + dz * length / 2.0;
        double tailX = x - dx * length / 2.0, tailZ = z - dz * length / 2.0;
        double neckX = tipX - dx * head, neckZ = tipZ - dz * head;
        return List.of(
                new double[]{tailX + px * shaft / 2.0, tailZ + pz * shaft / 2.0},
                new double[]{neckX + px * shaft / 2.0, neckZ + pz * shaft / 2.0},
                new double[]{neckX + px * wide / 2.0, neckZ + pz * wide / 2.0},
                new double[]{tipX, tipZ},
                new double[]{neckX - px * wide / 2.0, neckZ - pz * wide / 2.0},
                new double[]{neckX - px * shaft / 2.0, neckZ - pz * shaft / 2.0},
                new double[]{tailX - px * shaft / 2.0, tailZ - pz * shaft / 2.0});
    }
}
