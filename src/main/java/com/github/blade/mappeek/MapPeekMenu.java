package com.github.blade.mappeek;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /mappeek} (0.2.0): a menu for admins to choose what MapPeek shows on
 * the web map, and how often it brings it up to date. Each change is written
 * to config.yml, comments kept, and MapPeek redraws at once.
 *
 * <pre>
 *  [ about ]  .  .  .  .  .  .  .  [ maps ]
 *  . [stations][arrows][wind][air][clock][units][flow] .
 *  . [refresh][wind refresh] . [area] .  .  . [redraw]
 * </pre>
 */
final class MapPeekMenu implements Listener {
    static final String PERMISSION = "mappeek.admin";

    private static final int ABOUT = 0, MAPS = 8;
    private static final int STATIONS = 10, ARROWS = 11, WIND = 12, AIR = 13, CLOCK = 14, UNITS = 15, FLOW = 16;
    private static final int REFRESH = 19, WIND_REFRESH = 20, AREA = 22, REDRAW = 25;

    private final MapPeekPlugin plugin;

    /** Marks MapPeek's own menu among open inventories. */
    private static final class Holder implements InventoryHolder {
        Inventory inventory;

        @Override
        public Inventory getInventory() { return inventory; }
    }

    MapPeekMenu(MapPeekPlugin plugin) {
        this.plugin = plugin;
    }

    void open(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You need " + PERMISSION + " to change MapPeek.", NamedTextColor.RED));
            return;
        }
        Holder holder = new Holder();
        holder.inventory = plugin.getServer().createInventory(holder, 27,
                Component.text("MapPeek", NamedTextColor.DARK_AQUA));
        draw(holder.inventory);
        player.openInventory(holder.inventory);
    }

    private void draw(Inventory inventory) {
        Settings s = plugin.settings();
        inventory.clear();
        inventory.setItem(ABOUT, item(Material.FILLED_MAP, "MapPeek " + plugin.getPluginMeta().getVersion(),
                NamedTextColor.AQUA, List.of("EZvac on your web map.", "Click a button to switch it;",
                        "shift-click a layer: shown at first,", "or hidden until a viewer ticks it.")));
        List<String> maps = plugin.mapNames();
        inventory.setItem(MAPS, item(maps.isEmpty() ? Material.BARRIER : Material.COMPASS, "Drawing on",
                NamedTextColor.WHITE, maps.isEmpty() ? List.of("No map plugin is ready:", "install squaremap or BlueMap.")
                        : maps.stream().map(name -> "✔ " + name).toList()));
        inventory.setItem(STATIONS, toggle(Material.LIGHTNING_ROD, "Weather stations", s.stations(), s.hiddenByDefault(),
                "Each station's coverage, icon and forecast."));
        inventory.setItem(ARROWS, toggle(Material.ARROW, "Wind on stations", s.stationArrows(), null,
                "An arrow on each working station,", "the way its wind blows."));
        inventory.setItem(WIND, toggle(Material.FEATHER, "Wind layer", s.windLayer().enabled(),
                s.windLayer().hiddenByDefault(), "Arrows across the map, coloured", "and sized by how strong it is."));
        inventory.setItem(AIR, toggle(Material.TUBE_CORAL_BLOCK, "Air quality layer", s.airLayer().enabled(),
                s.airLayer().hiddenByDefault(), "Squares in the AQI's colours,", "from Good to Hazardous."));
        inventory.setItem(CLOCK, toggle(Material.CLOCK, "Time and season", s.clock(), null,
                "The time of day and EZvac's season", "at the top of the layer list."));
        inventory.setItem(FLOW, toggle(Material.WIND_CHARGE, "Wind flow", s.windAnimation(), null,
                "Moving particles along the wind,", "like a weather site's wind map,",
                "on squaremap and BlueMap.", "Viewers tick Wind flow to see it."));
        inventory.setItem(UNITS, item(Material.NAME_TAG, "Units: " + (s.celsius() ? "°C and km/h" : "°F and mph"),
                NamedTextColor.WHITE, List.of("Click for " + (s.celsius() ? "°F and mph." : "°C and km/h."))));
        inventory.setItem(REFRESH, item(Material.REPEATER, "Refresh every " + seconds(s.refreshSeconds()),
                NamedTextColor.WHITE, List.of("Stations' forecasts, the air", "and the season.",
                        "Click: less often. Right-click: more often.")));
        inventory.setItem(WIND_REFRESH, item(Material.COMPARATOR, "Wind every " + seconds(s.windRefreshSeconds()),
                NamedTextColor.WHITE, List.of("The wind and the stations' arrows,", "which turn and gust.",
                        "Click: less often. Right-click: more often.")));
        boolean explored = s.windLayer().area() == Settings.Area.EXPLORED;
        inventory.setItem(AREA, item(explored ? Material.GRASS_BLOCK : Material.RED_BED,
                "Layers cover: " + (explored ? "the explored map" : "around spawn"), NamedTextColor.WHITE,
                List.of(explored ? "Everywhere the world has been explored." : "Out to radius-blocks from spawn.",
                        "Click for " + (explored ? "around spawn." : "the explored map."))));
        inventory.setItem(REDRAW, item(Material.SPYGLASS, "Redraw now", NamedTextColor.GREEN,
                List.of("Brings every map up to date", "without waiting.")));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != holder.inventory) return;
        if (!player.hasPermission(PERMISSION)) {
            player.closeInventory();
            return;
        }
        FileConfiguration config = plugin.getConfig();
        Settings s = plugin.settings();
        boolean shift = event.getClick().isShiftClick();
        boolean right = event.getClick() == ClickType.RIGHT;
        String changed = switch (event.getSlot()) {
            case STATIONS -> shift ? set(config, "stations.hidden-by-default", !s.hiddenByDefault())
                    : set(config, "stations.enabled", !s.stations());
            case ARROWS -> set(config, "wind.station-arrows", !s.stationArrows());
            case WIND -> shift ? set(config, "wind.layer.hidden-by-default", !s.windLayer().hiddenByDefault())
                    : set(config, "wind.layer.enabled", !s.windLayer().enabled());
            case AIR -> shift ? set(config, "air.layer.hidden-by-default", !s.airLayer().hiddenByDefault())
                    : set(config, "air.layer.enabled", !s.airLayer().enabled());
            case CLOCK -> set(config, "clock.enabled", !s.clock());
            case FLOW -> set(config, "wind.animation", !s.windAnimation());
            case UNITS -> set(config, "units", s.celsius() ? "F" : "C");
            case REFRESH -> set(config, "refresh-seconds", Settings.step(Settings.REFRESH_STEPS, s.refreshSeconds(), right));
            case WIND_REFRESH -> set(config, "wind.refresh-seconds",
                    Settings.step(Settings.WIND_REFRESH_STEPS, s.windRefreshSeconds(), right));
            case AREA -> {
                String area = s.windLayer().area() == Settings.Area.EXPLORED ? "spawn" : "explored";
                config.set("wind.layer.area", area);
                yield set(config, "air.layer.area", area);
            }
            case REDRAW -> {
                plugin.refreshWind();
                plugin.refresh();
                player.sendMessage(Component.text("MapPeek redrew the map.", NamedTextColor.GREEN));
                yield null;
            }
            default -> null;
        };
        if (changed == null) return;
        plugin.saveConfig();
        plugin.reload();
        plugin.getLogger().info(player.getName() + " set " + changed + " in /mappeek.");
        draw(holder.inventory);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    private static String set(FileConfiguration config, String path, Object value) {
        config.set(path, value);
        return path + " to " + value;
    }

    private static String seconds(int seconds) {
        return seconds % 60 == 0 && seconds >= 60 ? (seconds / 60) + (seconds == 60 ? " minute" : " minutes")
                : seconds + " seconds";
    }

    /** A switch: green when on, grey when off, with whether its layer starts hidden. */
    private static ItemStack toggle(Material material, String name, boolean on, Boolean hidden, String... about) {
        List<String> lore = new ArrayList<>(List.of(about));
        lore.add("");
        lore.add((on ? "On" : "Off") + " · click to switch " + (on ? "off." : "on."));
        if (hidden != null)
            lore.add((hidden ? "Hidden until ticked" : "Shown at first") + " · shift-click to change.");
        ItemStack item = item(on ? material : Material.GRAY_DYE, name, on ? NamedTextColor.GREEN : NamedTextColor.GRAY,
                lore);
        return item;
    }

    private static ItemStack item(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> (Component) Component.text(line, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }
}
