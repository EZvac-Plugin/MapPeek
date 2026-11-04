package com.github.blade.mappeek;

import com.github.blade.hvac.api.Wind;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.Color;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * MapPeek's config.yml. A value out of range keeps its default, with a
 * warning.
 *
 * @param refreshSeconds  how often the map is brought up to date
 * @param celsius         temperatures in a station's details in °C, and wind speeds in km/h (mph with °F)
 * @param stations        whether weather stations are drawn
 * @param layerName       the stations layer's name in the map's layer list
 * @param hiddenByDefault whether that layer starts hidden
 * @param working         a working station's colour
 * @param off             a switched-off station's colour
 * @param damaged         a damaged station's colour
 * @param fillOpacity     how solid a station's circle is filled, 0 to 1
 * @param stationArrows   whether each station shows its wind as an arrow (0.2.0)
 * @param breezy          a breezy wind's arrow colour (0.2.0)
 * @param windy           a windy one's
 * @param gale            a gale's
 * @param windLayer       the wind across the map (0.2.0)
 * @param airLayer        the air quality across the map (0.2.0)
 * @param clock           whether the layer list shows each world's time of day and season (0.2.0)
 */
public record Settings(int refreshSeconds, boolean celsius, boolean stations, String layerName, boolean hiddenByDefault,
                       Color working, Color off, Color damaged, double fillOpacity, boolean stationArrows,
                       Color breezy, Color windy, Color gale, Grid windLayer, Grid airLayer, boolean clock) {

    /** No layer draws more than this many arrows or squares: past it, they are spread further apart. */
    public static final int MAXIMUM_POINTS = 2500;

    /**
     * A layer drawn on a grid around each world's spawn (0.2.0).
     *
     * @param enabled         whether it is drawn at all
     * @param name            its name in the map's layer list
     * @param hiddenByDefault whether it starts hidden until a viewer ticks it
     * @param spacingBlocks   how far apart its arrows or squares are
     * @param radiusBlocks    how far from spawn it reaches each way
     * @param opacity         how solid its shapes are, 0 to 1
     */
    public record Grid(boolean enabled, String name, boolean hiddenByDefault, int spacingBlocks, int radiusBlocks,
                       double opacity) {
        /**
         * The spacing actually used: the setting's, or wider where that
         * would draw more than {@link #MAXIMUM_POINTS} across the radius.
         */
        public int spacing() {
            int across = 2 * radiusBlocks / spacingBlocks + 1;
            int most = (int) Math.floor(Math.sqrt(MAXIMUM_POINTS));
            if (across <= most) return spacingBlocks;
            return (int) Math.ceil(2.0 * radiusBlocks / (most - 1));
        }

        /**
         * Where its arrows or squares go: the centres of a grid {@link #spacing()}
         * apart, lined up on whole multiples of it so they stay put as spawn
         * moves a little, out to the radius around {@code (x, z)}.
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
        return new Settings(60, false, true, "Weather stations", false, new Color(0x4FA3FF), new Color(0x8A8F98),
                new Color(0xE0A030), 0.12, true, new Color(0x3FBF7F), new Color(0xE0A030), new Color(0xE04040),
                new Grid(true, "Wind", true, 192, 2048, 0.85), new Grid(true, "Air quality", true, 128, 2048, 0.35),
                true);
    }

    public static Settings load(ConfigurationSection config, Consumer<String> warn) {
        Settings d = defaults();
        int refresh = config.getInt("refresh-seconds", d.refreshSeconds);
        if (refresh < 10) {
            warn.accept("refresh-seconds must be 10 or more; using " + d.refreshSeconds + ".");
            refresh = d.refreshSeconds;
        }
        String units = config.getString("units", "F").trim().toUpperCase(Locale.ROOT);
        if (!units.equals("F") && !units.equals("C")) warn.accept("units must be F or C; using F.");
        double opacity = opacity(config, "stations.fill-opacity", d.fillOpacity, warn);
        String name = config.getString("stations.layer-name", d.layerName);
        return new Settings(refresh, units.equals("C"), config.getBoolean("stations.enabled", true),
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
                opacity(config, path + ".opacity", fallback.opacity(), warn));
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

    public Color color(StationMarker.State state) {
        return switch (state) {
            case WORKING -> working;
            case OFF -> off;
            case DAMAGED -> damaged;
        };
    }

    /** A wind arrow's colour; a calm spot has no arrow, so it takes a switched-off station's grey. */
    public Color color(Wind.Level level) {
        return switch (level) {
            case CALM -> off;
            case BREEZY -> breezy;
            case WINDY -> windy;
            case GALE -> gale;
        };
    }
}
