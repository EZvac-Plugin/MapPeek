package com.github.blade.mappeek;

import org.bukkit.configuration.ConfigurationSection;

import java.awt.Color;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * MapPeek's config.yml. A value out of range keeps its default, with a
 * warning.
 *
 * @param refreshSeconds  how often the map is brought up to date
 * @param celsius         temperatures in a station's details in °C
 * @param stations        whether weather stations are drawn
 * @param layerName       the stations layer's name in the map's layer list
 * @param hiddenByDefault whether that layer starts hidden
 * @param working         a working station's colour
 * @param off             a switched-off station's colour
 * @param damaged         a damaged station's colour
 * @param fillOpacity     how solid a station's circle is filled, 0 to 1
 */
public record Settings(int refreshSeconds, boolean celsius, boolean stations, String layerName, boolean hiddenByDefault,
                       Color working, Color off, Color damaged, double fillOpacity) {

    public static Settings defaults() {
        return new Settings(60, false, true, "Weather stations", false, new Color(0x4FA3FF), new Color(0x8A8F98),
                new Color(0xE0A030), 0.12);
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
        double opacity = config.getDouble("stations.fill-opacity", d.fillOpacity);
        if (!(opacity >= 0.0 && opacity <= 1.0)) {
            warn.accept("stations.fill-opacity must be 0 to 1; using " + d.fillOpacity + ".");
            opacity = d.fillOpacity;
        }
        String name = config.getString("stations.layer-name", d.layerName);
        return new Settings(refresh, units.equals("C"), config.getBoolean("stations.enabled", true),
                name == null || name.isBlank() ? d.layerName : name.trim(),
                config.getBoolean("stations.hidden-by-default", false),
                color(config.getString("stations.colors.working"), d.working, "working", warn),
                color(config.getString("stations.colors.off"), d.off, "off", warn),
                color(config.getString("stations.colors.damaged"), d.damaged, "damaged", warn), opacity);
    }

    /** {@code #RRGGBB}; anything else keeps the default. */
    static Color color(String text, Color fallback, String which, Consumer<String> warn) {
        if (text == null) return fallback;
        String hex = text.trim();
        if (hex.matches("#[0-9A-Fa-f]{6}")) return new Color(Integer.parseInt(hex.substring(1), 16));
        warn.accept("stations.colors." + which + " must be #RRGGBB; using the default.");
        return fallback;
    }

    public Color color(StationMarker.State state) {
        return switch (state) {
            case WORKING -> working;
            case OFF -> off;
            case DAMAGED -> damaged;
        };
    }
}
