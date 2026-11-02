package com.github.blade.mappeek;

import org.bukkit.World;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Weather stations on squaremap: a layer in each world's map with a circle
 * for every station's coverage, coloured by what it is doing.
 */
final class SquaremapLayer implements MapLayer {
    private static final Key LAYER = Key.of("mappeek_weather_stations");

    private final Squaremap squaremap = SquaremapProvider.get();
    private final Settings settings;
    private final Map<UUID, SimpleLayerProvider> layers = new HashMap<>();

    SquaremapLayer(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "squaremap";
    }

    @Override
    public void stations(World world, List<StationMarker> markers) {
        Optional<MapWorld> map = squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world));
        if (map.isEmpty()) return;
        SimpleLayerProvider layer = layers.computeIfAbsent(world.getUID(), id -> {
            SimpleLayerProvider created = SimpleLayerProvider.builder(settings.layerName())
                    .showControls(true).defaultHidden(settings.hiddenByDefault()).layerPriority(10).zIndex(250)
                    .build();
            if (map.get().layerRegistry().hasEntry(LAYER)) map.get().layerRegistry().unregister(LAYER);
            map.get().layerRegistry().register(LAYER, created);
            return created;
        });
        layer.clearMarkers();
        for (StationMarker station : markers) {
            Color color = settings.color(station.state());
            Marker circle = Marker.circle(Point.of(station.x() + 0.5, station.z() + 0.5), station.radius());
            circle.markerOptions(MarkerOptions.builder()
                    .strokeColor(color).strokeWeight(2).strokeOpacity(0.9)
                    .fillColor(color).fillOpacity(settings.fillOpacity())
                    .hoverTooltip(StationMarker.escape(station.hover()))
                    .clickTooltip(station.details())
                    .build());
            layer.addMarker(Key.of("station_" + station.x() + "_" + station.z()), circle);
        }
    }

    @Override
    public void close() {
        for (Map.Entry<UUID, SimpleLayerProvider> entry : layers.entrySet()) {
            World world = org.bukkit.Bukkit.getWorld(entry.getKey());
            if (world == null) continue;
            squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(map -> {
                if (map.layerRegistry().hasEntry(LAYER)) map.layerRegistry().unregister(LAYER);
            });
        }
        layers.clear();
    }
}
