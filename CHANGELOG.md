# Changelog

All notable changes to MapPeek are recorded here.

## 0.2.0

The wind and the air on your map, BlueMap support, and a menu. Needs EZvac
BETA 1.0 Pilot 3 or newer (API 1.8), which gave the wind a direction and
regions.

- **BlueMap:** MapPeek now draws on BlueMap too, or on both maps at once:
  stations as icons with their coverage and details, wind arrows, AQI squares
  over the terrain and the time and season, as marker sets in BlueMap's
  menu. It picks BlueMap up whenever it starts or reloads.
- **`/mappeek`** (permission `mappeek.admin`, ops by default): a menu to
  switch each layer on or off, choose whether it starts hidden, the units,
  the two refresh periods and the area. Changes are saved to `config.yml`,
  comments kept, and shown at once. `/mappeek reload` reads the file again.
- **Weather stations** get an icon, so they're easy to spot, and an arrow
  pointing the way their wind blows, longer the stronger it is, in green for
  a breeze, amber when windy and red in a gale. Their popup adds the wind and
  air right now with a coloured AQI badge, and a three-day outlook.
- **A Wind layer:** arrows every 192 blocks, showing EZvac's regional wind:
  the trade winds over deserts, the westerlies over the plains, and rain
  turning them. Calm spots are small grey dots. It refreshes every 15 seconds
  (`wind.refresh-seconds`), more often than the rest. Hidden until a viewer
  ticks it.
- **An Air quality layer:** squares 128 blocks across, shaded on a smooth
  scale through the US EPA's AQI colours so neighbouring squares don't flip
  colour at a band's edge, with the AQI, PM2.5, PM10 and pollen on hovering.
  Desert dust storms and spring pollen show. Hidden until a viewer ticks it.
- **Both cover the whole explored map** (`area: explored`, read from the
  world's region files), or a square around spawn (`area: spawn`). They read
  EZvac's wind and air from the world seed, so nothing is loaded. Each draws
  at most 2,500 shapes, spread further apart on a very big map.
- **The time and season:** each world with a sun shows its time of day and
  EZvac's season in the map's layer list, such as
  `14:30 · Summer, day 3 of 15`, keeping time between refreshes.
  `clock.enabled` switches it off.
- Wind speeds follow `units`: mph with F, km/h with C. The Nether and the End
  have no wind layer, and with air quality switched off in EZvac there is no
  air layer.

## 0.1.0

The first release: EZvac on your web map.

- **Weather station coverage on squaremap.** Every EZvac weather station is a
  circle of the area it measures, on a "Weather stations" layer of each
  world's map, coloured by what it's doing: working, switched off, or
  damaged. Hovering names it; clicking shows today's weather, high and low,
  what's certain about it, the area it measures, and its lightning strikes.
- **Kept up to date** every 60 seconds (`refresh-seconds`).
- **Settings** in `config.yml`: the layer's name, whether it starts hidden,
  the colours and fill, and °F or °C.
- Needs EZvac BETA 1.0 Pilot 2 or newer (API 1.7) and squaremap. Without
  squaremap it stays idle and says so.
