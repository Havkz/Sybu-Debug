# Sybu Debug

Meteor Client addon for Minecraft Java Edition **1.21.11**. It adds the exact Meteor category **Sybu Debug** containing the single module **SpectatorDetector**.

## Installation

1. Install Fabric Loader `0.18.2`, Meteor Client `1.21.11`, and Minecraft `1.21.11`.
2. Download the JAR from the latest GitHub release.
3. Put it beside Meteor Client in the Minecraft `mods` directory.
4. Open Meteor's GUI, select `Sybu Debug`, and enable `SpectatorDetector`.

Java 21 is required. To build locally, run `gradlew.bat clean build`; the remapped addon JAR is written to `build/libs`.

## Detection

The detector maintains one candidate per UUID and uses bounded packet evidence with time-based decay. Current direct signals include:

- explicit `SPECTATOR` game mode in player-info updates;
- UUID and player name from player info;
- client-tracked player entity ID and exact position;
- correlation between a known spectator and entity removal;
- cleanup when player info is removed or game mode changes away from spectator.

The system never invents coordinates. A live position becomes **Last Known** when the entity is removed and expires after 15 seconds.

## Rendering and warnings

Exact positions can show a 3D tracer, box, name, distance, confidence, and live/last-known state. Positionless detections produce a chat warning without a fake distance. Confidence thresholds, distance, tracer mode, and warning cooldown are configurable in `SpectatorDetector`.

## Actions

`Panic On Detect` can disable all active modules or only a selected list. The active module collection is copied before any module is disabled, preventing concurrent modification. `Keep Detector Active` defaults to true.

`Logoff On Detect` cleanly disconnects with `SpectatorDetector: spectator detected`; it never closes Minecraft. Warning, panic, and disconnect run in that order and panic/logoff trigger at most once per candidate.

## Known limits

A correctly implemented server-side vanish can withhold every player-specific signal. This addon cannot reconstruct information the server never sends. Direct packet leaks are reliable; locator, chunk, simulation, and activity correlations require additional caution and are not claimed as detections until implemented and verified. Network lag, respawns, dimension changes, server plugins, NPCs, and packet bursts can still create transient evidence; join/server-change state is cleared and guarded by a short grace period.

## License

CC0-1.0. The project began from the official Meteor addon template.
