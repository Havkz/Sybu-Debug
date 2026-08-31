# Changelog

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
