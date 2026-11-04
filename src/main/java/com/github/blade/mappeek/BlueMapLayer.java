package com.github.blade.mappeek;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * MapPeek on BlueMap (0.2.0), BlueMap's 3D web map: the same things as on
 * squaremap, as marker sets on every BlueMap map of a world. Stations are an
 * icon with their details and a circle of their coverage; the wind is arrows
 * drawn in HTML, turned the way it blows; the air is squares shaded in their
 * AQI colour, drawn over the terrain. The time and season are a marker set's
 * name, kept up to date every few seconds. Only loaded with BlueMap installed.
 */
final class BlueMapLayer implements MapLayer {
    private static final String STATIONS = "mappeek-stations";
    private static final String WIND = "mappeek-wind";
    private static final String AIR = "mappeek-air";
    private static final String CLOCK = "mappeek-clock";
    /** An empty set a viewer ticks to see the wind animation (0.2.0, wind.animation). */
    private static final String FLOW = "mappeek-wind-flow";
    /** Where the animation's script goes in BlueMap's web app, and each map's wind for it. */
    private static final String SCRIPT = "mappeek/wind.js";
    private static final String FIELD = "mappeek/wind.json";
    /** How often the clock's name is brought up to date, in ticks. */
    private static final long CLOCK_TICKS = 100L;

    private static Consumer<BlueMapAPI> enabled;
    private static Consumer<BlueMapAPI> disabled;

    private final BlueMapAPI api;
    private final Settings settings;
    private final MapPeekPlugin plugin;
    /** Each world's season for its clock, and the task that keeps the clocks' names. */
    private final Map<UUID, String> seasons = new HashMap<>();
    private final BukkitTask clockTask;

    private BlueMapLayer(MapPeekPlugin plugin, BlueMapAPI api, Settings settings) {
        this.api = api;
        this.settings = settings;
        this.plugin = plugin;
        for (BlueMapMap map : api.getMaps())
            for (StationMarker.State state : StationMarker.State.values()) writeIcon(map, state);
        // BlueMap loads addon scripts officially: the animation's goes in while it's on.
        java.nio.file.Path script = api.getWebApp().getWebRoot().resolve(SCRIPT);
        if (settings.windAnimation()) {
            api.getWebApp().registerScript(SCRIPT);
            async(() -> {
                try {
                    java.nio.file.Files.createDirectories(script.getParent());
                    java.nio.file.Files.write(script, WindFlow.script());
                } catch (IOException failure) {
                    plugin.getLogger().warning("Couldn't add the wind animation to BlueMap: " + failure.getMessage());
                }
            });
        } else {
            async(() -> {
                try {
                    java.nio.file.Files.deleteIfExists(script);
                } catch (IOException ignored) {
                    // Not registered, so BlueMap never loads it.
                }
            });
        }
        clockTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickClocks, CLOCK_TICKS, CLOCK_TICKS);
    }

    /** BlueMap, if it is ready now. */
    static Optional<MapLayer> current(MapPeekPlugin plugin, Settings settings) {
        return BlueMapAPI.getInstance().map(api -> new BlueMapLayer(plugin, api, settings));
    }

    /** Draws on BlueMap whenever it becomes ready, and stops when it goes: it can reload on its own. */
    static void watch(MapPeekPlugin plugin) {
        enabled = api -> Bukkit.getScheduler().runTask(plugin,
                () -> plugin.blueMapReady(new BlueMapLayer(plugin, api, plugin.settings())));
        disabled = api -> {
            if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, plugin::blueMapGone);
        };
        BlueMapAPI.onEnable(enabled);
        BlueMapAPI.onDisable(disabled);
    }

    static void unwatch() {
        if (enabled != null) BlueMapAPI.unregisterListener(enabled);
        if (disabled != null) BlueMapAPI.unregisterListener(disabled);
        enabled = null;
        disabled = null;
    }

    @Override
    public String name() {
        return "BlueMap";
    }

    private Collection<BlueMapMap> maps(World world) {
        return api.getWorld(world).map(found -> found.getMaps()).orElse(List.of());
    }

    @Override
    public void stations(World world, List<StationMarker> markers) {
        for (BlueMapMap map : maps(world)) {
            MarkerSet set = new MarkerSet(settings.layerName(), true, settings.hiddenByDefault());
            for (StationMarker station : markers) {
                double x = station.x() + 0.5, z = station.z() + 0.5;
                String id = station.x() + "_" + station.z();
                java.awt.Color color = settings.color(station.state());
                set.put("station-" + id, POIMarker.builder()
                        .label(station.label())
                        .position(x, station.y() + 4.0, z)
                        .icon(map.getAssetStorage().getAssetUrl(iconPath(station.state())),
                                StationIcon.SIZE / 2, StationIcon.SIZE / 2)
                        .detail(station.details())
                        .build());
                ShapeMarker circle = ShapeMarker.builder()
                        .label(station.label() + " coverage")
                        .shape(Shape.createCircle(x, z, station.radius(), 64), station.y())
                        .lineColor(color(color, 0.9f)).lineWidth(2)
                        .fillColor(color(color, (float) settings.fillOpacity()))
                        .depthTestEnabled(false)
                        .detail(station.details())
                        .build();
                circle.setListed(false);
                set.put("station-area-" + id, circle);
                if (settings.stationArrows() && station.wind().drawn()) {
                    // Half-way out on the downwind side, so it doesn't sit on the icon.
                    double toward = Math.toRadians(station.wind().towardDegrees()), out = station.radius() * 0.5;
                    WindArrow wind = station.wind();
                    WindArrow beside = new WindArrow(x + Math.sin(toward) * out, z - Math.cos(toward) * out,
                            wind.towardDegrees(), wind.level(), wind.tooltip());
                    set.put("station-wind-" + id, arrow(beside, station.y() + 6.0, 34, 0.95));
                }
            }
            map.getMarkerSets().put(STATIONS, set);
        }
    }

    @Override
    public void wind(World world, List<WindArrow> arrows, double longest) {
        Settings.Grid grid = settings.windLayer();
        double y = world.getSeaLevel() + 2.0;
        for (BlueMapMap map : maps(world)) {
            if (arrows.isEmpty()) {
                map.getMarkerSets().remove(WIND);
                map.getMarkerSets().remove(FLOW);
                continue;
            }
            MarkerSet set = new MarkerSet(grid.name(), true, grid.hiddenByDefault());
            int index = 0;
            for (WindArrow wind : arrows) set.put("wind-" + index++, arrow(wind, y, 26, grid.opacity()));
            map.getMarkerSets().put(WIND, set);
            if (settings.windAnimation()) flow(world, map, arrows, longest);
        }
    }

    /** The wind animation (0.2.0): its switch in BlueMap's menu, and this map's wind for its script. */
    private void flow(World world, BlueMapMap map, List<WindArrow> arrows, double longest) {
        if (!map.getMarkerSets().containsKey(FLOW)) {
            MarkerSet flow = new MarkerSet("Wind flow", true, true);
            flow.setSorting(1);
            map.getMarkerSets().put(FLOW, flow);
        }
        int spacing = (int) Math.round(longest / MapPeekPlugin.ARROW_SHARE);
        byte[] field = WindFlow.field(arrows, spacing, WindFlow.colors(settings), world.getSeaLevel())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        async(() -> {
            try (OutputStream out = map.getAssetStorage().writeAsset(FIELD)) {
                out.write(field);
            } catch (IOException failure) {
                plugin.getLogger().warning("Couldn't write the wind for BlueMap's animation: " + failure.getMessage());
            }
        });
    }

    private void async(Runnable work) {
        if (plugin.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(plugin, work);
        else work.run();
    }

    @Override
    public void air(World world, List<AirCell> cells) {
        Settings.Grid grid = settings.airLayer();
        float y = world.getSeaLevel();
        for (BlueMapMap map : maps(world)) {
            if (cells.isEmpty()) {
                map.getMarkerSets().remove(AIR);
                continue;
            }
            MarkerSet set = new MarkerSet(grid.name(), true, grid.hiddenByDefault());
            int index = 0;
            for (AirCell cell : cells) {
                double half = cell.size() / 2.0;
                ShapeMarker square = ShapeMarker.builder()
                        .label(cell.tooltip())
                        .shape(Shape.createRect(cell.x() - half, cell.z() - half, cell.x() + half, cell.z() + half), y)
                        .lineWidth(0).lineColor(color(cell.color(), 0f))
                        .fillColor(color(cell.color(), (float) grid.opacity()))
                        // Drawn over the terrain, like a weather map's shading.
                        .depthTestEnabled(false)
                        .detail(StationMarker.escape(cell.tooltip()))
                        .build();
                square.setListed(false);
                set.put("air-" + index++, square);
            }
            map.getMarkerSets().put(AIR, set);
        }
    }

    @Override
    public void clock(World world, String season) {
        seasons.put(world.getUID(), season == null ? "" : season);
        tickClocks();
    }

    /** Names each world's clock set after its time of day and season. */
    private void tickClocks() {
        for (Map.Entry<UUID, String> entry : seasons.entrySet()) {
            World world = Bukkit.getWorld(entry.getKey());
            if (world == null) continue;
            String label = ClockLabel.label(world.getTime(), entry.getValue());
            for (BlueMapMap map : maps(world)) {
                MarkerSet clock = map.getMarkerSets().get(CLOCK);
                if (clock == null) {
                    clock = new MarkerSet(label, false, false);
                    clock.setSorting(-1);
                    map.getMarkerSets().put(CLOCK, clock);
                } else {
                    clock.setLabel(label);
                }
            }
        }
    }

    /**
     * The wind as an HTML arrow, turned the way it blows, longer and redder the
     * stronger it is; a calm spot as a small grey dot. Hovering names it.
     */
    private HtmlMarker arrow(WindArrow wind, double y, int largest, double opacity) {
        java.awt.Color color = settings.color(wind.level());
        String hex = String.format(Locale.US, "#%06X", color.getRGB() & 0xFFFFFF);
        String title = StationMarker.escape(wind.tooltip());
        String html;
        if (wind.drawn()) {
            int size = (int) Math.max(10, Math.round(largest * wind.share()));
            // The glyph points east: turn it from there to where the wind blows.
            html = String.format(Locale.US, "<div title=\"%s\" style=\"transform:translate(-50%%,-50%%) rotate(%.0fdeg);"
                    + "color:%s;opacity:%.2f;font-size:%dpx;line-height:1;text-shadow:0 0 2px #000;"
                    + "pointer-events:auto\">&#10140;</div>", title, wind.towardDegrees() - 90.0, hex, opacity, size);
        } else {
            html = String.format(Locale.US, "<div title=\"%s\" style=\"transform:translate(-50%%,-50%%);width:6px;"
                    + "height:6px;border-radius:50%%;background:%s;opacity:%.2f;pointer-events:auto\"></div>",
                    title, hex, opacity * 0.8);
        }
        HtmlMarker marker = HtmlMarker.builder().label(wind.tooltip()).position(wind.x(), y, wind.z()).html(html).build();
        marker.setListed(false);
        return marker;
    }

    private static String iconPath(StationMarker.State state) {
        return "mappeek/station-" + state.name().toLowerCase(Locale.ROOT) + ".png";
    }

    private void writeIcon(BlueMapMap map, StationMarker.State state) {
        try (OutputStream out = map.getAssetStorage().writeAsset(iconPath(state))) {
            out.write(StationIcon.png(settings.color(state)));
        } catch (IOException failure) {
            Bukkit.getLogger().warning("[MapPeek] Couldn't save the station icon to BlueMap's map " + map.getId()
                    + ": " + failure);
        }
    }

    private static Color color(java.awt.Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0f, Math.min(1f, alpha)));
    }

    @Override
    public void close() {
        clockTask.cancel();
        for (BlueMapMap map : api.getMaps()) {
            map.getMarkerSets().remove(STATIONS);
            map.getMarkerSets().remove(WIND);
            map.getMarkerSets().remove(AIR);
            map.getMarkerSets().remove(CLOCK);
            map.getMarkerSets().remove(FLOW);
        }
        seasons.clear();
    }
}
