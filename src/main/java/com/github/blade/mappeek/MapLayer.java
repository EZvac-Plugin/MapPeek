package com.github.blade.mappeek;

import org.bukkit.World;

import java.util.List;

/**
 * One web map MapPeek draws on. squaremap is the first; another map plugin
 * (BlueMap, Dynmap) only needs its own implementation of this.
 */
public interface MapLayer {
    /** The map plugin's name, for the log. */
    String name();

    /** Replaces the weather stations shown for a world. Main thread. */
    void stations(World world, List<StationMarker> markers);

    /**
     * Replaces the wind drawn across a world (0.2.0): arrows up to
     * {@code longest} blocks long. An empty list takes the layer away. Main thread.
     */
    void wind(World world, List<WindArrow> arrows, double longest);

    /** Replaces the air quality shaded across a world (0.2.0). An empty list takes the layer away. Main thread. */
    void air(World world, List<AirCell> cells);

    /**
     * Shows a world's time of day and its season (0.2.0): the clock keeps
     * time on its own, the season is the one given here ({@link ClockLabel#season}),
     * empty where there is none. Main thread.
     */
    void clock(World world, String season);

    /** Takes everything MapPeek drew off the map, when it stops. */
    void close();
}
