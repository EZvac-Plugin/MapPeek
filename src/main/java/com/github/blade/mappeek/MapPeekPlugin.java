package com.github.blade.mappeek;

import com.github.blade.hvac.api.EzvacApi;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * MapPeek: EZvac on your web map. Draws every weather station's coverage on
 * squaremap, and since 0.2.0 its wind as an arrow, and the wind and air
 * quality across the map, refreshed every minute. Built only on EZvac's
 * public API (1.8 or newer: the wind's direction and the air anywhere came in
 * 1.8).
 */
public final class MapPeekPlugin extends JavaPlugin {
    private EzvacApi ezvac;
    private Settings settings;
    private final List<MapLayer> maps = new ArrayList<>();
    private BukkitTask task;
    /** A gale's arrow on the Wind layer is this share of the gap between arrows, so they never crowd. */
    static final double ARROW_SHARE = 0.6;

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
        settings = Settings.load(getConfig(), getLogger()::warning);
        if (getServer().getPluginManager().isPluginEnabled("squaremap")) maps.add(new SquaremapLayer(settings));
        if (maps.isEmpty()) {
            getLogger().warning("No supported map plugin is installed (squaremap), so there is nothing to draw on."
                    + " MapPeek stays idle.");
            return;
        }
        // A tick after start, so every world's map is set up first; then every refresh-seconds.
        task = getServer().getScheduler().runTaskTimer(this, this::refresh, 20L, settings.refreshSeconds() * 20L);
        List<String> drawn = new ArrayList<>();
        if (settings.stations()) drawn.add("weather stations");
        if (settings.windLayer().enabled()) drawn.add("the wind");
        if (settings.airLayer().enabled()) drawn.add("air quality");
        if (settings.clock()) drawn.add("the time and season");
        String what = drawn.size() < 2 ? String.join("", drawn)
                : String.join(", ", drawn.subList(0, drawn.size() - 1)) + " and " + drawn.get(drawn.size() - 1);
        getLogger().info("MapPeek draws " + (what.isEmpty() ? "nothing" : what) + " on " + String.join(", ",
                maps.stream().map(MapLayer::name).toList()) + ", every " + settings.refreshSeconds() + " seconds.");
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        for (MapLayer map : maps) {
            try {
                map.close();
            } catch (RuntimeException ignored) {
                // The map plugin may already have stopped.
            }
        }
        maps.clear();
    }

    /** Brings every map up to date with EZvac. Main thread. */
    private void refresh() {
        for (World world : getServer().getWorlds()) {
            List<StationMarker> markers = new ArrayList<>();
            if (settings.stations())
                ezvac.weatherStations(world).forEach(station -> markers.add(StationMarker.of(station, settings.celsius())));
            List<WindArrow> arrows = settings.windLayer().enabled() ? windAcross(world) : List.of();
            List<AirCell> cells = settings.airLayer().enabled() ? airAcross(world) : List.of();
            for (MapLayer map : maps) {
                try {
                    map.stations(world, markers);
                    map.wind(world, arrows, settings.windLayer().spacing() * ARROW_SHARE);
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

    /**
     * The wind on a grid around spawn. A world with no wind at all (the
     * Nether, the End, or wind switched off) has no direction anywhere, so it
     * gets no layer.
     */
    private List<WindArrow> windAcross(World world) {
        Location spawn = world.getSpawnLocation();
        List<WindArrow> arrows = new ArrayList<>();
        for (double[] point : settings.windLayer().points(spawn.getX(), spawn.getZ())) {
            var wind = ezvac.windAt(world, (int) Math.floor(point[0]), (int) Math.floor(point[1]));
            if (wind.hasDirection()) arrows.add(WindArrow.of(wind, point[0], point[1], settings.celsius()));
        }
        return arrows;
    }

    /** The air on a grid around spawn; none where EZvac gives no air. */
    private List<AirCell> airAcross(World world) {
        Location spawn = world.getSpawnLocation();
        int size = settings.airLayer().spacing();
        List<AirCell> cells = new ArrayList<>();
        for (double[] point : settings.airLayer().points(spawn.getX(), spawn.getZ()))
            ezvac.outdoorAirAt(world, (int) Math.floor(point[0]), (int) Math.floor(point[1]))
                    .ifPresent(air -> cells.add(AirCell.of(air, point[0], point[1], size)));
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
