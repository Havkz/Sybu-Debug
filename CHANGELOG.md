# Changelog

## 1.9.0

- Renamed the visible module from LümmelFinder to BaseGrid to describe its inactivity-based probability raster.
- Added a standard Meteor grid-line color setting with independent transparency.
- Spread radius and activity recoloring across four chunks per tick to prevent loaded-world-wide FPS spikes.

## 1.8.0

- Removed Smooth Layer and its per-chunk height/color raster work.
- Kept only flat binary Chunk Based rendering at one shared Y level.
- Replaced per-chunk border lines with one shared grid, reducing line count from proportional to chunk count to proportional to view diameter.
- Merged adjacent same-color chunks into row runs, reducing overlay quads from proportional to chunk count to roughly proportional to view diameter.

## 1.7.0

- Rendered every Chunk Based quad at one shared whole-block Y coordinate derived from the average ground height of all visible loaded chunks.
- Added efficient chunk boundary lines while avoiding duplicate shared edges.
- Made recoloring independent of red-radius size by scanning only loaded chunks and collecting activity points once per recolor pass.

## 1.6.0

- Removed the redundant overlay-opacity slider; transparency comes directly from the two Meteor color pickers.
- Redefined Chunk Based as a flat binary overlay: activity color only when more than half of the 16x16 chunk area lies within the configured red radius.
- Removed gradients and terrain-following geometry from Chunk Based while keeping both in Smooth Layer.
- Restored the previous one-quad-per-chunk rendering cost for Smooth Layer and stopped recoloring unaffected chunks during streaming.

## 1.5.0

- Replaced the separate overlay-alpha slider with standard Meteor color pickers for activity and untouched terrain.
- Both endpoint colors include independent transparency and are blended across the configured red radius.
- Replaced neighbor-based ground filtering with per-column descent toward a robust chunk-wide average.
- Unified four-chunk corner heights to prevent holes in the coarse carpet mesh.
- Added Smooth Layer and single-quad Chunk Based rendering modes.
- Extended red radius input to 2048 meters and added a global 0-100 percent overlay opacity control.

## 1.4.0

- Ignored tall positive Y outliers before smoothing so buildings and pillars no longer lift the carpet.
- Added a configurable red activity radius in blocks/meters.
- Guaranteed that every loaded rendered chunk is queued, including retry and mid-scan block-update cases.
- Reduced the overlay to pixel-style 8x8-block tiles, cutting rendered quads to one quarter of 1.3.0.

## 1.3.0

- Increased scanning to five workers and five player-prioritized chunks per tick.
- Reduced the normal hole snapshot from the full world height to a narrow surface band; full-height scanning now runs only when optional obsidian activity is enabled.
- Made obsidian activity disabled by default and reduced the module to three settings.
- Switched the carpet to Minecraft's render distance and retained it for the complete loaded lifetime of each chunk.
- Rebuilt coloring as an immediate 64-block red activity aura with green used only outside known activity range.
- Fixed open shafts using their low heightmap floor by deriving the hole search level from the smoothed neighboring surface.

## 1.2.0

- Prioritized newly loaded chunks outward from the player's current position.
- Moved full hole and obsidian analysis to bounded background workers.
- Rejected unsupported sky structures when deriving terrain height and smoothed surface samples and chunk seams.
- Reduced false positives by requiring five-block-deep, surface-near enclosed holes.

## 1.1.1

- Renamed the visible activity module to `LümmelFinder`.

## 1.1.0

- Added `BaseActivityDetector` with incremental hole and obsidian activity scanning.
- Added ruined-portal filtering, cached Euclidean proximity heatmaps, and deduplicated low-activity region notifications.

## 1.0.0

- Finalized the exact `SpectatorDetector` GUI name and `Sybu Debug by V0trex` branding.
- Prevented stale tracked-player coordinates from becoming live positions again.
- Completed the documented A-I checks, release workflow, and final Minecraft 1.21.11 build.

## 0.12.0

- Track exact positions for all client-spawned players without scanning the world.
- Correlate entity removal up to 500 ms before a spectator game-mode update.

## 0.11.0

- Reset warning and one-shot action state when a spectator returns to a normal game mode.
- Reset anomaly-scan timing on join, respawn, world change, and disconnect.

## 0.10.0

- Added explicit automated scenarios A-I and one-shot action-state tests.
- Changed the development release to one stable snapshot JAR.

## 0.9.0

- Branded the addon as Sybu Debug by V0trex.

## 0.8.0

- Updated GitHub Actions to maintained Node 24-based releases.

## 0.7.0

- Fixed Linux CI and persistent spectator-state refresh.

## 0.6.0

- Added range and lifecycle false-positive guards.

## 0.5.0

- Added Minecraft 1.21.11 locator/waypoint correlation.

## 0.4.0

- Added warnings, rendering, friend filtering, panic, and clean logoff.

## 0.3.0

- Added direct player-info and entity lifecycle tracking.

## 0.2.0

- Added candidate, evidence, confidence, decay, and bounded history core.

## 0.1.0

- Added the buildable Minecraft 1.21.11 Meteor addon shell.
