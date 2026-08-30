# Graph Report - Sybu-Debug  (2026-08-30)

## Corpus Check
- 23 files · ~6,173 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 225 nodes · 481 edges · 13 communities (11 shown, 2 thin omitted)
- Extraction: 78% EXTRACTED · 22% INFERRED · 0% AMBIGUOUS · INFERRED: 108 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c14ae77d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Meteor Addon Template
- SybuDebugAddon
- gradlew
- SpectatorDetector.java
- Override
- Sybu Debug
- DetectionEngine
- .handlePlayerSpawn
- PanicMode
- TracerMode
- PositionalTrackedWaypointAccessor.java

## God Nodes (most connected - your core abstractions)
1. `SpectatorDetector` - 42 edges
2. `DetectionCandidate` - 38 edges
3. `DetectionEngine` - 21 edges
4. `Entry` - 17 edges
5. `CoreSelfTest` - 14 edges
6. `Changelog` - 14 edges
7. `DetectionSignal` - 12 edges
8. `PlayerTracker` - 12 edges
9. `DetectionActionState` - 10 edges
10. `PacketEvidence` - 10 edges

## Surprising Connections (you probably didn't know these)
- `SpectatorDetector` --references--> `DetectionActionState`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/SpectatorDetector.java → src/main/java/com/havkz/sybudebug/detection/DetectionActionState.java
- `DetectionCandidate` --references--> `PacketEvidence`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java → src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java
- `DetectionEngine` --references--> `DetectionCandidate`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java → src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java
- `DetectionEngine` --references--> `DetectionHistory`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java → src/main/java/com/havkz/sybudebug/detection/DetectionHistory.java
- `SpectatorDetector` --references--> `DetectionEngine`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/SpectatorDetector.java → src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java

## Import Cycles
- None detected.

## Communities (13 total, 2 thin omitted)

### Community 0 - "Meteor Addon Template"
Cohesion: 0.08
Nodes (22): 0.10.0, 0.11.0, 0.12.0, 0.1.0, 0.2.0, 0.3.0, 0.4.0, 0.5.0 (+14 more)

### Community 1 - "SybuDebugAddon"
Cohesion: 0.31
Nodes (6): Category, GithubRepo, Logger, MeteorAddon, Override, SybuDebugAddon

### Community 2 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 3 - "SpectatorDetector.java"
Cohesion: 0.07
Nodes (23): EntitySpawnS2CPacket, EventHandler, GameJoinedEvent, GameLeftEvent, Module, Packet, PlayerListS2CPacket, Post (+15 more)

### Community 5 - "Sybu Debug"
Cohesion: 0.09
Nodes (12): DetectionHistory, DetectionSignal, CHUNK_ACTIVITY, ENTITY_ACTIVITY, EXPLICIT_SPECTATOR, LIVE_POSITION, PLAYER_INFO_WITHOUT_ENTITY, RECENT_ENTITY_REMOVAL (+4 more)

### Community 9 - "DetectionEngine"
Cohesion: 0.20
Nodes (3): DetectionActionState, DetectionEngine, CoreSelfTest

### Community 11 - "PanicMode"
Cohesion: 0.17
Nodes (12): AzimuthTrackedWaypointAccessor, Accessor, Mixin, ChunkTrackedWaypointAccessor, Accessor, ChunkPos, Mixin, Entry (+4 more)

### Community 12 - "TracerMode"
Cohesion: 0.53
Nodes (4): Accessor, Mixin, Vec3i, PositionalTrackedWaypointAccessor

### Community 13 - "PositionalTrackedWaypointAccessor.java"
Cohesion: 0.22
Nodes (7): ConfidenceCalculator, Level, CONFIRMED, IGNORE, LIKELY, LOW, POSSIBLE

## Knowledge Gaps
- **39 isolated node(s):** `IGNORE`, `LOW`, `POSSIBLE`, `LIKELY`, `CONFIRMED` (+34 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SpectatorDetector` connect `SpectatorDetector.java` to `DetectionEngine`, `.handlePlayerSpawn`, `PanicMode`, `Override`?**
  _High betweenness centrality (0.243) - this node is a cross-community bridge._
- **Why does `WaypointTracker` connect `PanicMode` to `.handlePlayerSpawn`, `SpectatorDetector.java`?**
  _High betweenness centrality (0.168) - this node is a cross-community bridge._
- **Why does `DetectionCandidate` connect `Override` to `DetectionEngine`, `.handlePlayerSpawn`, `SpectatorDetector.java`, `Sybu Debug`?**
  _High betweenness centrality (0.126) - this node is a cross-community bridge._
- **What connects `IGNORE`, `LOW`, `POSSIBLE` to the rest of the system?**
  _39 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Meteor Addon Template` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._
- **Should `SpectatorDetector.java` be split into smaller, more focused modules?**
  _Cohesion score 0.06976744186046512 - nodes in this community are weakly interconnected._
- **Should `Sybu Debug` be split into smaller, more focused modules?**
  _Cohesion score 0.09486166007905138 - nodes in this community are weakly interconnected._