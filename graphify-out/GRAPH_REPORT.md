# Graph Report - Sybu-Debug  (2026-09-01)

## Corpus Check
- 28 files · ~11,298 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 354 nodes · 784 edges · 16 communities (15 shown, 1 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 120 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `efe4621c`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Meteor Addon Template
- SybuDebugAddon
- gradlew
- SpectatorDetector.java
- ChunkTrackedWaypointAccessor.java
- Sybu Debug
- ChunkActivityData
- DetectionEngine
- PositionalTrackedWaypointAccessor.java
- PanicMode
- PositionalTrackedWaypointAccessor.java
- PositionalTrackedWaypointAccessor.java
- ActivityScanner
- .testActivityMathAndPortalFilter

## God Nodes (most connected - your core abstractions)
1. `SpectatorDetector` - 42 edges
2. `BaseActivityDetector` - 39 edges
3. `DetectionCandidate` - 38 edges
4. `Changelog` - 24 edges
5. `ChunkActivityData` - 23 edges
6. `ActivityScanner` - 21 edges
7. `DetectionEngine` - 21 edges
8. `CoreSelfTest` - 18 edges
9. `Entry` - 17 edges
10. `DetectionSignal` - 12 edges

## Surprising Connections (you probably didn't know these)
- `SpectatorDetector` --references--> `DetectionActionState`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/SpectatorDetector.java → src/main/java/com/havkz/sybudebug/detection/DetectionActionState.java
- `DetectionCandidate` --references--> `PacketEvidence`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java → src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java
- `DetectionEngine` --references--> `DetectionCandidate`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java → src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java
- `SpectatorDetector` --references--> `DetectionEngine`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/SpectatorDetector.java → src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java
- `PacketEvidence` --references--> `DetectionSignal`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java → src/main/java/com/havkz/sybudebug/detection/DetectionSignal.java

## Import Cycles
- None detected.

## Communities (16 total, 1 thin omitted)

### Community 0 - "Meteor Addon Template"
Cohesion: 0.05
Nodes (34): 0.10.0, 0.11.0, 0.12.0, 0.1.0, 0.2.0, 0.3.0, 0.4.0, 0.5.0 (+26 more)

### Community 1 - "SybuDebugAddon"
Cohesion: 0.31
Nodes (6): Category, GithubRepo, Logger, MeteorAddon, Override, SybuDebugAddon

### Community 2 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 3 - "SpectatorDetector.java"
Cohesion: 0.06
Nodes (20): EntitySpawnS2CPacket, Module, Packet, PlayerListS2CPacket, Receive, DetectionCandidate, EventHandler, GameJoinedEvent (+12 more)

### Community 4 - "ChunkTrackedWaypointAccessor.java"
Cohesion: 0.50
Nodes (4): PanicMode, DISABLE_ALL_MODULES, DISABLE_SELECTED_MODULES, OFF

### Community 5 - "Sybu Debug"
Cohesion: 0.15
Nodes (10): DetectionSignal, CHUNK_ACTIVITY, ENTITY_ACTIVITY, EXPLICIT_SPECTATOR, LIVE_POSITION, PLAYER_INFO_WITHOUT_ENTITY, RECENT_ENTITY_REMOVAL, SPECTATOR_ENTITY_REMOVED (+2 more)

### Community 6 - "ChunkActivityData"
Cohesion: 0.07
Nodes (20): BlockUpdateEvent, ChunkDataEvent, ActivityPoint, BlockPos, ChunkActivityData, ChunkPos, Color, BaseActivityDetector (+12 more)

### Community 9 - "DetectionEngine"
Cohesion: 0.13
Nodes (5): DetectionActionState, DetectionEngine, DetectionHistory, PacketEvidence, CoreSelfTest

### Community 10 - "PositionalTrackedWaypointAccessor.java"
Cohesion: 0.53
Nodes (4): ChunkTrackedWaypointAccessor, Accessor, ChunkPos, Mixin

### Community 11 - "PanicMode"
Cohesion: 0.10
Nodes (10): AzimuthTrackedWaypointAccessor, Accessor, Mixin, Entry, PlayerTracker, Entry, ChunkPos, Vec3i (+2 more)

### Community 12 - "PositionalTrackedWaypointAccessor.java"
Cohesion: 0.53
Nodes (4): Accessor, Mixin, Vec3i, PositionalTrackedWaypointAccessor

### Community 13 - "PositionalTrackedWaypointAccessor.java"
Cohesion: 0.22
Nodes (7): ConfidenceCalculator, Level, CONFIRMED, IGNORE, LIKELY, LOW, POSSIBLE

### Community 14 - "ActivityScanner"
Cohesion: 0.14
Nodes (12): BlockState, ChunkSection, ClientWorld, Mutable, ActivityType, HOLE, OBSIDIAN, ActivityScanner (+4 more)

## Knowledge Gaps
- **52 isolated node(s):** `HOLE`, `OBSIDIAN`, `IGNORE`, `LOW`, `POSSIBLE` (+47 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SpectatorDetector` connect `SpectatorDetector.java` to `DetectionEngine`, `PanicMode`, `ChunkTrackedWaypointAccessor.java`?**
  _High betweenness centrality (0.226) - this node is a cross-community bridge._
- **Why does `BaseActivityDetector` connect `ChunkActivityData` to `SpectatorDetector.java`?**
  _High betweenness centrality (0.175) - this node is a cross-community bridge._
- **Why does `WaypointTracker` connect `PanicMode` to `SpectatorDetector.java`?**
  _High betweenness centrality (0.115) - this node is a cross-community bridge._
- **What connects `HOLE`, `OBSIDIAN`, `IGNORE` to the rest of the system?**
  _52 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Meteor Addon Template` be split into smaller, more focused modules?**
  _Cohesion score 0.05405405405405406 - nodes in this community are weakly interconnected._
- **Should `SpectatorDetector.java` be split into smaller, more focused modules?**
  _Cohesion score 0.06210526315789474 - nodes in this community are weakly interconnected._
- **Should `ChunkActivityData` be split into smaller, more focused modules?**
  _Cohesion score 0.06749482401656315 - nodes in this community are weakly interconnected._