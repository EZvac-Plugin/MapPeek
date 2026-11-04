# MapPeek

**EZvac on your web map.** MapPeek is an official addon for
[EZvac](https://github.com/EZvac-Plugin/EZvac): it draws what EZvac knows onto
your server's web map, [squaremap](https://modrinth.com/plugin/squaremap) or
[BlueMap](https://modrinth.com/plugin/bluemap), or both at once.

- **Weather stations:** an icon for each, coloured by what it's doing
  (working, switched off, or damaged), a circle of the area it measures, and
  an arrow the way its wind blows. Hover to see its name; click for today's
  weather, the wind and air right now with a coloured AQI badge, a three-day
  outlook, the area it measures, and its lightning strikes.
- **The wind across the map** (a **Wind** layer, hidden until you tick it):
  arrows every 192 blocks, coloured and sized by how strong it is, showing
  EZvac's regional wind and where it blows from: the trade winds over
  deserts, the westerlies over the plains, rain turning them. Calm spots are
  small grey dots. It's brought up to date every 15 seconds, as it gusts and
  turns.
- **The wind in motion** (opt-in, `wind.animation`): a **Wind flow** layer of
  particles drifting along the wind like a weather site's wind map, coloured
  by how strong it blows, on squaremap and BlueMap. It runs in the viewer's
  browser and takes well under a millisecond a frame. BlueMap loads it as an
  addon script. squaremap has no way for addons to add to its page, so MapPeek
  adds one script line to squaremap's `index.html` while it's on, and takes it
  away again when it's off. Viewers with the map already open reload it once.
- **Air quality across the map** (an **Air quality** layer, hidden until you
  tick it): squares 128 blocks across, shaded on a smooth scale through the US
  EPA's AQI colours, from green to maroon, with the AQI, PM2.5, PM10 and
  pollen on hovering. Desert dust storms and spring pollen show up on it.
- **Over the whole explored map:** both layers cover every part of the world
  that has been explored. EZvac works the wind and air out from the world
  seed, so nothing is loaded to draw them. On a very big map they spread out
  to stay under 2,500 shapes each.
- **The time and season** in the layer list, such as
  `14:30 · Summer, day 3 of 15`, keeping time as the day goes by.
- **A menu for admins:** `/mappeek` switches each layer on or off, chooses
  whether it starts hidden, the units, the two refresh periods and the area.
  Changes are saved to `config.yml` and shown at once.

## Requirements

- **EZvac BETA 1.0 Pilot 3 or newer** (EZvac API 1.8), on Paper or Purpur.
  MapPeek 0.1.0 works with EZvac BETA 1.0 Pilot 2.
- **squaremap** and/or **BlueMap** (tested with squaremap 1.4.3 and BlueMap
  5.29 on Purpur 26.3). Without either, MapPeek stays idle and says so in the
  log. BlueMap needs its own `accept-download` set in its `core.conf` before
  it draws anything.

## Install

1. Download `MapPeek-0.2.0.jar` from the
   [releases](https://github.com/EZvac-Plugin/MapPeek/releases).
2. Put it in your server's `plugins` folder, next to EZvac and your map
   plugin.
3. Start the server. Open the map and look for the **Weather stations**,
   **Wind** and **Air quality** layers (on BlueMap, under Markers in its
   menu).

## Commands

| Command | Who | What |
| --- | --- | --- |
| `/mappeek` | `mappeek.admin` (ops) | Opens the menu |
| `/mappeek reload` | `mappeek.admin` | Reads `config.yml` again after you edit it, and redraws |

In the menu, click a layer to switch it on or off, and shift-click it to
choose whether viewers see it at first or only once they tick it. Click a
refresh period for less often, right-click for more often.

## Settings

`plugins/MapPeek/config.yml`:

```yaml
clock:
  enabled: true          # the time and season in the layer list
refresh-seconds: 60      # stations, the air and the season (10 to 3600)
units: F                 # F: °F and mph; C: °C and km/h
stations:
  enabled: true
  layer-name: Weather stations
  hidden-by-default: false
  colors:
    working: '#4FA3FF'
    off: '#8A8F98'
    damaged: '#E0A030'
  fill-opacity: 0.12     # 0 to 1
wind:
  refresh-seconds: 15    # the wind and the stations' arrows (5 to 3600)
  station-arrows: true   # an arrow on each working station
  animation: false       # the Wind flow layer, on both maps (opt-in)
  colors: { breezy: '#3FBF7F', windy: '#E0A030', gale: '#E04040' }
  layer:
    enabled: true
    layer-name: Wind
    hidden-by-default: true
    area: explored       # explored: the whole explored map; spawn: radius-blocks around spawn
    spacing-blocks: 192  # how far apart the arrows are
    radius-blocks: 2048
    opacity: 0.85
air:
  layer:
    enabled: true
    layer-name: Air quality
    hidden-by-default: true
    area: explored
    spacing-blocks: 128  # how big each square is
    radius-blocks: 2048
    opacity: 0.35
```

## Building

```bash
mvn clean verify
```

The jar is written to `target/MapPeek-0.2.0.jar`. MapPeek is built only on
EZvac's public API, from [JitPack](https://jitpack.io/#EZvac-Plugin/EZvac),
and the map plugins' own APIs.

## License

MIT. See [LICENSE](LICENSE).
