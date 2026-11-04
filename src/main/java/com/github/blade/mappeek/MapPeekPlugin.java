package com.github.blade.mappeek;

import com.github.blade.hvac.api.EzvacApi;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MapPeek: EZvac on your web map. Draws every weather station's coverage, and
 * since 0.2.0 its wind as an arrow, the wind and air quality across the
 * explored map, and each world's time and season, on squaremap and BlueMap.
 * The wind is brought up to date every {@code wind.refresh-seconds}, the
 * rest every {@code refresh-seconds}; {@code /mappeek} opens a menu to
 * change what is shown. Built only on EZvac's public API (1.8 or newer: the
 * wind's direction and the air anywhere came in 1.8).
 */
public final class MapPeekPlugin extends JavaPlugin {
    private EzvacApi ezvac;
    private Settings settings;
    private final List<MapLayer> maps = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    /** Each world's explored regions, read again at most once a minute. */
    private final Map<UUID, Explored> explored = new HashMap<>();
    private MapPeekMenu menu;
    /** A gale's arrow on the Wind layer is this share of the gap between arrows, so they never crowd. */
    static final double ARROW_SHARE = 0.6;
    private static final long EXPLORED_MILLIS = 60_000L;

    private record Explored(long readAt, Set<Long> regions) {}

    @Override
    public void onEnable() {
        try {
            ezvac = EzvacApi.get();
        } catch (IllegalStateException missing) {
            getLogger().severe(missing.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!supported(ezvac.apiVersion())) {
            getLogger().severe("MapPeek needs EZvac API 1.8 or newer (EZvac BETA 1.0 Pilot 3), but EZvac "
                    + ezvac.pluginVersion() + " offers API " + ezvac.apiVersion() + ". Update EZvac.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        menu = new MapPeekMenu(this);
        getServer().getPluginManager().registerEvents(menu, this);
        start();
        // BlueMap starts after MapPeek and can reload on its own: it says when it's ready.
        if (blueMap()) BlueMapLayer.watch(this);
    }

    @Override
    public void onDisable() {
        stop();
        if (blueMap()) BlueMapLayer.unwatch();
    }

    private boolean blueMap() {
        return getServer().getPluginManager().getPlugin("BlueMap") != null;
    }

    Settings settings() { return settings; }

    /** The map plugins MapPeek draws on right now, for the menu. */
    List<String> mapNames() {
        return maps.stream().map(MapLayer::name).toList();
    }

    /** Reads config.yml again and redraws every map with it: the menu's changes, or {@code /mappeek reload}. */
    void reload() {
        reloadConfig();
        stop();
        start();
    }

    /** BlueMap became ready (or reloaded): draw on it too. */
    void blueMapReady(MapLayer blueMap) {
        maps.removeIf(map -> map.name().equals(blueMap.name()));
        maps.add(blueMap);
        refreshWind();
        refresh();
        getLogger().info("MapPeek draws on BlueMap too.");
    }

    /** BlueMap stopped: stop drawing on it. */
    void blueMapGone() {
        maps.removeIf(map -> map.name().equals("BlueMap"));
    }

    private void start() {
        settings = Settings.load(getConfig(), getLogger()::warning);
        if (getServer().getPluginManager().isPluginEnabled("squaremap")) maps.add(new SquaremapLayer(settings, this));
        // BlueMap's classes are only there with BlueMap installed.
        if (blueMap()) BlueMapLayer.current(this, settings).ifPresent(maps::add);
        if (maps.isEmpty() && !blueMap()) {
            getLogger().warning("No supported map plugin is installed (squaremap or BlueMap), so there is nothing to"
                    + " draw on. MapPeek stays idle.");
            return;
        }
        // A tick after start, so every world's map is set up first.
        tasks.add(getServer().getScheduler().runTaskTimer(this, this::refresh, 20L, settings.refreshSeconds() * 20L));
        tasks.add(getServer().getScheduler().runTaskTimer(this, this::refreshWind, 25L,
                settings.windRefreshSeconds() * 20L));
        List<String> drawn = new ArrayList<>();
        if (settings.stations()) drawn.add("weather stations");
        if (settings.windLayer().enabled()) drawn.add("the wind");
        if (settings.airLayer().enabled()) drawn.add("air quality");
        if (settings.clock()) drawn.add("the time and season");
        String what = drawn.size() < 2 ? String.join("", drawn)
                : String.join(", ", drawn.subList(0, drawn.size() - 1)) + " and " + drawn.get(drawn.size() - 1);
        getLogger().info("MapPeek draws " + (what.isEmpty() ? "nothing" : what) + " on "
                + (maps.isEmpty() ? "BlueMap, once it is ready" : String.join(", ", mapNames()))
                + ": the wind every " + settings.windRefreshSeconds() + " seconds, the rest every "
                + settings.refreshSeconds() + ".");
    }

    private void stop() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        for (MapLayer map : maps) {
            try {
                map.close();
            } catch (RuntimeException ignored) {
                // The map plugin may already have stopped.
            }
        }
        maps.clear();
        explored.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reload();
            sender.sendMessage("MapPeek reloaded config.yml and redrew " + (maps.isEmpty() ? "nothing (no map plugin)"
                    : String.join(" and ", mapNames())) + ".");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Open the menu in game, or use /mappeek reload.");
            return true;
        }
        menu.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && "reload".startsWith(args[0].toLowerCase(java.util.Locale.ROOT))
                ? List.of("reload") : List.of();
    }

    /** Stations, the air and the season. Main thread. */
    void refresh() {
        for (World world : getServer().getWorlds()) {
            List<AirCell> cells = settings.airLayer().enabled() ? airAcross(world) : List.of();
            for (MapLayer map : maps) {
                try {
                    map.air(world, cells);
                    // The clock and season where the world has a sun; the Nether and the End keep no time.
                    if (settings.clock() && world.getEnvironment() == World.Environment.NORMAL)
                        map.clock(world, ClockLabel.season(ezvac.season(world)));
                } catch (RuntimeException failure) {
                    getLogger().warning("Could not update " + map.name() + " for " + world.getName() + ": " + failure);
                }
            }
        }
    }

    /** The wind, and the stations with their arrows, more often than the rest (0.2.0). Main thread. */
    void refreshWind() {
        for (World world : getServer().getWorlds()) {
            List<StationMarker> markers = new ArrayList<>();
            if (settings.stations())
                ezvac.weatherStations(world).forEach(station -> markers.add(StationMarker.of(station, settings.celsius())));
            Points arrows = settings.windLayer().enabled() ? windAcross(world) : Points.NONE;
            for (MapLayer map : maps) {
                try {
                    map.stations(world, markers);
                    map.wind(world, arrows.arrows(), arrows.spacing() * ARROW_SHARE);
                } catch (RuntimeException failure) {
                    getLogger().warning("Could not update " + map.name() + " for " + world.getName() + ": " + failure);
                }
            }
        }
    }

    private record Points(int spacing, List<WindArrow> arrows) {
        static final Points NONE = new Points(1, List.of());
    }

    /**
     * Where a layer's grid goes in a world: the whole explored map, or
     * around spawn; around spawn too where the world's regions can't be read.
     */
    private Coverage.Points grid(World world, Settings.Grid grid) {
        if (grid.area() == Settings.Area.EXPLORED) {
            Set<Long> regions = regions(world);
            if (!regions.isEmpty()) return Coverage.points(regions, grid.spacingBlocks(), Settings.MAXIMUM_POINTS);
        }
        Location spawn = world.getSpawnLocation();
        return new Coverage.Points(grid.spacing(), grid.points(spawn.getX(), spawn.getZ()));
    }

    private Set<Long> regions(World world) {
        Explored known = explored.get(world.getUID());
        long now = System.currentTimeMillis();
        if (known != null && now - known.readAt() < EXPLORED_MILLIS) return known.regions();
        Set<Long> regions = Coverage.regions(world);
        explored.put(world.getUID(), new Explored(now, regions));
        return regions;
    }

    /**
     * The wind on the grid. A world with no wind at all (the Nether, the End,
     * or wind switched off) has no direction anywhere, so it gets no layer.
     */
    private Points windAcross(World world) {
        Coverage.Points grid = grid(world, settings.windLayer());
        List<WindArrow> arrows = new ArrayList<>();
        for (double[] point : grid.points()) {
            var wind = ezvac.windAt(world, (int) Math.floor(point[0]), (int) Math.floor(point[1]));
            if (wind.hasDirection()) arrows.add(WindArrow.of(wind, point[0], point[1], settings.celsius()));
        }
        return new Points(grid.spacing(), arrows);
    }

    /** The air on the grid; none where EZvac gives no air. */
    private List<AirCell> airAcross(World world) {
        Coverage.Points grid = grid(world, settings.airLayer());
        List<AirCell> cells = new ArrayList<>();
        for (double[] point : grid.points())
            ezvac.outdoorAirAt(world, (int) Math.floor(point[0]), (int) Math.floor(point[1]))
                    .ifPresent(air -> cells.add(AirCell.of(air, point[0], point[1], grid.spacing())));
        return cells;
    }

    /** API 1.8 or newer, within major version 1. */
    static boolean supported(String apiVersion) {
        String[] parts = apiVersion.split("\\.");
        try {
            return Integer.parseInt(parts[0]) == 1 && parts.length > 1 && Integer.parseInt(parts[1]) >= 8;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }
}
