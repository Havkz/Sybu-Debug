# Test matrix

`gradlew.bat clean build` runs the framework-free `CoreSelfTest` as part of `check`. GitHub Actions runs the same build on Linux with Java 21.

| Test | Automated evidence | Runtime evidence |
| --- | --- | --- |
| A Survival player appears/disappears | A player-info-only anomaly remains below 30%; removal deletes the candidate. | Packet handlers compile against Minecraft 1.21.11. |
| B Visible spectator | Explicit spectator plus UUID scores at least 90% (`CONFIRMED`). | Player-info packet API verified from the 1.21.11 dependency. |
| C Spectator entity present | Live exact position is retained and direct evidence caps at 100%. | Client reaches the main menu with renderer/event classes loaded. A multiplayer visual check still requires a server spectator. |
| D Entity removed, spectator remains | Position becomes stale without losing coordinates; remove-before-gamemode ordering remains correlatable by UUID and the score stays high. | Entity-remove packet handler compiles and the addon starts. |
| E Normal leave | Candidate is removed immediately. | Player-remove packet handler compiles. |
| F Dimension change | Full engine/history reset is asserted. | Respawn packet and join/leave event handlers compile. |
| G Chunk activity only | Chunk plus generic activity remains below 30%. | These signals are deliberately not wired to packets because the client cannot attribute them reliably to a player. |
| H Panic | Threshold and one-shot behavior are asserted; snapshot iteration is mutation-safe. | Meteor `Modules.get().getActive()` and `Module.disable()` resolve and client startup succeeds. |
| I Logoff | Threshold and one-shot behavior are asserted. | Minecraft 1.21.11 `DisconnectS2CPacket` path resolves and client startup succeeds. |

The runtime smoke test uses `gradlew.bat runClient`. It has verified Fabric Loader 0.18.2, Meteor Client 1.21.11, Sybu Debug initialization, and all three locator mixin targets. It does not claim a real multiplayer spectator, panic, or disconnect was exercised without a suitable test server.

## LümmelFinder checks

The core self-test also verifies diagonal Euclidean X/Z distance (`1,1 = sqrt(2)`), near/far normalization, and ruined-portal evidence rules. The clean build compiles the real Minecraft 1.21.11 chunk, heightmap, block-state, event, and quad-rendering APIs. Runtime startup verifies module registration; representative live terrain remains the final visual/performance check because repository tests do not ship a fixed Minecraft world fixture.
