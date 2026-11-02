# MapPeek

**EZvac on your web map.** MapPeek is an official addon for
[EZvac](https://github.com/EZvac-Plugin/EZvac): it draws what EZvac knows onto
your server's web map, starting with every **weather station's coverage** on
[squaremap](https://modrinth.com/plugin/squaremap).

- **Each weather station is a circle** of the area it measures, on a
  "Weather stations" layer, coloured by what it's doing: working, switched
  off, or damaged.
- **Hover** to see its name; **click** for today's weather (high, low, and
  what's certain, such as "Rain from 15:00"), the area it measures, and how
  many times lightning has struck it.
- **Up to date** every minute.

## Requirements

- **EZvac BETA 1.0 Pilot 2 or newer** (EZvac API 1.7), on Paper or Purpur.
- **squaremap.** Without a supported map plugin MapPeek stays idle and says
  so in the log. Other map plugins (BlueMap, Dynmap) may follow.

## Install

1. Download `MapPeek-0.1.0.jar` from the
   [releases](https://github.com/EZvac-Plugin/MapPeek/releases).
2. Put it in your server's `plugins` folder, next to EZvac and squaremap.
3. Start the server. Open the map and look for the **Weather stations** layer.

## Settings

`plugins/MapPeek/config.yml`:

```yaml
refresh-seconds: 60     # how often the map is updated (10 or more)
units: F                # temperatures in a station's details: F or C
stations:
  enabled: true
  layer-name: Weather stations
  hidden-by-default: false
  colors:
    working: '#4FA3FF'
    off: '#8A8F98'
    damaged: '#E0A030'
  fill-opacity: 0.12    # 0 to 1
```

## Building

```bash
mvn clean verify
```

The jar is written to `target/MapPeek-0.1.0.jar`. MapPeek is built only on
EZvac's public API, from [JitPack](https://jitpack.io/#EZvac-Plugin/EZvac).

## License

MIT. See [LICENSE](LICENSE).
