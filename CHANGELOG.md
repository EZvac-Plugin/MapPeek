# Changelog

All notable changes to MapPeek are recorded here.

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
