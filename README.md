<div align="center">

# MapPeek

**EZvac on your web map.**<br>
An official addon for [EZvac](https://github.com/EZvac-Plugin/EZvac), real heating, cooling and indoor climate for Paper and Purpur.

[![Release](https://img.shields.io/github/v/release/EZvac-Plugin/MapPeek?label=release&color=0e7c86&logo=github&style=for-the-badge&labelColor=1f2328)](https://github.com/EZvac-Plugin/MapPeek/releases)
[![Needs EZvac](https://img.shields.io/badge/needs%20EZvac-BETA%201.0%20P3%2B-0e7c86?style=for-the-badge&labelColor=1f2328&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNiAxNiI+PHBhdGggZmlsbD0iI2ZmZiIgZmlsbC1ydWxlPSJldmVub2RkIiBkPSJNOCAxYzIuNCAyLjggNC42IDUuNSA0LjYgOC4yYTQuNiA0LjYgMCAwIDEtOS4yIDBDMy40IDYuNSA1LjYgMy44IDggMXptMCA2LjZhMS43IDEuNyAwIDEgMCAwIDMuNCAxLjcgMS43IDAgMCAwIDAtMy40eiIvPjwvc3ZnPg==)](https://github.com/EZvac-Plugin/EZvac)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1%2B-0e7c86?style=for-the-badge&labelColor=1f2328&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNiAxNiIgc2hhcGUtcmVuZGVyaW5nPSJjcmlzcEVkZ2VzIj48cmVjdCB3aWR0aD0iMTYiIGhlaWdodD0iMTYiIGZpbGw9IiM4YTVhM2IiLz48cmVjdCB3aWR0aD0iMTYiIGhlaWdodD0iNSIgZmlsbD0iIzZhYWEzYSIvPjxyZWN0IHg9IjIiIHk9IjUiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIGZpbGw9IiM2YWFhM2EiLz48cmVjdCB4PSI3IiB5PSI1IiB3aWR0aD0iMiIgaGVpZ2h0PSIzIiBmaWxsPSIjNmFhYTNhIi8+PHJlY3QgeD0iMTIiIHk9IjUiIHdpZHRoPSIyIiBoZWlnaHQ9IjIiIGZpbGw9IiM2YWFhM2EiLz48cmVjdCB4PSI0IiB5PSIxMCIgd2lkdGg9IjIiIGhlaWdodD0iMiIgZmlsbD0iIzZiNDMyOSIvPjxyZWN0IHg9IjEwIiB5PSIxMiIgd2lkdGg9IjIiIGhlaWdodD0iMiIgZmlsbD0iIzZiNDMyOSIvPjxyZWN0IHg9IjEyIiB5PSI4IiB3aWR0aD0iMSIgaGVpZ2h0PSIxIiBmaWxsPSIjYTc3NzRmIi8+PHJlY3QgeD0iMiIgeT0iMTMiIHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9IiNhNzc3NGYiLz48L3N2Zz4=)](#requirements)
[![License](https://img.shields.io/github/license/EZvac-Plugin/MapPeek?color=0e7c86&logo=opensourceinitiative&style=for-the-badge&labelColor=1f2328)](LICENSE)

[![Download](https://img.shields.io/badge/Download-f69500?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNiAxNiI+PHBhdGggZmlsbD0iI2ZmZiIgZD0iTTcgMWgydjdsMi41LTIuNSAxLjQgMS40TDggMTEuOCAzLjEgNi45bDEuNC0xLjRMNyA4eiIvPjxwYXRoIGZpbGw9IiNmZmYiIGQ9Ik0yIDEyaDJ2MS41aDhWMTJoMnYzLjVIMnoiLz48L3N2Zz4=)](https://github.com/EZvac-Plugin/MapPeek/releases/latest)
[![EZvac](https://img.shields.io/badge/EZvac-f69500?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNiAxNiI+PHBhdGggZmlsbD0iI2ZmZiIgZmlsbC1ydWxlPSJldmVub2RkIiBkPSJNOCAxYzIuNCAyLjggNC42IDUuNSA0LjYgOC4yYTQuNiA0LjYgMCAwIDEtOS4yIDBDMy40IDYuNSA1LjYgMy44IDggMXptMCA2LjZhMS43IDEuNyAwIDEgMCAwIDMuNCAxLjcgMS43IDAgMCAwIDAtMy40eiIvPjwvc3ZnPg==)](https://github.com/EZvac-Plugin/EZvac)
[![Website](https://img.shields.io/badge/EZvac%20website-f69500?style=for-the-badge&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNiAxNiI+PHBhdGggZmlsbD0iI2ZmZiIgZmlsbC1ydWxlPSJldmVub2RkIiBkPSJNOCAxYTcgNyAwIDEgMCAwIDE0QTcgNyAwIDAgMCA4IDF6bS0uOCAxLjZDNi40IDMuNyA1LjkgNS4zIDUuOCA3LjJIMy4xYTUgNSAwIDAgMSA0LjEtNC42em0xLjYgMGE1IDUgMCAwIDEgNC4xIDQuNmgtMi43Yy0uMS0xLjktLjYtMy41LTEuNC00LjZ6TTcuMiA3LjJINy4zYy4xLTEuNi41LTMgLjctMy42LjIuNi42IDIgLjcgMy42ek0zLjEgOC44aDIuN2MuMSAxLjkuNiAzLjUgMS40IDQuNmE1IDUgMCAwIDEtNC4xLTQuNnptNC4yIDBoMS40Yy0uMSAxLjYtLjUgMy0uNyAzLjYtLjItLjYtLjYtMi0uNy0zLjZ6bTMuNiAwaDIuN2E1IDUgMCAwIDEtNC4xIDQuNmMuOC0xLjEgMS4zLTIuNyAxLjQtNC42eiIvPjwvc3ZnPg==)](https://ezvac-plugin.github.io/EZvac/)

</div>

MapPeek draws what EZvac knows onto your server's web map, [squaremap](https://modrinth.com/plugin/squaremap) or [BlueMap](https://modrinth.com/plugin/bluemap), or both at once.

## Features

<table>
<tr>
<td width="50%" valign="top">

**📡 Weather stations**<br>
Each with its area, its wind arrow and a click for its forecast and air.

</td>
<td width="50%" valign="top">

**🌬️ The wind across the map**<br>
Arrows coloured and sized by strength, and an optional Wind flow layer in motion.

</td>
</tr>
<tr>
<td valign="top">

**🌫️ Air quality across the map**<br>
The AQI on EPA colours: dust storms and spring pollen show up.

</td>
<td valign="top">

**🕒 The time, the season, a menu**<br>
A clock in the layer list, and `/mappeek` to set it all up in game.

</td>
</tr>
</table>


## In detail

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

1. Download the latest `MapPeek-*.jar` from the
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

<details>
<summary><b>Building</b></summary>

<br>

```bash
mvn clean verify
```

The jar is written to `target/MapPeek-<version>.jar`. MapPeek is built only on
EZvac's public API, from [JitPack](https://jitpack.io/#EZvac-Plugin/EZvac),
and the map plugins' own APIs.

</details>

## License

MIT. See [LICENSE](LICENSE).
