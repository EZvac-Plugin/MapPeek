# Changelog

All notable changes to MapPeek are recorded here.

## 0.2.0

The wind and the air on your map. Needs EZvac BETA 1.0 Pilot 3 or newer
(API 1.8), which gave the wind a direction and regions.

- **Wind arrows on weather stations:** each working station gets an arrow at
  its centre pointing the way its wind blows, longer the stronger it is, in
  green for a breeze, amber when windy and red in a gale. A calm station has
  none. Its popup adds the wind and the air right now:
  `Wind: Breezy 18 km/h from the W`, `Air: AQI 42 (Good)`.
- **A Wind layer:** arrows every 192 blocks out to 2,048 from each world's
  spawn, showing EZvac's regional wind across the map: the trade winds over
  deserts, the westerlies over the plains, and rain turning them. Hidden
  until a viewer ticks it.
- **An Air quality layer:** squares 128 blocks across shaded in the US EPA's
  AQI colours, with the AQI and pollen on hovering: the open outdoor air,
  with its season's pollen, the rain and the wind (desert dust storms show).
  Hidden until a viewer ticks it.
- Both read EZvac's wind and air from the world seed, so they cover land no
  one has explored, and nothing is loaded. Each draws at most 2,500 shapes,
  spread further apart for a big radius.
- **The time and season:** each world with a sun shows its time of day and
  EZvac's season at the top of the map's layer list, such as
  `14:30 · Summer, day 3 of 15`. squaremap reads it as it updates, so it keeps
  time between refreshes. `clock.enabled` switches it off.
- **Settings** for both layers and the arrow colours in `config.yml`. Wind
  speeds follow `units`: mph with F, km/h with C.
- The Nether and the End have no wind layer, and with air quality switched
  off in EZvac there is no air layer.

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
