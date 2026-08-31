# Graph Report - Sybu-Debug  (2026-08-31)

## Corpus Check
- 28 files · ~9,005 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 320 nodes · 700 edges · 15 communities
- Extraction: 82% EXTRACTED · 18% INFERRED · 0% AMBIGUOUS · INFERRED: 128 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5410337d`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Meteor Addon Template
- SybuDebugAddon
- gradlew
- SpectatorDetector.java
- Override
- Sybu Debug
- ChunkActivityData
- DetectionEngine
- .handlePlayerSpawn
- PanicMode
- TracerMode
- PositionalTrackedWaypointAccessor.java
- ActivityScanner

## God Nodes (most connected - your core abstractions)
1. `SpectatorDetector` - 42 edges
2. `DetectionCandidate` - 38 edges
3. `BaseActivityDetector` - 32 edges
4. `ChunkActivityData` - 21 edges
5. `DetectionEngine` - 21 edges
6. `ActivityScanner` - 17 edges
7. `Entry` - 17 edges
8. `Changelog` - 16 edges
9. `CoreSelfTest` - 15 edges
10. `DetectionSignal` - 12 edges

## Surprising Connections (you probably didn't know these)
- `BaseActivityDetector` --references--> `ActivityPoint`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/BaseActivityDetector.java → src/main/java/com/havkz/sybudebug/activity/ActivityPoint.java
- `BaseActivityDetector` --references--> `ActivityScanner`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/BaseActivityDetector.java → src/main/java/com/havkz/sybudebug/activity/ActivityScanner.java
- `BaseActivityDetector` --references--> `ChunkActivityData`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/BaseActivityDetector.java → src/main/java/com/havkz/sybudebug/activity/ChunkActivityData.java
- `SpectatorDetector` --references--> `DetectionActionState`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/SpectatorDetector.java → src/main/java/com/havkz/sybudebug/detection/DetectionActionState.java
- `DetectionCandidate` --references--> `PacketEvidence`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java → src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java

## Import Cycles
- None detected.

## Communities (15 total, 0 thin omitted)

### Community 0 - "Meteor Addon Template"
Cohesion: 0.07
Nodes (26): 0.10.0, 0.11.0, 0.12.0, 0.1.0, 0.2.0, 0.3.0, 0.4.0, 0.5.0 (+18 more)

### Community 1 - "SybuDebugAddon"
Cohesion: 0.31
Nodes (6): Category, GithubRepo, Logger, MeteorAddon, Override, SybuDebugAddon

### Community 2 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 3 - "SpectatorDetector.java"
Cohesion: 0.07
Nodes (18): Module, Packet, PlayerListS2CPacket, Receive, SettingColor, DetectionCandidate, EventHandler, GameJoinedEvent (+10 more)

### Community 4 - "Override"
Cohesion: 0.10
Nodes (18): BlockUpdateEvent, ChunkDataEvent, BaseActivityDetector, ChunkPos, EventHandler, GameJoinedEvent, GameLeftEvent, Override (+10 more)

### Community 5 - "Sybu Debug"
Cohesion: 0.15
Nodes (10): DetectionSignal, CHUNK_ACTIVITY, ENTITY_ACTIVITY, EXPLICIT_SPECTATOR, LIVE_POSITION, PLAYER_INFO_WITHOUT_ENTITY, RECENT_ENTITY_REMOVAL, SPECTATOR_ENTITY_REMOVED (+2 more)

### Community 6 - "ChunkActivityData"
Cohesion: 0.13
Nodes (7): ActivityHeatmap, Color, ActivityPoint, BlockPos, ChunkActivityData, ChunkPos, Color

### Community 9 - "DetectionEngine"
Cohesion: 0.13
Nodes (5): DetectionActionState, DetectionEngine, DetectionHistory, PacketEvidence, CoreSelfTest

### Community 10 - ".handlePlayerSpawn"
Cohesion: 0.11
Nodes (4): EntitySpawnS2CPacket, WaypointS2CPacket, Entry, PlayerTracker

### Community 11 - "PanicMode"
Cohesion: 0.13
Nodes (16): AzimuthTrackedWaypointAccessor, Accessor, Mixin, ChunkTrackedWaypointAccessor, Accessor, ChunkPos, Mixin, Accessor (+8 more)

### Community 12 - "TracerMode"
Cohesion: 0.50
Nodes (4): PanicMode, DISABLE_ALL_MODULES, DISABLE_SELECTED_MODULES, OFF

### Community 13 - "PositionalTrackedWaypointAccessor.java"
Cohesion: 0.22
Nodes (7): ConfidenceCalculator, Level, CONFIRMED, IGNORE, LIKELY, LOW, POSSIBLE

### Community 14 - "ActivityScanner"
Cohesion: 0.21
Nodes (10): BlockState, Chunk, ClientWorld, ActivityType, HOLE, OBSIDIAN, ActivityScanner, Context (+2 more)

## Knowledge Gaps
- **48 isolated node(s):** `HOLE`, `OBSIDIAN`, `IGNORE`, `LOW`, `POSSIBLE` (+43 more)
  These have ≤1 connection - possible missing edges or undocumented components.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SpectatorDetector` connect `SpectatorDetector.java` to `DetectionEngine`, `.handlePlayerSpawn`, `PanicMode`, `TracerMode`?**
  _High betweenness centrality (0.250) - this node is a cross-community bridge._
- **Why does `BaseActivityDetector` connect `Override` to `SpectatorDetector.java`, `ChunkActivityData`, `ActivityScanner`?**
  _High betweenness centrality (0.184) - this node is a cross-community bridge._
- **Why does `WaypointTracker` connect `PanicMode` to `.handlePlayerSpawn`, `SpectatorDetector.java`?**
  _High betweenness centrality (0.128) - this node is a cross-community bridge._
- **What connects `HOLE`, `OBSIDIAN`, `IGNORE` to the rest of the system?**
  _48 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Meteor Addon Template` be split into smaller, more focused modules?**
  _Cohesion score 0.06896551724137931 - nodes in this community are weakly interconnected._
- **Should `SpectatorDetector.java` be split into smaller, more focused modules?**
  _Cohesion score 0.06768905341089371 - nodes in this community are weakly interconnected._
- **Should `Override` be split into smaller, more focused modules?**
  _Cohesion score 0.10384615384615385 - nodes in this community are weakly interconnected._