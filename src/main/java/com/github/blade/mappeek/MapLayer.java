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

    /** Takes everything MapPeek drew off the map, when it stops. */
    void close();
}
