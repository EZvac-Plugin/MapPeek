package com.github.blade.mappeek;

import com.github.blade.hvac.api.EzvacApi;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * MapPeek: EZvac on your web map. Draws every weather station's coverage on
 * squaremap, refreshed every minute. Built only on EZvac's public API (1.7 or
 * newer: a station's coverage came in 1.7).
 */
public final class MapPeekPlugin extends JavaPlugin {
    private EzvacApi ezvac;
    private Settings settings;
    private final List<MapLayer> maps = new ArrayList<>();
    private BukkitTask task;

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
            getLogger().severe("MapPeek needs EZvac API 1.7 or newer (EZvac BETA 1.0 Pilot 2), but EZvac "
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
        getLogger().info("MapPeek draws EZvac's weather stations on " + String.join(", ",
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

    /** Brings every map up to date with EZvac's weather stations. Main thread. */
    private void refresh() {
        for (World world : getServer().getWorlds()) {
            List<StationMarker> markers = new ArrayList<>();
            if (settings.stations())
                ezvac.weatherStations(world).forEach(station -> markers.add(StationMarker.of(station, settings.celsius())));
            for (MapLayer map : maps) {
                try {
                    map.stations(world, markers);
                } catch (RuntimeException failure) {
                    getLogger().warning("Could not update " + map.name() + " for " + world.getName() + ": " + failure);
                }
            }
        }
    }

    /** API 1.7 or newer, within major version 1. */
    static boolean supported(String apiVersion) {
        String[] parts = apiVersion.split("\\.");
        try {
            return Integer.parseInt(parts[0]) == 1 && parts.length > 1 && Integer.parseInt(parts[1]) >= 7;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }
}
