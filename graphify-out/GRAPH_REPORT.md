# Graph Report - Sybu-Debug  (2026-08-30)

## Corpus Check
- 14 files · ~4,032 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 132 nodes · 248 edges · 13 communities (10 shown, 3 thin omitted)
- Extraction: 81% EXTRACTED · 19% INFERRED · 0% AMBIGUOUS · INFERRED: 47 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e5c371fd`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Meteor Addon Template
- SybuDebugAddon
- gradlew
- SpectatorDetector.java
- Override
- Sybu Debug
- .getRepo
- DetectionEngine
- .handlePlayerSpawn
- PanicMode
- TracerMode

## God Nodes (most connected - your core abstractions)
1. `SpectatorDetector` - 34 edges
2. `DetectionCandidate` - 33 edges
3. `DetectionEngine` - 13 edges
4. `DetectionSignal` - 12 edges
5. `PacketEvidence` - 10 edges
6. `SybuDebugAddon` - 8 edges
7. `DetectionHistory` - 8 edges
8. `Sybu Debug` - 7 edges
9. `PanicMode` - 4 edges
10. `ConfidenceCalculator` - 3 edges

## Surprising Connections (you probably didn't know these)
- `DetectionCandidate` --references--> `PacketEvidence`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java → src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java
- `DetectionEngine` --references--> `DetectionCandidate`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java → src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java
- `SpectatorDetector` --references--> `DetectionEngine`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/SpectatorDetector.java → src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java
- `PacketEvidence` --references--> `DetectionSignal`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java → src/main/java/com/havkz/sybudebug/detection/DetectionSignal.java
- `DetectionEngine` --references--> `DetectionHistory`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java → src/main/java/com/havkz/sybudebug/detection/DetectionHistory.java

## Import Cycles
- None detected.

## Communities (13 total, 3 thin omitted)

### Community 0 - "Meteor Addon Template"
Cohesion: 0.25
Nodes (7): Actions, Detection, Installation, Known limits, License, Rendering and warnings, Sybu Debug

### Community 1 - "SybuDebugAddon"
Cohesion: 0.31
Nodes (6): Category, GithubRepo, Logger, MeteorAddon, Override, SybuDebugAddon

### Community 2 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 3 - "SpectatorDetector.java"
Cohesion: 0.13
Nodes (13): EventHandler, GameJoinedEvent, GameLeftEvent, Module, Packet, PlayerListS2CPacket, Post, Receive (+5 more)

### Community 5 - "Sybu Debug"
Cohesion: 0.15
Nodes (10): DetectionSignal, CHUNK_ACTIVITY, ENTITY_ACTIVITY, EXPLICIT_SPECTATOR, LIVE_POSITION, PLAYER_INFO_WITHOUT_ENTITY, RECENT_ENTITY_REMOVAL, SPECTATOR_ENTITY_REMOVED (+2 more)

### Community 9 - "DetectionEngine"
Cohesion: 0.16
Nodes (4): DetectionEngine, DetectionHistory, PacketEvidence, CoreSelfTest

### Community 11 - "PanicMode"
Cohesion: 0.50
Nodes (4): PanicMode, DisableAllModules, DisableSelectedModules, Off

### Community 12 - "TracerMode"
Cohesion: 0.67
Nodes (3): TracerMode, LiveAndLastKnown, LiveOnly

## Knowledge Gaps
- **20 isolated node(s):** `EXPLICIT_SPECTATOR`, `SPECTATOR_WITH_UUID`, `LIVE_POSITION`, `SPECTATOR_ENTITY_REMOVED`, `WAYPOINT_CORRELATION` (+15 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SpectatorDetector` connect `SpectatorDetector.java` to `Override`, `.getRepo`, `DetectionEngine`, `.handlePlayerSpawn`, `PanicMode`, `TracerMode`?**
  _High betweenness centrality (0.289) - this node is a cross-community bridge._
- **Why does `DetectionCandidate` connect `Override` to `DetectionEngine`, `.handlePlayerSpawn`, `SpectatorDetector.java`, `.getRepo`?**
  _High betweenness centrality (0.239) - this node is a cross-community bridge._
- **Why does `DetectionSignal` connect `Sybu Debug` to `DetectionEngine`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **What connects `EXPLICIT_SPECTATOR`, `SPECTATOR_WITH_UUID`, `LIVE_POSITION` to the rest of the system?**
  _20 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SpectatorDetector.java` be split into smaller, more focused modules?**
  _Cohesion score 0.12535612535612536 - nodes in this community are weakly interconnected._