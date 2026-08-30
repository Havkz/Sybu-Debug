# Graph Report - Sybu-Debug  (2026-08-30)

## Corpus Check
- 7 files · ~2,678 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 33 nodes · 38 edges · 9 communities (6 shown, 3 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Meteor Addon Template
- SybuDebugAddon
- gradlew
- SpectatorDetector.java
- Override
- Sybu Debug
- .getRepo

## God Nodes (most connected - your core abstractions)
1. `SybuDebugAddon` - 8 edges
2. `Meteor Addon Template` - 5 edges
3. `How to use` - 4 edges
4. `SpectatorDetector` - 3 edges
5. `Sybu Debug` - 3 edges
6. `Build` - 1 edges
7. `Limits` - 1 edges
8. `Use GitHub Template (Recommended)` - 1 edges
9. `Clone Manually` - 1 edges
10. `Development` - 1 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Import Cycles
- None detected.

## Communities (9 total, 3 thin omitted)

### Community 0 - "Meteor Addon Template"
Cohesion: 0.25
Nodes (8): Clone Manually, Development, How to use, License, Meteor Addon Template, Project structure, Updating to newer Minecraft versions, Use GitHub Template (Recommended)

### Community 1 - "SybuDebugAddon"
Cohesion: 0.70
Nodes (4): Category, Logger, MeteorAddon, SybuDebugAddon

### Community 2 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 5 - "Sybu Debug"
Cohesion: 0.50
Nodes (3): Build, Limits, Sybu Debug

## Knowledge Gaps
- **8 isolated node(s):** `Build`, `Limits`, `Use GitHub Template (Recommended)`, `Clone Manually`, `Development` (+3 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **3 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Meteor Addon Template` connect `Meteor Addon Template` to `Sybu Debug`?**
  _High betweenness centrality (0.087) - this node is a cross-community bridge._
- **Why does `SybuDebugAddon` connect `SybuDebugAddon` to `Override`, `.getRepo`?**
  _High betweenness centrality (0.087) - this node is a cross-community bridge._
- **What connects `Build`, `Limits`, `Use GitHub Template (Recommended)` to the rest of the system?**
  _8 weakly-connected nodes found - possible documentation gaps or missing edges._