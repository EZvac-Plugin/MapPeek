package com.github.blade.mappeek;

import com.github.blade.hvac.api.Wind;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.Color;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * MapPeek's config.yml. A value out of range keeps its default, with a
 * warning. The /mappeek menu (0.2.0) changes config.yml and loads it again.
 *
 * @param refreshSeconds     how often stations, the air and the season are brought up to date
 * @param windRefreshSeconds how often the wind is, arrows on stations included (0.2.0)
 * @param celsius            temperatures in °C and wind speeds in km/h; °F and mph otherwise
 * @param stations           whether weather stations are drawn
 * @param layerName          the stations layer's name in the map's layer list
 * @param hiddenByDefault    whether that layer starts hidden
 * @param working            a working station's colour
 * @param off                a switched-off station's colour
 * @param damaged            a damaged station's colour
 * @param fillOpacity        how solid a station's circle is filled, 0 to 1
 * @param stationArrows      whether each station shows its wind as an arrow (0.2.0)
 * @param breezy             a breezy wind's arrow colour (0.2.0)
 * @param windy              a windy one's
 * @param gale               a gale's
 * @param windLayer          the wind across the map (0.2.0)
 * @param airLayer           the air quality across the map (0.2.0)
 * @param clock              whether the layer list shows each world's time of day and season (0.2.0)
 */
public record Settings(int refreshSeconds, int windRefreshSeconds, boolean celsius, boolean stations,
                       String layerName, boolean hiddenByDefault, Color working, Color off, Color damaged,
                       double fillOpacity, boolean stationArrows, Color breezy, Color windy, Color gale,
                       Grid windLayer, Grid airLayer, boolean clock) {

    /** No layer draws more than this many arrows or squares: past it, they are spread further apart. */
    public static final int MAXIMUM_POINTS = 2500;
    /** The refresh periods the menu steps through, in seconds. */
    public static final int[] REFRESH_STEPS = {10, 15, 30, 60, 120, 300};
    public static final int[] WIND_REFRESH_STEPS = {5, 10, 15, 30, 60};

    /** Where a layer's grid goes (0.2.0). */
    public enum Area {
        /** Everywhere the world has been explored: every region of it saved so far. */
        EXPLORED,
        /** A square out to {@code radius-blocks} around spawn. */
        SPAWN;

        static Area parse(String text) {
            return text != null && text.trim().equalsIgnoreCase("spawn") ? SPAWN : EXPLORED;
        }
    }

    /**
     * A layer drawn on a grid (0.2.0).
     *
     * @param enabled         whether it is drawn at all
     * @param name            its name in the map's layer list
     * @param hiddenByDefault whether it starts hidden until a viewer ticks it
     * @param spacingBlocks   how far apart its arrows or squares are
     * @param radiusBlocks    with {@link Area#SPAWN}, how far from spawn it reaches each way
     * @param opacity         how solid its shapes are, 0 to 1
     * @param area            the whole explored map, or around spawn
     */
    public record Grid(boolean enabled, String name, boolean hiddenByDefault, int spacingBlocks, int radiusBlocks,
                       double opacity, Area area) {
        public Grid {
            area = area == null ? Area.EXPLORED : area;
        }

        /** Around spawn, as before the explored map could be covered. */
        public Grid(boolean enabled, String name, boolean hiddenByDefault, int spacingBlocks, int radiusBlocks,
                    double opacity) {
            this(enabled, name, hiddenByDefault, spacingBlocks, radiusBlocks, opacity, Area.SPAWN);
        }

        /**
         * The spacing actually used around spawn: the setting's, or wider
         * where that would draw more than {@link #MAXIMUM_POINTS} across the radius.
         */
        public int spacing() {
            int across = 2 * radiusBlocks / spacingBlocks + 1;
            int most = (int) Math.floor(Math.sqrt(MAXIMUM_POINTS));
            if (across <= most) return spacingBlocks;
            return (int) Math.ceil(2.0 * radiusBlocks / (most - 1));
        }

        /**
         * Where its arrows or squares go around spawn: the centres of a grid
         * {@link #spacing()} apart, lined up on whole multiples of it so they
         * stay put as spawn moves a little, out to the radius around {@code (x, z)}.
         */
        public java.util.List<double[]> points(double x, double z) {
            int step = spacing();
            long middleX = Math.round(x / step), middleZ = Math.round(z / step);
            int reach = radiusBlocks / step;
            java.util.List<double[]> points = new java.util.ArrayList<>();
            for (long i = -reach; i <= reach; i++)
                for (long j = -reach; j <= reach; j++)
                    points.add(new double[]{(middleX + i) * (double) step, (middleZ + j) * (double) step});
            return points;
        }
    }

    public static Settings defaults() {
        return new Settings(60, 15, false, true, "Weather stations", false, new Color(0x4FA3FF),
                new Color(0x8A8F98), new Color(0xE0A030), 0.12, true, new Color(0x3FBF7F), new Color(0xE0A030),
                new Color(0xE04040),
                new Grid(true, "Wind", true, 192, 2048, 0.85, Area.EXPLORED),
                new Grid(true, "Air quality", true, 128, 2048, 0.35, Area.EXPLORED),
                true);
    }

    public static Settings load(ConfigurationSection config, Consumer<String> warn) {
        Settings d = defaults();
        int refresh = config.getInt("refresh-seconds", d.refreshSeconds);
        if (refresh < 10 || refresh > 3600) {
            warn.accept("refresh-seconds must be 10 to 3600; using " + d.refreshSeconds + ".");
            refresh = d.refreshSeconds;
        }
        int windRefresh = config.getInt("wind.refresh-seconds", d.windRefreshSeconds);
        if (windRefresh < 5 || windRefresh > 3600) {
            warn.accept("wind.refresh-seconds must be 5 to 3600; using " + d.windRefreshSeconds + ".");
            windRefresh = d.windRefreshSeconds;
        }
        String units = config.getString("units", "F").trim().toUpperCase(Locale.ROOT);
        if (!units.equals("F") && !units.equals("C")) warn.accept("units must be F or C; using F.");
        double opacity = opacity(config, "stations.fill-opacity", d.fillOpacity, warn);
        String name = config.getString("stations.layer-name", d.layerName);
        return new Settings(refresh, windRefresh, units.equals("C"), config.getBoolean("stations.enabled", true),
                name == null || name.isBlank() ? d.layerName : name.trim(),
                config.getBoolean("stations.hidden-by-default", false),
                color(config.getString("stations.colors.working"), d.working, "stations.colors.working", warn),
                color(config.getString("stations.colors.off"), d.off, "stations.colors.off", warn),
                color(config.getString("stations.colors.damaged"), d.damaged, "stations.colors.damaged", warn), opacity,
                config.getBoolean("wind.station-arrows", d.stationArrows),
                color(config.getString("wind.colors.breezy"), d.breezy, "wind.colors.breezy", warn),
                color(config.getString("wind.colors.windy"), d.windy, "wind.colors.windy", warn),
                color(config.getString("wind.colors.gale"), d.gale, "wind.colors.gale", warn),
                grid(config, "wind.layer", d.windLayer, warn), grid(config, "air.layer", d.airLayer, warn),
                config.getBoolean("clock.enabled", d.clock));
    }

    private static Grid grid(ConfigurationSection config, String path, Grid fallback, Consumer<String> warn) {
        int spacing = config.getInt(path + ".spacing-blocks", fallback.spacingBlocks());
        if (spacing < 32 || spacing > 8192) {
            warn.accept(path + ".spacing-blocks must be 32 to 8192; using " + fallback.spacingBlocks() + ".");
            spacing = fallback.spacingBlocks();
        }
        int radius = config.getInt(path + ".radius-blocks", fallback.radiusBlocks());
        if (radius < spacing || radius > 100_000) {
            warn.accept(path + ".radius-blocks must be at least its spacing and at most 100000; using "
                    + Math.max(spacing, fallback.radiusBlocks()) + ".");
            radius = Math.max(spacing, fallback.radiusBlocks());
        }
        String name = config.getString(path + ".layer-name", fallback.name());
        return new Grid(config.getBoolean(path + ".enabled", fallback.enabled()),
                name == null || name.isBlank() ? fallback.name() : name.trim(),
                config.getBoolean(path + ".hidden-by-default", fallback.hiddenByDefault()), spacing, radius,
                opacity(config, path + ".opacity", fallback.opacity(), warn),
                Area.parse(config.getString(path + ".area", fallback.area().name())));
    }

    private static double opacity(ConfigurationSection config, String path, double fallback, Consumer<String> warn) {
        double opacity = config.getDouble(path, fallback);
        if (opacity >= 0.0 && opacity <= 1.0) return opacity;
        warn.accept(path + " must be 0 to 1; using " + fallback + ".");
        return fallback;
    }

    /** {@code #RRGGBB}; anything else keeps the default. */
    static Color color(String text, Color fallback, String which, Consumer<String> warn) {
        if (text == null) return fallback;
        String hex = text.trim();
        if (hex.matches("#[0-9A-Fa-f]{6}")) return new Color(Integer.parseInt(hex.substring(1), 16));
        warn.accept(which + " must be #RRGGBB; using the default.");
        return fallback;
    }

    /**
     * The first of {@code steps} (ascending) above {@code current}, or with
     * {@code back} the last below it, wrapping round at either end.
     */
    static int step(int[] steps, int current, boolean back) {
        if (back) {
            for (int i = steps.length - 1; i >= 0; i--) if (steps[i] < current) return steps[i];
            return steps[steps.length - 1];
        }
        for (int step : steps) if (step > current) return step;
        return steps[0];
    }

    public Color color(StationMarker.State state) {
        return switch (state) {
            case WORKING -> working;
            case OFF -> off;
            case DAMAGED -> damaged;
        };
    }

    /** A wind arrow's colour; a calm spot's dot takes a switched-off station's grey. */
    public Color color(Wind.Level level) {
        return switch (level) {
            case CALM -> off;
            case BREEZY -> breezy;
            case WINDY -> windy;
            case GALE -> gale;
        };
    }
}
