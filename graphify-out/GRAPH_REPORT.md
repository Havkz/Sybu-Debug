# Graph Report - Sybu-Debug  (2026-08-30)

## Corpus Check
- 22 files · ~5,648 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 203 nodes · 428 edges · 14 communities (12 shown, 2 thin omitted)
- Extraction: 79% EXTRACTED · 21% INFERRED · 0% AMBIGUOUS · INFERRED: 91 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `f9d66f31`
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

## God Nodes (most connected - your core abstractions)
1. `SpectatorDetector` - 40 edges
2. `DetectionCandidate` - 37 edges
3. `DetectionEngine` - 21 edges
4. `CoreSelfTest` - 14 edges
5. `DetectionSignal` - 12 edges
6. `Changelog` - 12 edges
7. `DetectionActionState` - 10 edges
8. `PacketEvidence` - 10 edges
9. `SybuDebugAddon` - 8 edges
10. `DetectionHistory` - 8 edges

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

## Communities (14 total, 2 thin omitted)

### Community 0 - "Meteor Addon Template"
Cohesion: 0.09
Nodes (20): 0.10.0, 0.11.0, 0.1.0, 0.2.0, 0.3.0, 0.4.0, 0.5.0, 0.6.0 (+12 more)

### Community 1 - "SybuDebugAddon"
Cohesion: 0.31
Nodes (6): Category, GithubRepo, Logger, MeteorAddon, Override, SybuDebugAddon

### Community 2 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 3 - "SpectatorDetector.java"
Cohesion: 0.08
Nodes (17): EventHandler, GameJoinedEvent, GameLeftEvent, Module, Packet, PlayerListS2CPacket, Post, Receive (+9 more)

### Community 5 - "Sybu Debug"
Cohesion: 0.15
Nodes (10): DetectionSignal, CHUNK_ACTIVITY, ENTITY_ACTIVITY, EXPLICIT_SPECTATOR, LIVE_POSITION, PLAYER_INFO_WITHOUT_ENTITY, RECENT_ENTITY_REMOVAL, SPECTATOR_ENTITY_REMOVED (+2 more)

### Community 6 - ".getRepo"
Cohesion: 0.50
Nodes (4): PanicMode, DISABLE_ALL_MODULES, DISABLE_SELECTED_MODULES, OFF

### Community 9 - "DetectionEngine"
Cohesion: 0.13
Nodes (5): DetectionActionState, DetectionEngine, DetectionHistory, PacketEvidence, CoreSelfTest

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
- **37 isolated node(s):** `IGNORE`, `LOW`, `POSSIBLE`, `LIKELY`, `CONFIRMED` (+32 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SpectatorDetector` connect `SpectatorDetector.java` to `Override`, `.getRepo`, `DetectionEngine`, `.handlePlayerSpawn`, `PanicMode`?**
  _High betweenness centrality (0.252) - this node is a cross-community bridge._
- **Why does `WaypointTracker` connect `PanicMode` to `SpectatorDetector.java`?**
  _High betweenness centrality (0.164) - this node is a cross-community bridge._
- **Why does `DetectionCandidate` connect `Override` to `DetectionEngine`, `.handlePlayerSpawn`, `SpectatorDetector.java`?**
  _High betweenness centrality (0.148) - this node is a cross-community bridge._
- **What connects `IGNORE`, `LOW`, `POSSIBLE` to the rest of the system?**
  _37 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Meteor Addon Template` be split into smaller, more focused modules?**
  _Cohesion score 0.08695652173913043 - nodes in this community are weakly interconnected._
- **Should `SpectatorDetector.java` be split into smaller, more focused modules?**
  _Cohesion score 0.08194905869324474 - nodes in this community are weakly interconnected._
- **Should `DetectionEngine` be split into smaller, more focused modules?**
  _Cohesion score 0.1329268292682927 - nodes in this community are weakly interconnected._