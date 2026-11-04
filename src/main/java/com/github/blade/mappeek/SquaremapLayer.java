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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * MapPeek on squaremap: in each world's map, a layer of weather stations (a
 * circle for each one's coverage, coloured by what it is doing, and since
 * 0.2.0 an arrow of its wind), and since 0.2.0 a layer of wind arrows and one
 * of air quality across the map, hidden until a viewer ticks them.
 */
final class SquaremapLayer implements MapLayer {
    private static final Key STATIONS = Key.of("mappeek_weather_stations");
    private static final Key WIND = Key.of("mappeek_wind");
    private static final Key AIR = Key.of("mappeek_air_quality");
    private static final Key CLOCK = Key.of("mappeek_clock");
    private static final Key FLOW = Key.of("mappeek_wind_flow");

    private final Squaremap squaremap = SquaremapProvider.get();
    private final Settings settings;
    /** Runs file work off the main thread, and warns in the log. */
    private final org.bukkit.plugin.Plugin plugin;
    /** Each world's layers MapPeek registered, by key. */
    private final Map<UUID, Map<Key, SimpleLayerProvider>> layers = new HashMap<>();
    /** Each world's clock, registered once. */
    private final Map<UUID, ClockProvider> clocks = new HashMap<>();

    /**
     * A layer with no markers whose name is the world's time of day and
     * season (0.2.0): squaremap reads the name each time it updates the map,
     * so the clock keeps time between MapPeek's refreshes. It sits at the top
     * of the layer list.
     */
    private static final class ClockProvider implements xyz.jpenilla.squaremap.api.LayerProvider {
        private final World world;
        private volatile String season = "";

        ClockProvider(World world) {
            this.world = world;
        }

        @Override
        public String getLabel() {
            // squaremap may ask from its own thread: the time of day is a plain read.
            return ClockLabel.label(world.getTime(), season);
        }

        @Override
        public boolean showControls() { return true; }

        @Override
        public boolean defaultHidden() { return false; }

        @Override
        public int layerPriority() { return 0; }

        @Override
        public java.util.Collection<Marker> getMarkers() { return List.of(); }
    }

    SquaremapLayer(Settings settings, org.bukkit.plugin.Plugin plugin) {
        this.settings = settings;
        this.plugin = plugin;
        // The wind animation's script goes in squaremap's page only while it's on (0.2.0).
        java.nio.file.Path web = squaremap.webDir();
        async(() -> {
            if (settings.windAnimation()) WindFlow.install(web, plugin.getLogger()::warning);
            else WindFlow.uninstall(web);
        });
        // The station icons, one per state (0.2.0).
        for (StationMarker.State state : StationMarker.State.values()) {
            Key key = icon(state);
            if (squaremap.iconRegistry().hasEntry(key)) squaremap.iconRegistry().unregister(key);
            squaremap.iconRegistry().register(key, StationIcon.image(settings.color(state)));
        }
    }

    private static Key icon(StationMarker.State state) {
        return Key.of("mappeek_station_" + state.name().toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public void clock(World world, String season) {
        Optional<MapWorld> map = squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world));
        if (map.isEmpty()) return;
        clocks.computeIfAbsent(world.getUID(), id -> {
            ClockProvider created = new ClockProvider(world);
            if (map.get().layerRegistry().hasEntry(CLOCK)) map.get().layerRegistry().unregister(CLOCK);
            map.get().layerRegistry().register(CLOCK, created);
            return created;
        }).season = season == null ? "" : season;
    }

    @Override
    public String name() {
        return "squaremap";
    }

    @Override
    public void stations(World world, List<StationMarker> markers) {
        SimpleLayerProvider layer = layer(world, STATIONS, settings.layerName(), settings.hiddenByDefault(), 10, 250);
        if (layer == null) return;
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
            String id = station.x() + "_" + station.z();
            layer.addMarker(Key.of("station_" + id), circle);
            // Its icon at the centre, easy to spot and to click (0.2.0).
            Marker icon = Marker.icon(Point.of(station.x() + 0.5, station.z() + 0.5), icon(station.state()),
                    StationIcon.SIZE - 8);
            icon.markerOptions(MarkerOptions.builder()
                    .hoverTooltip(StationMarker.escape(station.hover()))
                    .clickTooltip(station.details())
                    .build());
            layer.addMarker(Key.of("station_icon_" + id), icon);
            if (settings.stationArrows()) {
                Marker arrow = arrow(station.wind(), station.radius() * 0.8, 0.95);
                if (arrow != null) layer.addMarker(Key.of("station_wind_" + id), arrow);
            }
        }
    }

    @Override
    public void wind(World world, List<WindArrow> arrows, double longest) {
        if (arrows.isEmpty()) {
            drop(world, WIND);
            drop(world, FLOW);
            return;
        }
        Settings.Grid grid = settings.windLayer();
        SimpleLayerProvider layer = layer(world, WIND, grid.name(), grid.hiddenByDefault(), 12, 260);
        if (layer == null) return;
        layer.clearMarkers();
        if (settings.windAnimation()) flow(world, arrows, longest);
        int index = 0;
        for (WindArrow wind : arrows) {
            Marker arrow = arrow(wind, longest, grid.opacity());
            if (arrow == null) arrow = calm(wind, longest, grid.opacity());
            layer.addMarker(Key.of("wind_" + index++), arrow);
        }
    }

    @Override
    public void air(World world, List<AirCell> cells) {
        if (cells.isEmpty()) {
            drop(world, AIR);
            return;
        }
        Settings.Grid grid = settings.airLayer();
        SimpleLayerProvider layer = layer(world, AIR, grid.name(), grid.hiddenByDefault(), 11, 240);
        if (layer == null) return;
        layer.clearMarkers();
        int index = 0;
        for (AirCell cell : cells) {
            double half = cell.size() / 2.0;
            Marker square = Marker.rectangle(Point.of(cell.x() - half, cell.z() - half),
                    Point.of(cell.x() + half, cell.z() + half));
            square.markerOptions(MarkerOptions.builder()
                    .stroke(false).fillColor(cell.color()).fillOpacity(grid.opacity())
                    .hoverTooltip(StationMarker.escape(cell.tooltip()))
                    .build());
            layer.addMarker(Key.of("air_" + index++), square);
        }
    }

    /**
     * The wind animation (0.2.0): an empty "Wind flow" layer a viewer ticks
     * to see it, the script in squaremap's page (again, should squaremap
     * have written a fresh one), and this world's wind for it.
     */
    private void flow(World world, List<WindArrow> arrows, double longest) {
        layer(world, FLOW, "Wind flow", true, 13, 255);
        int spacing = (int) Math.round(longest / MapPeekPlugin.ARROW_SHARE);
        String field = WindFlow.field(arrows, spacing, WindFlow.colors(settings), world.getSeaLevel());
        String name = WindFlow.folderName(world);
        java.nio.file.Path web = squaremap.webDir();
        async(() -> {
            WindFlow.install(web, plugin.getLogger()::warning);
            WindFlow.publish(web, name, field, plugin.getLogger()::warning);
        });
    }

    private void async(Runnable work) {
        if (plugin.isEnabled()) plugin.getServer().getScheduler().runTaskAsynchronously(plugin, work);
        else work.run();
    }

    /** A calm spot (0.2.0): a small grey dot, so the layer never looks empty when the air is still. */
    private Marker calm(WindArrow wind, double longest, double opacity) {
        Marker dot = Marker.circle(Point.of(wind.x(), wind.z()), Math.max(4.0, longest * WindArrow.CALM_DOT));
        Color color = settings.color(wind.level());
        dot.markerOptions(MarkerOptions.builder()
                .strokeColor(color.darker()).strokeWeight(1).strokeOpacity(opacity)
                .fillColor(color).fillOpacity(opacity * 0.8)
                .hoverTooltip(StationMarker.escape(wind.tooltip()))
                .build());
        return dot;
    }

    /** An arrow's polygon, or null where there is none to draw. */
    private Marker arrow(WindArrow wind, double longest, double opacity) {
        List<double[]> outline = wind.outline(longest);
        if (outline.isEmpty()) return null;
        List<Point> points = new ArrayList<>(outline.size());
        for (double[] corner : outline) points.add(Point.of(corner[0], corner[1]));
        Color color = settings.color(wind.level());
        Marker arrow = Marker.polygon(points);
        arrow.markerOptions(MarkerOptions.builder()
                .strokeColor(color.darker()).strokeWeight(1).strokeOpacity(opacity)
                .fillColor(color).fillOpacity(opacity)
                .hoverTooltip(StationMarker.escape(wind.tooltip()))
                .build());
        return arrow;
    }

    /** A world's layer, registered the first time; null where squaremap doesn't map the world. */
    private SimpleLayerProvider layer(World world, Key key, String name, boolean hidden, int priority, int zIndex) {
        Optional<MapWorld> map = squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world));
        if (map.isEmpty()) return null;
        return layers.computeIfAbsent(world.getUID(), id -> new HashMap<>()).computeIfAbsent(key, ignored -> {
            SimpleLayerProvider created = SimpleLayerProvider.builder(name)
                    .showControls(true).defaultHidden(hidden).layerPriority(priority).zIndex(zIndex).build();
            if (map.get().layerRegistry().hasEntry(key)) map.get().layerRegistry().unregister(key);
            map.get().layerRegistry().register(key, created);
            return created;
        });
    }

    /** Takes one of a world's layers off its map. */
    private void drop(World world, Key key) {
        Map<Key, SimpleLayerProvider> registered = layers.get(world.getUID());
        if (registered == null || registered.remove(key) == null) return;
        squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(map -> {
            if (map.layerRegistry().hasEntry(key)) map.layerRegistry().unregister(key);
        });
    }

    @Override
    public void close() {
        for (Map.Entry<UUID, Map<Key, SimpleLayerProvider>> entry : layers.entrySet()) {
            World world = org.bukkit.Bukkit.getWorld(entry.getKey());
            if (world == null) continue;
            squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(map -> {
                for (Key key : entry.getValue().keySet())
                    if (map.layerRegistry().hasEntry(key)) map.layerRegistry().unregister(key);
            });
        }
        layers.clear();
        for (StationMarker.State state : StationMarker.State.values())
            if (squaremap.iconRegistry().hasEntry(icon(state))) squaremap.iconRegistry().unregister(icon(state));
        for (UUID id : clocks.keySet()) {
            World world = org.bukkit.Bukkit.getWorld(id);
            if (world == null) continue;
            squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(map -> {
                if (map.layerRegistry().hasEntry(CLOCK)) map.layerRegistry().unregister(CLOCK);
            });
        }
        clocks.clear();
    }
}
