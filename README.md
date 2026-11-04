# MapPeek

**EZvac on your web map.** MapPeek is an official addon for
[EZvac](https://github.com/EZvac-Plugin/EZvac): it draws what EZvac knows onto
your server's web map, [squaremap](https://modrinth.com/plugin/squaremap).

- **Each weather station is a circle** of the area it measures, on a
  "Weather stations" layer, coloured by what it's doing: working, switched
  off, or damaged. **An arrow** at its centre points the way its wind blows,
  longer and redder the stronger it is.
- **Hover** to see its name; **click** for today's weather (high, low, and
  what's certain, such as "Rain from 15:00"), the wind and air right now
  (`Wind: Breezy 18 km/h from the W`, `Air: AQI 42 (Good)`), the area it
  measures, and how many times lightning has struck it.
- **The wind across the map** (a **Wind** layer, hidden until you tick it):
  arrows every 192 blocks around spawn, showing EZvac's regional wind and
  where it blows from: the trade winds over deserts, the westerlies over the
  plains, rain turning them.
- **Air quality across the map** (an **Air quality** layer, hidden until you
  tick it): squares 128 blocks across in the US EPA's AQI colours, from green
  to maroon, with the AQI and pollen on hovering. The air changes gradually
  from square to square; desert dust storms and spring pollen show up on it.
- **The time and season** at the top of the layer list, such as
  `14:30 · Summer, day 3 of 15`, keeping time as the day goes by.
- **Over land no one has explored too:** EZvac works the wind and air out
  from the world seed, so nothing is loaded to draw them.
- **Up to date** every minute.

## Requirements

- **EZvac BETA 1.0 Pilot 3 or newer** (EZvac API 1.8), on Paper or Purpur.
  MapPeek 0.1.0 works with EZvac BETA 1.0 Pilot 2.
- **squaremap.** Without a supported map plugin MapPeek stays idle and says
  so in the log. Other map plugins (BlueMap, Dynmap) may follow.

## Install

1. Download `MapPeek-0.2.0.jar` from the
   [releases](https://github.com/EZvac-Plugin/MapPeek/releases).
2. Put it in your server's `plugins` folder, next to EZvac and squaremap.
3. Start the server. Open the map and look for the **Weather stations**,
   **Wind** and **Air quality** layers.

## Settings

`plugins/MapPeek/config.yml`:

```yaml
clock:
  enabled: true        # the time and season in the layer list
refresh-seconds: 60     # how often the map is updated (10 or more)
units: F                # F: °F and mph; C: °C and km/h
stations:
  enabled: true
  layer-name: Weather stations
  hidden-by-default: false
  colors:
    working: '#4FA3FF'
    off: '#8A8F98'
    damaged: '#E0A030'
  fill-opacity: 0.12    # 0 to 1
wind:
  station-arrows: true  # an arrow on each working station
  colors: { breezy: '#3FBF7F', windy: '#E0A030', gale: '#E04040' }
  layer:
    enabled: true
    layer-name: Wind
    hidden-by-default: true
    spacing-blocks: 192   # how far apart the arrows are
    radius-blocks: 2048   # how far from spawn they reach
    opacity: 0.85
air:
  layer:
    enabled: true
    layer-name: Air quality
    hidden-by-default: true
    spacing-blocks: 128   # how big each square is
    radius-blocks: 2048
    opacity: 0.35
```

A layer draws at most 2,500 arrows or squares; with a bigger radius they are
spread further apart.

## Building

```bash
mvn clean verify
```

The jar is written to `target/MapPeek-0.2.0.jar`. MapPeek is built only on
EZvac's public API, from [JitPack](https://jitpack.io/#EZvac-Plugin/EZvac).

## License

MIT. See [LICENSE](LICENSE).
