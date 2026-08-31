# Sybu Debug by V0trex

Meteor Client addon by **V0trex** for Minecraft Java Edition **1.21.11**. It adds the exact Meteor category **Sybu Debug** containing **SpectatorDetector** and **LümmelFinder**.

## Installation

1. Install Fabric Loader `0.18.2`, Meteor Client `1.21.11` (verified build `86`), and Minecraft `1.21.11`.
2. Download the JAR from the [latest GitHub release](https://github.com/Havkz/Sybu-Debug/releases/latest).
3. Put it beside Meteor Client in the Minecraft `mods` directory.
4. Open Meteor's GUI, select `Sybu Debug`, and enable the desired module.

Java 21 is required. To build locally, run `gradlew.bat clean build`; the remapped addon JAR is written to `build/libs`.

See [TESTING.md](TESTING.md) for the A-I verification matrix and [CHANGELOG.md](CHANGELOG.md) for the version history.

## Detection

The detector maintains one candidate per UUID and uses bounded packet evidence with time-based decay. Current direct signals include:

- explicit `SPECTATOR` game mode in player-info updates;
- UUID and player name from player info;
- client-tracked player entity ID and exact position;
- correlation between a spectator game-mode update and entity removal in either packet order within 500 ms;
- Minecraft 1.21.11 locator/waypoint track, update, and untrack correlation by UUID;
- weak player-info-without-entity anomalies that remain far below warning thresholds on their own;
- cleanup when player info is removed or game mode changes away from spectator.

The system never invents coordinates. A live position becomes **Last Known** when the entity is removed and expires after 15 seconds.

## Rendering and warnings

Exact positions can show a 3D tracer, box, name, distance, confidence, and live/last-known state. Positionless detections produce a chat warning without a fake distance. Confidence thresholds, distance, tracer mode, and warning cooldown are configurable in `SpectatorDetector`.

## Actions

`Panic On Detect` can disable all active modules or only a selected list. The active module collection is copied before any module is disabled, preventing concurrent modification. `Keep Detector Active` defaults to true.

`Logoff On Detect` cleanly disconnects with `SpectatorDetector: spectator detected`; it never closes Minecraft. Warning, panic, and disconnect run in that order and panic/logoff trigger at most once per candidate.

## LümmelFinder activity heatmap

`LümmelFinder` passively analyzes normal client-visible chunk data. It treats enclosed vertical holes and, when enabled, visible obsidian as historical player-activity markers. `Smooth Layer` renders one terrain-following quad per chunk with a corner-to-corner color gradient. `Chunk Based` has no gradients or surface mesh: every loaded chunk is drawn at one shared whole-block Y coordinate derived from their average ground height, with visible boundary lines. It uses the activity color only when more than 50 percent of its 16x16 area lies inside the configured red radius. Activity and untouched colors, including transparency, use standard Meteor color pickers. Its range follows Minecraft's render distance, and cached carpet data remains until Minecraft unloads the chunk. The untouched color means only **low visible activity**, never a confirmed base.

Likely ruined-portal obsidian is ignored when its 5-block X/Y/Z neighborhood contains crying obsidian or a combination of netherrack with magma/lava. The module uses no seed, probing packets, anti-xray bypass, tunnels, or stairs.

Analysis is driven by Meteor's `ChunkDataEvent` and `BlockUpdateEvent`. One queued chunk is scanned per tick on the client thread; surface heights, activity points, squared Euclidean X/Z distances, colors, and candidate state are cached. Rendering reads those caches only. Unloaded or distant chunks are pruned and the cache is capped at 2048 chunks.

The installed Glazed `HoleTunnelStairsESP` was inspected. Only its hole collection has a public getter; its tunnel/stair collections and chunk cache are private, require the Glazed module to run, and provide no stable integration API. The hole geometry is therefore implemented independently so Sybu Debug remains standalone.

## Known limits

A correctly implemented server-side vanish can withhold every player-specific signal. This addon cannot reconstruct information the server never sends. Direct packet leaks are reliable. Locator packets are correlated only when they expose the same UUID; exact positional waypoints are used directly, while chunk and azimuth waypoints are stored but never converted into fake coordinates. Chunk, simulation, and generic activity correlations are not claimed as spectator detections. Network lag, respawns, dimension changes, server plugins, NPCs, and packet bursts can still create transient evidence; join/server-change state is cleared and guarded by a short grace period. Base activity is inherently heuristic: natural holes, unusual terrain, incomplete loaded-area coverage, and player-built portal decorations can affect the heatmap.

## License

CC0-1.0. The project began from the official Meteor addon template.
