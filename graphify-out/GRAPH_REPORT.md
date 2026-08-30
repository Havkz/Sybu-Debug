# Graph Report - Sybu-Debug  (2026-08-30)

## Corpus Check
- 19 files · ~4,628 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 163 nodes · 317 edges · 15 communities (14 shown, 1 thin omitted)
- Extraction: 80% EXTRACTED · 20% INFERRED · 0% AMBIGUOUS · INFERRED: 64 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5b12f7f0`
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
- PositionalTrackedWaypointAccessor.java
- TracerMode

## God Nodes (most connected - your core abstractions)
1. `SpectatorDetector` - 38 edges
2. `DetectionCandidate` - 36 edges
3. `DetectionEngine` - 13 edges
4. `DetectionSignal` - 12 edges
5. `PacketEvidence` - 10 edges
6. `SybuDebugAddon` - 8 edges
7. `DetectionHistory` - 8 edges
8. `Sybu Debug` - 7 edges
9. `WaypointTracker` - 6 edges
10. `PanicMode` - 4 edges

## Surprising Connections (you probably didn't know these)
- `DetectionCandidate` --references--> `PacketEvidence`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java → src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java
- `DetectionEngine` --references--> `DetectionCandidate`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java → src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java
- `SpectatorDetector` --references--> `DetectionEngine`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/SpectatorDetector.java → src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java
- `PacketEvidence` --references--> `DetectionSignal`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java → src/main/java/com/havkz/sybudebug/detection/DetectionSignal.java
- `SpectatorDetector` --references--> `WaypointTracker`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/SpectatorDetector.java → src/main/java/com/havkz/sybudebug/tracking/WaypointTracker.java

## Import Cycles
- None detected.

## Communities (15 total, 1 thin omitted)

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
Cohesion: 0.12
Nodes (13): EventHandler, GameJoinedEvent, GameLeftEvent, Module, Packet, PlayerListS2CPacket, Post, Receive (+5 more)

### Community 4 - "Override"
Cohesion: 0.12
Nodes (3): Render3DEvent, ConfidenceCalculator, DetectionCandidate

### Community 5 - "Sybu Debug"
Cohesion: 0.15
Nodes (10): DetectionSignal, CHUNK_ACTIVITY, ENTITY_ACTIVITY, EXPLICIT_SPECTATOR, LIVE_POSITION, PLAYER_INFO_WITHOUT_ENTITY, RECENT_ENTITY_REMOVAL, SPECTATOR_ENTITY_REMOVED (+2 more)

### Community 6 - ".getRepo"
Cohesion: 0.50
Nodes (4): PanicMode, DISABLE_ALL_MODULES, DISABLE_SELECTED_MODULES, OFF

### Community 9 - "DetectionEngine"
Cohesion: 0.15
Nodes (4): DetectionEngine, DetectionHistory, PacketEvidence, CoreSelfTest

### Community 11 - "PanicMode"
Cohesion: 0.23
Nodes (8): AzimuthTrackedWaypointAccessor, Accessor, Mixin, Entry, ChunkPos, Vec3i, WaypointS2CPacket, WaypointTracker

### Community 12 - "TracerMode"
Cohesion: 0.53
Nodes (4): ChunkTrackedWaypointAccessor, Accessor, ChunkPos, Mixin

### Community 13 - "PositionalTrackedWaypointAccessor.java"
Cohesion: 0.53
Nodes (4): Accessor, Mixin, Vec3i, PositionalTrackedWaypointAccessor

### Community 14 - "TracerMode"
Cohesion: 0.67
Nodes (3): TracerMode, LIVE_AND_LAST_KNOWN, LIVE_ONLY

## Knowledge Gaps
- **20 isolated node(s):** `EXPLICIT_SPECTATOR`, `SPECTATOR_WITH_UUID`, `LIVE_POSITION`, `SPECTATOR_ENTITY_REMOVED`, `WAYPOINT_CORRELATION` (+15 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SpectatorDetector` connect `SpectatorDetector.java` to `Override`, `.getRepo`, `DetectionEngine`, `.handlePlayerSpawn`, `PanicMode`, `TracerMode`?**
  _High betweenness centrality (0.313) - this node is a cross-community bridge._
- **Why does `WaypointTracker` connect `PanicMode` to `SpectatorDetector.java`?**
  _High betweenness centrality (0.212) - this node is a cross-community bridge._
- **Why does `DetectionCandidate` connect `Override` to `DetectionEngine`, `.handlePlayerSpawn`, `SpectatorDetector.java`?**
  _High betweenness centrality (0.202) - this node is a cross-community bridge._
- **What connects `EXPLICIT_SPECTATOR`, `SPECTATOR_WITH_UUID`, `LIVE_POSITION` to the rest of the system?**
  _20 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SpectatorDetector.java` be split into smaller, more focused modules?**
  _Cohesion score 0.11954022988505747 - nodes in this community are weakly interconnected._
- **Should `Override` be split into smaller, more focused modules?**
  _Cohesion score 0.12310606060606061 - nodes in this community are weakly interconnected._