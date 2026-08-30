# Graph Report - Sybu-Debug  (2026-08-30)

## Corpus Check
- 14 files · ~3,624 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 109 nodes · 169 edges · 10 communities (8 shown, 2 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 23 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `58692ca2`
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

## God Nodes (most connected - your core abstractions)
1. `DetectionCandidate` - 22 edges
2. `SpectatorDetector` - 16 edges
3. `DetectionEngine` - 13 edges
4. `DetectionSignal` - 12 edges
5. `PacketEvidence` - 10 edges
6. `SybuDebugAddon` - 8 edges
7. `DetectionHistory` - 8 edges
8. `Meteor Addon Template` - 5 edges
9. `How to use` - 4 edges
10. `ConfidenceCalculator` - 3 edges

## Surprising Connections (you probably didn't know these)
- `DetectionCandidate` --references--> `PacketEvidence`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java → src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java
- `DetectionEngine` --references--> `DetectionCandidate`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java → src/main/java/com/havkz/sybudebug/detection/DetectionCandidate.java
- `DetectionEngine` --references--> `DetectionHistory`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java → src/main/java/com/havkz/sybudebug/detection/DetectionHistory.java
- `SpectatorDetector` --references--> `DetectionEngine`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/modules/SpectatorDetector.java → src/main/java/com/havkz/sybudebug/detection/DetectionEngine.java
- `PacketEvidence` --references--> `DetectionSignal`  [EXTRACTED]
  src/main/java/com/havkz/sybudebug/detection/PacketEvidence.java → src/main/java/com/havkz/sybudebug/detection/DetectionSignal.java

## Import Cycles
- None detected.

## Communities (10 total, 2 thin omitted)

### Community 0 - "Meteor Addon Template"
Cohesion: 0.17
Nodes (11): Build, Clone Manually, Development, How to use, License, Limits, Meteor Addon Template, Project structure (+3 more)

### Community 1 - "SybuDebugAddon"
Cohesion: 0.31
Nodes (6): Category, GithubRepo, Logger, MeteorAddon, Override, SybuDebugAddon

### Community 2 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 3 - "SpectatorDetector.java"
Cohesion: 0.14
Nodes (9): EventHandler, GameJoinedEvent, GameLeftEvent, Module, Packet, PlayerListS2CPacket, Receive, Override (+1 more)

### Community 4 - "Override"
Cohesion: 0.10
Nodes (3): EntitySpawnS2CPacket, ConfidenceCalculator, DetectionCandidate

### Community 5 - "Sybu Debug"
Cohesion: 0.15
Nodes (10): DetectionSignal, CHUNK_ACTIVITY, ENTITY_ACTIVITY, EXPLICIT_SPECTATOR, LIVE_POSITION, PLAYER_INFO_WITHOUT_ENTITY, RECENT_ENTITY_REMOVAL, SPECTATOR_ENTITY_REMOVED (+2 more)

## Knowledge Gaps
- **17 isolated node(s):** `EXPLICIT_SPECTATOR`, `SPECTATOR_WITH_UUID`, `LIVE_POSITION`, `SPECTATOR_ENTITY_REMOVED`, `WAYPOINT_CORRELATION` (+12 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DetectionCandidate` connect `Override` to `DetectionEngine`, `SpectatorDetector.java`, `.getRepo`?**
  _High betweenness centrality (0.216) - this node is a cross-community bridge._
- **Why does `DetectionEngine` connect `DetectionEngine` to `SpectatorDetector.java`, `Override`, `.getRepo`?**
  _High betweenness centrality (0.207) - this node is a cross-community bridge._
- **Why does `DetectionSignal` connect `Sybu Debug` to `.getRepo`?**
  _High betweenness centrality (0.146) - this node is a cross-community bridge._
- **What connects `EXPLICIT_SPECTATOR`, `SPECTATOR_WITH_UUID`, `LIVE_POSITION` to the rest of the system?**
  _17 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `SpectatorDetector.java` be split into smaller, more focused modules?**
  _Cohesion score 0.14492753623188406 - nodes in this community are weakly interconnected._
- **Should `Override` be split into smaller, more focused modules?**
  _Cohesion score 0.1038961038961039 - nodes in this community are weakly interconnected._