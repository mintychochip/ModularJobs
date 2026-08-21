# DataBag Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move ModularJobs from the obsolete split `dev.conditions`/`dev.databag` artifacts to the consolidated sibling `../databag` API and remove duplicate local condition implementations.

**Architecture:** Consume `dev.mintychochip.databag:databag-api`, `databag-common`, and `databag-paper` from the existing `databagLocal` repository. Keep ModularJobs boost rules, arithmetic, aggregation, persistence envelopes, and payment behavior; delegate condition graphs, serialization, snapshots, and primitive bags to DataBag.

**Tech Stack:** Java 21/25, Gradle Kotlin DSL, DataBag `0.0.0-SNAPSHOT`, JUnit 5, Paper 26.2.

## Global Constraints

- Preserve the public ModularJobs boost API during this dependency migration.
- Do not change serialized boost JSON or DataBag payload format IDs.
- Remove all production and test references to `dev.conditions` and `dev.databag`.
- Keep `SnapshotCondition` as the narrow adapter from ModularJobs `Condition` to DataBag `Condition`.
- Delete legacy Bukkit-backed ModularJobs condition implementations after a compile checkpoint.

---

### Task 1: Consolidated dependencies and imports

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `api/build.gradle.kts`
- Modify: `common/build.gradle.kts`
- Modify: `paper/build.gradle.kts`
- Modify: Java sources importing `dev.conditions.*` or `dev.databag.*`

**Interfaces:**
- Consumes: DataBag artifacts published by `../databag`.
- Produces: all existing ModularJobs call sites compiled against `dev.mintychochip.databag.*`.

- [ ] Replace version catalog entries with `databag-common`, `databag-api`, and `databag-paper` coordinates.
- [ ] Point module dependencies at the consolidated artifacts.
- [ ] Migrate package imports and fully-qualified references.
- [ ] Run `./gradlew :api:compileJava :common:compileJava :paper:compileJava` and require success.

### Task 2: Delete legacy condition engine

**Files:**
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/AlwaysTrueConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/BiomeConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/ComposableConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/Conditions.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/JobConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/LiquidConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/NegatingConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/PlayerResourceConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/PotionConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/PotionTypeConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/SneakConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/SprintConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/WeatherConditionImpl.java`
- Delete: `paper/src/main/java/net/aincraft/boost/conditions/WorldConditionImpl.java`
- Keep: `paper/src/main/java/net/aincraft/boost/conditions/SnapshotCondition.java`
- Modify: `paper/src/main/java/net/aincraft/boost/config/BoostSourceConfigSerializer.java`
- Modify: affected tests and stale Javadocs.

**Interfaces:**
- Consumes: `SnapshotCondition(dev.mintychochip.databag.Condition)`.
- Produces: one condition implementation path backed by DataBag.

- [ ] Remove serializer branches for legacy implementations; require `SnapshotCondition`.
- [ ] Update tests to construct conditions through `BoostFactoryImpl` or DataBag `Conditions`.
- [ ] Delete the legacy classes.
- [ ] Run focused boost/config/condition tests and require success.

### Task 3: Verify clean cutover

**Files:**
- Modify only if verification exposes migration defects.

**Interfaces:**
- Consumes: migrated source tree.
- Produces: tested Paper shadow artifact with no obsolete imports or condition implementations.

- [ ] Search source and build files for obsolete `dev.conditions`, `dev.databag`, and legacy implementation references; require zero matches.
- [ ] Run `./gradlew :api:test :common:test :paper:test` and require success.
- [ ] Run `./gradlew :paper:build` and require success.
- [ ] Confirm `paper/build/libs/paper-all.jar` exists.
