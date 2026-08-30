# Graph Report - Sybu-Debug  (2026-08-30)

## Corpus Check
- 22 files · ~5,576 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 201 nodes · 422 edges · 15 communities (13 shown, 2 thin omitted)
- Extraction: 79% EXTRACTED · 21% INFERRED · 0% AMBIGUOUS · INFERRED: 89 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a68b39fd`
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
1. `SpectatorDetector` - 39 edges
2. `DetectionCandidate` - 37 edges
3. `DetectionEngine` - 21 edges
4. `CoreSelfTest` - 14 edges
5. `DetectionSignal` - 12 edges
6. `Changelog` - 11 edges
7. `PacketEvidence` - 10 edges
8. `DetectionActionState` - 9 edges
9. `SybuDebugAddon` - 8 edges
10. `DetectionHistory` - 8 edges

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

## Communities (15 total, 2 thin omitted)

### Community 0 - "Meteor Addon Template"
Cohesion: 0.09
Nodes (19): 0.10.0, 0.1.0, 0.2.0, 0.3.0, 0.4.0, 0.5.0, 0.6.0, 0.7.0 (+11 more)

### Community 1 - "SybuDebugAddon"
Cohesion: 0.31
Nodes (6): Category, GithubRepo, Logger, MeteorAddon, Override, SybuDebugAddon

### Community 2 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 3 - "SpectatorDetector.java"
Cohesion: 0.09
Nodes (15): EventHandler, GameJoinedEvent, GameLeftEvent, Module, Packet, PlayerListS2CPacket, Post, Receive (+7 more)

### Community 5 - "Sybu Debug"
Cohesion: 0.15
Nodes (10): DetectionSignal, CHUNK_ACTIVITY, ENTITY_ACTIVITY, EXPLICIT_SPECTATOR, LIVE_POSITION, PLAYER_INFO_WITHOUT_ENTITY, RECENT_ENTITY_REMOVAL, SPECTATOR_ENTITY_REMOVED (+2 more)

### Community 6 - ".getRepo"
Cohesion: 0.50
Nodes (4): PanicMode, DISABLE_ALL_MODULES, DISABLE_SELECTED_MODULES, OFF

### Community 9 - "DetectionEngine"
Cohesion: 0.17
Nodes (4): DetectionEngine, DetectionHistory, PacketEvidence, CoreSelfTest

### Community 11 - "PanicMode"
Cohesion: 0.17
Nodes (12): AzimuthTrackedWaypointAccessor, Accessor, Mixin, Accessor, Mixin, Vec3i, PositionalTrackedWaypointAccessor, Entry (+4 more)

### Community 12 - "TracerMode"
Cohesion: 0.53
Nodes (4): ChunkTrackedWaypointAccessor, Accessor, ChunkPos, Mixin

### Community 13 - "PositionalTrackedWaypointAccessor.java"
Cohesion: 0.22
Nodes (7): ConfidenceCalculator, Level, CONFIRMED, IGNORE, LIKELY, LOW, POSSIBLE

### Community 14 - "TracerMode"
Cohesion: 0.67
Nodes (3): TracerMode, LIVE_AND_LAST_KNOWN, LIVE_ONLY

## Knowledge Gaps
- **36 isolated node(s):** `IGNORE`, `LOW`, `POSSIBLE`, `LIKELY`, `CONFIRMED` (+31 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SpectatorDetector` connect `SpectatorDetector.java` to `Override`, `.getRepo`, `DetectionEngine`, `.handlePlayerSpawn`, `PanicMode`, `TracerMode`?**
  _High betweenness centrality (0.252) - this node is a cross-community bridge._
- **Why does `WaypointTracker` connect `PanicMode` to `SpectatorDetector.java`?**
  _High betweenness centrality (0.166) - this node is a cross-community bridge._
- **Why does `DetectionCandidate` connect `Override` to `DetectionEngine`, `.handlePlayerSpawn`, `SpectatorDetector.java`?**
  _High betweenness centrality (0.151) - this node is a cross-community bridge._
- **What connects `IGNORE`, `LOW`, `POSSIBLE` to the rest of the system?**
  _36 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Meteor Addon Template` be split into smaller, more focused modules?**
  _Cohesion score 0.09090909090909091 - nodes in this community are weakly interconnected._
- **Should `SpectatorDetector.java` be split into smaller, more focused modules?**
  _Cohesion score 0.09230769230769231 - nodes in this community are weakly interconnected._