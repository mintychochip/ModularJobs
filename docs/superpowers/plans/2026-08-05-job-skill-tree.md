# Job Skill Tree Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Historical note (2026-08-10):** Retained as an implementation record; its schema and setup assumptions are not current distribution guidance.

**Goal:** Refine ModularJobs' upgrade system into a generic JSON-driven skill-node graph with root/skill/major nodes, internal per-level costs, configurable requirement trees, explicit path locks, and derived effect evaluation.

**Architecture:** One `SkillTree` per base job replaces the current per-level-node `UpgradeTree`. Nodes carry `kind` (`root` / `skill` / `major`), per-level costs, a requirement condition tree, explicit symmetric `excludes`, and derived effects evaluated from persisted `node_levels`. Player state becomes persisted `total_skill_points` + `node_levels`; unlocked-set and state map become derived views. The legacy Wynncraft/JSON formats keep loading through migration adapters.

**Tech Stack:** Java 21, PaperMC API 1.21.11, Adventure components, Gson, SQLite/MySQL schema SQL, JUnit 5, MockBukkit, Gradle Kotlin DSL.

## Global Constraints

- Java 21 (no preview features).
- Compile against Paper API 1.21.11 (`libs.paper.api`); all Bukkit types are compileOnly.
- Follow existing composition-root pattern: all wiring in `PluginContext`, no DI framework.
- Node keys are plain job-unscoped strings; full keys are `Key.key(jobKey, nodeKey)`.
- `node_levels` is the single persisted source of truth; unlocked set / state map / perk levels are derived views, NEVER persisted.
- Skills may be `cumulative` (effects 1..level) or `replace` (effects(level) only) per node.
- `excludes` is explicit and symmetric; NEVER inferred from state writes or graph reachability.
- Skill points are the only node currency; costs are plain `int`.
- Requirements and effects use a bounded registered vocabulary; unknown `type` fails tree loading with a log message.
- Major nodes are one-time, require player confirmation, and are permanent during normal play.
- Leaving a job clears that job's tree state (levels, majors, state, points) and deletes persisted data; rejoining starts fresh.
- The pet-specialization `/upgrade` flow is a separate system and MUST NOT be touched.
- Tests run with `./gradlew :jobs-core:test`; only task-local test classes are run at each step.

---

## File Structure

- `jobs-api/src/main/java/net/aincraft/upgrade/SkillNode.java` — NEW: node record with kind, per-level data, requirement tree, excludes, effects, state writes.
- `jobs-api/src/main/java/net/aincraft/upgrade/SkillNodeKind.java` — NEW: `ROOT`, `SKILL`, `MAJOR` enum.
- `jobs-api/src/main/java/net/aincraft/upgrade/NodeLevel.java` — NEW: per-level `cost` + `effects` pair.
- `jobs-api/src/main/java/net/aincraft/upgrade/NodeStateWrite.java` — NEW: `set`/`remove` keyed state write.
- `jobs-api/src/main/java/net/aincraft/upgrade/Requirement.java` — NEW: sealed condition tree interface.
- `jobs-api/src/main/java/net/aincraft/upgrade/Requirements.java` — NEW: `all`/`any`/`not` + satisfied() composition.
- `jobs-api/src/main/java/net/aincraft/upgrade/SkillTree.java` — NEW: immutable per-job tree with graph traversal and derived children.
- `jobs-api/src/main/java/net/aincraft/upgrade/SkillTreeState.java` — NEW: immutable per-job player tree state. Persisted fields: `total_skill_points`, `node_levels`; the `state` map is a derived in-memory view (recomputed on load, never stored).
- `jobs-api/src/main/java/net/aincraft/upgrade/NodeEffect.java` — NEW: sealed effect interface with boost/ruled_boost/permission/recipe_unlock/state_set variants.
- `jobs-api/src/main/java/net/aincraft/upgrade/NodeEffectApplier.java` — NEW: derivable active-effect computation from state (no persistence).
- `jobs-api/src/main/java/net/aincraft/upgrade/UpgradeEffect.java` — MODIFY: stays for legacy parsing; new `NodeEffect` used by new engine.
- `jobs-api/src/main/java/net/aincraft/upgrade/UpgradeNode.java` — MODIFY: legacy record kept for migration only.
- `jobs-api/src/main/java/net/aincraft/upgrade/UpgradeTree.java` — MODIFY: legacy tree kept for migration only.
- `jobs-api/src/main/java/net/aincraft/upgrade/UpgradeService.java` — MODIFY: add `getSkillTreeState`/`purchaseSkillLevel`/`purchaseMajor`/`resetTree`/`clearTreeState` signatures.
- `jobs-api/src/main/java/net/aincraft/upgrade/PlayerUpgradeData.java` — MODIFY: add `nodeLevels()` / `state()` derived conveniences (kept column-compatible).
- `jobs-api/src/main/java/net/aincraft/upgrade/UpgradeBoostDataService.java` — MODIFY: keep `getBoostSources(UUID, Key)` as the state-driven public lookup and add `buildBoostSourcesForState(SkillTreeState, SkillTree)` in the implementation.
- `jobs-core/src/main/java/net/aincraft/upgrade/PlayerUpgradeDataImpl.java` — MODIFY: delegate persisted fields to `SkillTreeState`.
- `jobs-core/src/main/java/net/aincraft/upgrade/PlayerUpgradeRepository.java` — MODIFY: add `loadState`/`saveState`; JSON-encode `node_levels`; keep legacy column read migration.
- `jobs-core/src/main/java/net/aincraft/upgrade/config/UpgradeTreeLoader.java` — MODIFY: detect `version: 2` trees; delegate to new `SkillTreeConfigParser`; add `convertLegacy(UpgradeTree)` migration adapter; keep legacy path.
- `jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeConfigParser.java` — NEW: parses v2 JSON into `SkillTree` with registry-validated requirements/effects.
- `jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeRequirementParser.java` — NEW: JSON → sealed requirement tree.
- `jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeEffectParser.java` — NEW: JSON → sealed effect list.
- `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeServiceImpl.java` — MODIFY: route unlock/reset to new engine; add `purchase`/`purchaseMajor`/`reset`.
- `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeEffectApplier.java` — MODIFY: implement `NodeEffectApplier` with `syncEffects`/`derive`/`restoreAllForTrees`/`unapplyAll`.
- `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeBoostDataServiceImpl.java` — MODIFY: derive boost sources from `node_levels`.
- `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeLevelUpListener.java` — MODIFY: job leave clears upgrade data (calls `clearTreeState`).
- `jobs-core/src/main/java/net/aincraft/gui/UpgradeTreeGui.java` — MODIFY: render from `SkillTree`/`SkillTreeState`; add major confirmation flow in free control-row slots.
- `jobs-core/src/main/java/net/aincraft/commands/UpgradesCommand.java` — MODIFY: `reset` refunds skill levels but preserves majors/state; keep existing subcommand shape.
- `jobs-core/src/main/resources/sql/sqlite.sql` — MODIFY: add `node_levels` JSON column alongside `unlocked_nodes`.
- `jobs-core/src/main/resources/upgrade_trees/miner.json` — MODIFY: convert to v2 format (one skill node per perk, major companion branch).
- Tests (all under `jobs-core/src/test/java/net/aincraft/upgrade/` unless noted): `SkillNodeModelTest.java` (Task 1), `RequirementTreeTest.java` (Task 2), `SkillTreeTest.java` (Task 3), `config/SkillTreeConfigParserTest.java` (Task 4), `PlayerUpgradeRepositoryNewTest.java` (Task 5), `UpgradeServiceImplNewTest.java` (Task 6), `NodeEffectApplierTest.java` (Task 7), `UpgradeBoostDataServiceNewTest.java` (Task 8), `config/UpgradeTreeLoaderV2Test.java` (Task 9), `UpgradeLevelUpListenerLeaveTest.java` (Task 10), `../gui/UpgradeTreeGuiConfirmationTest.java` (Task 11), `config/LegacyToV2MigrationTest.java` (Task 12).

---

## Task 1: Sealed Node State Model (SkillNode / NodeLevel / SkillNodeKind / NodeStateWrite)

**Files:**
- Create: `jobs-api/src/main/java/net/aincraft/upgrade/SkillNodeKind.java`
- Create: `jobs-api/src/main/java/net/aincraft/upgrade/NodeLevel.java`
- Create: `jobs-api/src/main/java/net/aincraft/upgrade/NodeStateWrite.java`
- Create: `jobs-api/src/main/java/net/aincraft/upgrade/SkillNode.java`
- Test: `jobs-core/src/test/java/net/aincraft/upgrade/SkillNodeModelTest.java`

**Interfaces:**
- Consumes: nothing (new types).
- Produces:
  - `enum SkillNodeKind { ROOT, SKILL, MAJOR }`
  - `record NodeLevel(int cost, List<NodeEffect> effects)` — effects may be empty; `cost` ≥ 0.
  - `record NodeStateWrite(Op op, Key key, String value)` — `enum Op { SET, REMOVE }`.
  - `record SkillNode(Key key, String name, String description, Material lockedIcon, Material unlockedIcon, String lockedItemModel, String unlockedItemModel, SkillNodeKind kind, int cost, int maxLevel, LevelEffectMode mode, List<NodeLevel> levels, List<Requirement> requirements, Set<String> prerequisites, Set<String> excludes, List<NodeEffect> effects, Position position, List<Position> pathPoints, List<NodeStateWrite> stateWrites)` with:
    - `boolean isMajor()` → `kind == MAJOR`.
    - `boolean isSkill()` → `kind == SKILL`.
    - `int levelCost(int level)` → 1-indexed; returns 0 when out of range.
    - `List<NodeEffect> activeEffects(int level)` → for ROOT/MAJOR returns node effects when owned; for SKILL cumulative sums levels `1..level`, replace returns `levels[level-1].effects`.

**Steps:**

- [ ] **Step 1: Write the failing test**

Create `jobs-core/src/test/java/net/aincraft/upgrade/SkillNodeModelTest.java`:

```java
package net.aincraft.upgrade;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import net.aincraft.upgrade.SkillNode.LevelEffectMode;
import net.aincraft.upgrade.SkillNodeKind;
import net.aincraft.upgrade.NodeLevel;
import net.aincraft.upgrade.NodeEffect;
import net.aincraft.upgrade.SkillNode;
import net.aincraft.upgrade.SkillTreeState;
import net.aincraft.upgrade.Position;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * Proves the new node model carries levels, kind semantics, cumulative/replace
 * effect derivation, and precondition checks.
 */
class SkillNodeModelTest {

  private static SkillNode node(
      SkillNodeKind kind,
      int cost,
      List<NodeLevel> levels,
      LevelEffectMode mode,
      List<Requirement> requirements
  ) {
    return new SkillNode(
        Key.key("miner", "test"),
        "Test",
        "desc",
        Material.DIAMOND,
        Material.DIAMOND,
        null,
        null,
        kind,
        cost,
        levels.size(),
        mode,
        levels,
        requirements,
        Set.of(),
        Set.of(),
        List.of(),
        null,
        List.of(),
        List.of()
    );
  }

  @Test
  void kindSemantics() {
    SkillNode root = node(SkillNodeKind.ROOT, 0, List.of(), LevelEffectMode.REPLACE, List.of());
    SkillNode skill = node(SkillNodeKind.SKILL, 0, List.of(new NodeLevel(1, List.of())), LevelEffectMode.REPLACE, List.of());
    SkillNode major = node(SkillNodeKind.MAJOR, 5, List.of(), LevelEffectMode.REPLACE, List.of());

    assertTrue(root.isRoot());
    assertFalse(root.isSkill());
    assertFalse(root.isMajor());

    assertTrue(skill.isSkill());
    assertFalse(skill.isMajor());

    assertFalse(major.isSkill());
    assertTrue(major.isMajor());
  }

  @Test
  void levelCostOutOfRangeReturnsZero() {
    SkillNode skill = node(
        SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(1, List.of()), new NodeLevel(2, List.of())),
        LevelEffectMode.REPLACE,
        List.of()
    );
    assertEquals(1, skill.levelCost(1));
    assertEquals(2, skill.levelCost(2));
    assertEquals(0, skill.levelCost(0));
    assertEquals(0, skill.levelCost(3));
  }

  @Test
  void activeEffectsCumulativeVsReplace() {
    NodeEffect boost1 = NodeEffect.boost("xp", 1);
    NodeEffect boost2 = NodeEffect.boost("xp", 2);
    SkillNode cumulative = node(
        SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(1, List.of(boost1)), new NodeLevel(2, List.of(boost2))),
        LevelEffectMode.CUMULATIVE,
        List.of()
    );
    SkillNode replace = node(
        SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(1, List.of(boost1)), new NodeLevel(2, List.of(boost2))),
        LevelEffectMode.REPLACE,
        List.of()
    );

    // cumulative at level 2: level1 effect + level2 effect
    assertEquals(List.of(boost1, boost2), cumulative.activeEffects(2));
    // replace at level 2: only level2 effect
    assertEquals(List.of(boost2), replace.activeEffects(2));
    // at level 1 both behave the same
    assertEquals(List.of(boost1), cumulative.activeEffects(1));
    assertEquals(List.of(boost1), replace.activeEffects(1));
  }

  @Test
  void majorNeverHasLevels() {
    SkillNode major = node(SkillNodeKind.MAJOR, 5, List.of(), LevelEffectMode.REPLACE, List.of());
    assertEquals(List.of(), major.activeEffects(0));
    assertEquals(List.of(), major.activeEffects(1));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.SkillNodeModelTest`
Expected: FAIL with "cannot find symbol: class SkillNode".

- [ ] **Step 3: Write minimal implementation**

Create `jobs-api/src/main/java/net/aincraft/upgrade/SkillNodeKind.java`:

```java
package net.aincraft.upgrade;

/** Determines how a node behaves in the skill graph. */
public enum SkillNodeKind {
  /** Starting point of a tree; normally cost 0, no requirements. */
  ROOT,
  /** Repeatable purchase, one per level, with per-level costs/effects. */
  SKILL,
  /** One-time permanent choice requiring player confirmation. */
  MAJOR
}
```

Create `jobs-api/src/main/java/net/aincraft/upgrade/NodeLevel.java`:

```java
package net.aincraft.upgrade;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A single purchasable level of a skill node.
 *
 * @param cost    skill points required to buy this level
 * @param effects effects granted by reaching this level (see {@link SkillNode#activeEffects})
 */
public record NodeLevel(int cost, @NotNull List<NodeEffect> effects) {
  public NodeLevel {
    if (cost < 0) {
      throw new IllegalArgumentException("Node level cost must be non-negative");
    }
    effects = List.copyOf(effects);
  }
}
```

Create `jobs-api/src/main/java/net/aincraft/upgrade/NodeStateWrite.java`:

```java
package net.aincraft.upgrade;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * A keyed state write performed when a major node is purchased.
 * keys are namespaced (e.g. {@code tree.vocation}).
 *
 * @param op    SET writes {@code key = value}; REMOVE clears the key
 * @param key   namespaced state key
 * @param value value for SET; ignored for REMOVE
 */
public record NodeStateWrite(
    @NotNull Op op,
    @NotNull Key key,
    @NotNull String value
) {
  public enum Op { SET, REMOVE }
}
```

Create `jobs-api/src/main/java/net/aincraft/upgrade/NodeEffect.java`:

```java
package net.aincraft.upgrade;

import java.math.BigDecimal;
import java.util.List;
import net.aincraft.container.BoostSource;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Effect granted by a skill level or major node. Sealed vocabulary; unknown
 * types fail tree loading rather than silently no-op.
 */
public sealed interface NodeEffect permits
    NodeEffect.BoostEffect,
    NodeEffect.RuledBoostEffect,
    NodeEffect.PermissionEffect,
    NodeEffect.RecipeUnlockEffect,
    NodeEffect.StateSetEffect {

  /** Simple multiplier boost for a payable target. */
  record BoostEffect(@NotNull String target, @NotNull BigDecimal multiplier) implements NodeEffect {
    public static final String TARGET_XP = "xp";
    public static final String TARGET_MONEY = "money";
    public static final String TARGET_ALL = "all";

    public static BoostEffect of(String target, double multiplier) {
      return new BoostEffect(target, BigDecimal.valueOf(multiplier));
    }
  }

  /** Full BoostSource effect with rules/conditions, reusing the composition API. */
  record RuledBoostEffect(@NotNull String target, @NotNull BoostSource boostSource) implements NodeEffect {
  }

  /** One or more temporary permissions granted via PermissionAttachment. */
  record PermissionEffect(@NotNull List<String> permissions) implements NodeEffect {
    public PermissionEffect(String permission) {
      this(List.of(permission));
    }
  }

  /** Unlocks a namespaced crafting/smelting recipe. */
  record RecipeUnlockEffect(@NotNull Key recipeKey) implements NodeEffect {
  }

  /** Sets or removes a namespaced tree-state key. */
  record StateSetEffect(@NotNull Key key, @NotNull String value, boolean remove) implements NodeEffect {
  }

  static BoostEffect boost(String target, double multiplier) {
    return BoostEffect.of(target, multiplier);
  }
}
```

Create `jobs-api/src/main/java/net/aincraft/upgrade/SkillNode.java`:

```java
package net.aincraft.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A node in a job's skill graph. One model for all kinds: ROOT, SKILL, MAJOR.
 * Levelled skills carry per-level costs and effects; majors are one-time
 * permanent choices with optional state writes.
 */
public record SkillNode(
    @NotNull Key key,
    @NotNull String name,
    @Nullable String description,
    @NotNull Material lockedIcon,
    @NotNull Material unlockedIcon,
    @Nullable String lockedItemModel,
    @Nullable String unlockedItemModel,
    @NotNull SkillNodeKind kind,
    int cost,
    int maxLevel,
    @NotNull LevelEffectMode mode,
    @NotNull List<NodeLevel> levels,
    @NotNull List<Requirement> requirements,
    @NotNull Set<String> prerequisites,
    @NotNull Set<String> excludes,
    @NotNull List<NodeEffect> effects,
    @Nullable Position position,
    @NotNull List<Position> pathPoints,
    @NotNull List<NodeStateWrite> stateWrites
) implements Keyed {

  public enum LevelEffectMode {
    /** Active effects are effects of levels 1..current. */
    CUMULATIVE,
    /** Active effects are effects of the current level only. */
    REPLACE
  }

  public SkillNode {
    if (cost < 0 || maxLevel < 0) {
      throw new IllegalArgumentException("Skill node cost and maxLevel must be non-negative");
    }
    levels = List.copyOf(levels);
    requirements = List.copyOf(requirements);
    prerequisites = Set.copyOf(prerequisites);
    excludes = Set.copyOf(excludes);
    effects = List.copyOf(effects);
    pathPoints = List.copyOf(pathPoints);
    stateWrites = List.copyOf(stateWrites);
  }

  public boolean isRoot() {
    return kind == SkillNodeKind.ROOT;
  }

  public boolean isSkill() {
    return kind == SkillNodeKind.SKILL;
  }

  public boolean isMajor() {
    return kind == SkillNodeKind.MAJOR;
  }

  /** Cost to buy the given level (1-indexed); 0 when out of range. */
  public int levelCost(int level) {
    if (level < 1 || level > levels.size()) {
      return 0;
    }
    return levels.get(level - 1).cost();
  }

  /** Effects active at the given owned level, per {@link #mode}. */
  public @NotNull List<NodeEffect> activeEffects(int level) {
    if (level <= 0) {
      return List.of();
    }
    if (!isSkill()) {
      return effects;
    }
    int capped = Math.min(level, levels.size());
    if (mode == LevelEffectMode.CUMULATIVE) {
      List<NodeEffect> result = new ArrayList<>();
      for (int i = 1; i <= capped; i++) {
        result.addAll(levels.get(i - 1).effects());
      }
      return List.copyOf(result);
    }
    return levels.get(capped - 1).effects();
  }

  /**
   * Whether all configured requirements are satisfied for the given player state.
   */
  public boolean preconditionSatisfied(@NotNull SkillTreeState state) {
    for (Requirement requirement : requirements) {
      if (!requirement.satisfied(state)) {
        return false;
      }
    }
    return true;
  }
}
```

Also create the requirement contract used by `SkillNode` (Task 2 supplies its
registered implementations):

`jobs-api/src/main/java/net/aincraft/upgrade/Requirement.java`:

```java
package net.aincraft.upgrade;

import org.jetbrains.annotations.NotNull;

/**
 * A declarative condition in a skill tree. Bounded vocabulary; implementations
 * are registered and parsed from JSON.
 *
 * Implementations are provided by the sealed requirement vocabulary in Task 2.
public interface Requirement {
  boolean satisfied(@NotNull SkillTreeState state);
}
```

`jobs-api/src/main/java/net/aincraft/upgrade/SkillTreeState.java`:

```java
package net.aincraft.upgrade;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable snapshot of a player's progress in one job's skill tree.
 * Only {@link #totalSkillPoints()} and {@link #nodeLevels()} are persisted;
 * the state map is a derived view. Job-level and permission suppliers are
 * runtime evaluation context and are never persisted.
 */
public record SkillTreeState(
    @NotNull String playerId,
    @NotNull String jobKey,
    int totalSkillPoints,
    @NotNull Map<String, Integer> nodeLevels,
    @NotNull Map<Key, String> state,
    @NotNull IntSupplier currentJobLevel,
    @NotNull Predicate<String> permissionCheck
) {
  public SkillTreeState {
    nodeLevels = Collections.unmodifiableMap(new HashMap<>(nodeLevels));
    state = Collections.unmodifiableMap(new HashMap<>(state));
  }

  public SkillTreeState(
      String playerId,
      String jobKey,
      int totalSkillPoints,
      Map<String, Integer> nodeLevels,
      Map<Key, String> state) {
    this(playerId, jobKey, totalSkillPoints, nodeLevels, state, () -> 0, permission -> false);
  }

  public int levelOf(@NotNull String nodeKey) {
    return nodeLevels.getOrDefault(nodeKey, 0);
  }

  public boolean hasUnlocked(@NotNull String nodeKey) {
    return levelOf(nodeKey) > 0;
  }

  public int jobLevel() {
    return currentJobLevel.getAsInt();
  }

  public boolean hasPermission(@NotNull String permission) {
    return permissionCheck.test(permission);
  }

  /** Spent points = sum of purchased level costs plus major costs. Needs the tree. */
  public static SkillTreeState empty(@NotNull String playerId, @NotNull String jobKey) {
    return new SkillTreeState(playerId, jobKey, 0, Map.of(), Map.of());
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.SkillNodeModelTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add jobs-api/src/main/java/net/aincraft/upgrade/SkillNodeKind.java \
        jobs-api/src/main/java/net/aincraft/upgrade/NodeLevel.java \
        jobs-api/src/main/java/net/aincraft/upgrade/NodeStateWrite.java \
        jobs-api/src/main/java/net/aincraft/upgrade/NodeEffect.java \
        jobs-api/src/main/java/net/aincraft/upgrade/SkillNode.java \
        jobs-api/src/main/java/net/aincraft/upgrade/Requirement.java \
        jobs-api/src/main/java/net/aincraft/upgrade/SkillTreeState.java \
        jobs-core/src/test/java/net/aincraft/upgrade/SkillNodeModelTest.java
git commit -m "feat(api): add levelled skill node model with per-level costs"
```

---

## Task 2: Requirement Tree (all/any/not + leaves)

**Files:**
- Create: `jobs-api/src/main/java/net/aincraft/upgrade/Requirement.java` (the stable contract used by Task 2)
- Create: `jobs-api/src/main/java/net/aincraft/upgrade/Requirements.java`
- Test: `jobs-core/src/test/java/net/aincraft/upgrade/RequirementTreeTest.java`

**Interfaces:**
- Consumes: `SkillTreeState` (Task 1), `Requirement` (Task 1).
- Produces:
  - `sealed interface Requirement permits AllOf, AnyOf, Not, JobLevelRequirement, NodeLevelRequirement, NodeUnlockedRequirement, StateEqualsRequirement, PermissionRequirement { boolean satisfied(SkillTreeState state); }`
  - `record AllOf(List<Requirement> requirements)` — true when all satisfied.
  - `record AnyOf(List<Requirement> requirements)` — true when any satisfied.
  - `record Not(Requirement requirement)` — true when inner false.
  - `record JobLevelRequirement(int minimumJobLevel)` — satisfied via `state.jobLevel()` using the runtime current-job-level supplier.
  - `record NodeLevelRequirement(String nodeKey, int minimum)` — `state.levelOf(nodeKey) >= minimum`.
  - `record NodeUnlockedRequirement(String nodeKey)` — `state.hasUnlocked(nodeKey)`.
  - `record StateEqualsRequirement(Key key, String value)` — `state.state().get(key).equals(value)`.
  - `record PermissionRequirement(String key)` — satisfied via `state.hasPermission(key)`.

**Steps:**

- [ ] **Step 1: Enhance SkillTreeState with host-dependent leaves (job level / permission)**

Modify `SkillTreeState` to add two functional hooks so the requirement tree stays declarative:

```java
public record SkillTreeState(
    @NotNull String playerId,
    @NotNull String jobKey,
    int totalSkillPoints,
    @NotNull Map<String, Integer> nodeLevels,
    @NotNull Map<Key, String> state,
    @NotNull IntSupplier currentJobLevel,
    @NotNull Predicate<String> permissionCheck
) {
  // existing canonical ctor becomes:
  public SkillTreeState(
      String playerId, String jobKey, int totalSkillPoints,
      Map<String, Integer> nodeLevels, Map<Key, String> state) {
    this(playerId, jobKey, totalSkillPoints, nodeLevels, state, () -> 0, k -> false);
  }

  public int jobLevel() {
    return currentJobLevel.getAsInt();
  }

  public boolean hasPermission(String key) {
    return permissionCheck.test(key);
  }
  // ... keep levelOf/hasUnlocked
}
```

Tests construct the full ctor with `() -> 5` and `k -> true` to prove the leaves evaluate.

- [ ] **Step 2: Write the failing test**

Create `jobs-core/src/test/java/net/aincraft/upgrade/RequirementTreeTest.java`:

```java
package net.aincraft.upgrade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import net.aincraft.upgrade.Requirements.AllOf;
import net.aincraft.upgrade.Requirements.AnyOf;
import net.aincraft.upgrade.Requirements.Not;
import net.aincraft.upgrade.Requirements.JobLevelRequirement;
import net.aincraft.upgrade.Requirements.NodeLevelRequirement;
import net.aincraft.upgrade.Requirements.NodeUnlockedRequirement;
import net.aincraft.upgrade.Requirements.PermissionRequirement;
import net.aincraft.upgrade.Requirements.StateEqualsRequirement;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class RequirementTreeTest {

  private SkillTreeState state() {
    return new SkillTreeState(
        "p1", "miner", 10,
        Map.of("efficiency", 2, "blasting", 1),
        Map.of(Key.key("tree", "vocation"), "weaponsmith"),
        () -> 25,
        key -> key.equals("jobs.special_access")
    );
  }

  @Test
  void nodeLevelLeaf() {
    assertTrue(new NodeLevelRequirement("efficiency", 2).satisfied(state()));
    assertTrue(new NodeLevelRequirement("efficiency", 1).satisfied(state()));
    assertFalse(new NodeLevelRequirement("efficiency", 3).satisfied(state()));
  }

  @Test
  void nodeUnlockedLeaf() {
    assertTrue(new NodeUnlockedRequirement("blasting").satisfied(state()));
    assertFalse(new NodeUnlockedRequirement("deep_mine").satisfied(state()));
  }

  @Test
  void jobLevelLeaf() {
    assertTrue(new JobLevelRequirement(25).satisfied(state()));
    assertFalse(new JobLevelRequirement(30).satisfied(state()));
  }

  @Test
  void stateEqualsLeaf() {
    assertTrue(new StateEqualsRequirement(Key.key("tree", "vocation"), "weaponsmith").satisfied(state()));
    assertFalse(new StateEqualsRequirement(Key.key("tree", "vocation"), "toolsmith").satisfied(state()));
  }

  @Test
  void permissionLeaf() {
    assertTrue(new PermissionRequirement("jobs.special_access").satisfied(state()));
    assertFalse(new PermissionRequirement("jobs.nope").satisfied(state()));
  }

  @Test
  void allOfRequiresEveryChild() {
    Requirement all = new AllOf(List.of(
        new NodeLevelRequirement("efficiency", 1),
        new NodeLevelRequirement("blasting", 1)
    ));
    assertTrue(all.satisfied(state()));

    Requirement allFail = new AllOf(List.of(
        new NodeLevelRequirement("efficiency", 1),
        new NodeLevelRequirement("deep_mine", 1)
    ));
    assertFalse(allFail.satisfied(state()));
  }

  @Test
  void anyOfRequiresOneChild() {
    Requirement any = new AnyOf(List.of(
        new JobLevelRequirement(30),
        new NodeLevelRequirement("efficiency", 2)
    ));
    assertTrue(any.satisfied(state()));

    Requirement anyFail = new AnyOf(List.of(
        new JobLevelRequirement(30),
        new NodeLevelRequirement("deep_mine", 1)
    ));
    assertFalse(anyFail.satisfied(state()));
  }

  @Test
  void notInverts() {
    assertTrue(new Not(new NodeUnlockedRequirement("deep_mine")).satisfied(state()));
    assertFalse(new Not(new NodeUnlockedRequirement("efficiency")).satisfied(state()));
  }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.RequirementTreeTest`
Expected: FAIL — `Requirements` missing.

- [ ] **Step 4: Implement**

Replace the Task 1 provisional `Requirement.java` contract with:

```java
package net.aincraft.upgrade;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** A declarative condition in a skill tree. */
public sealed interface Requirement permits
    Requirements.AllOf,
    Requirements.AnyOf,
    Requirements.Not,
    Requirements.JobLevelRequirement,
    Requirements.NodeLevelRequirement,
    Requirements.NodeUnlockedRequirement,
    Requirements.StateEqualsRequirement,
    Requirements.PermissionRequirement {

  boolean satisfied(@NotNull SkillTreeState state);
}
```

Create `jobs-api/src/main/java/net/aincraft/upgrade/Requirements.java`:

```java
package net.aincraft.upgrade;

import java.util.List;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/** Concrete requirement variants: logical combinators and typed leaves. */
public final class Requirements {

  private Requirements() {}

  /** All children must be satisfied. */
  public record AllOf(@NotNull List<Requirement> requirements) implements Requirement {
    public AllOf {
      requirements = List.copyOf(requirements);
    }

    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return requirements.stream().allMatch(r -> r.satisfied(state));
    }
  }

  /** At least one child must be satisfied. */
  public record AnyOf(@NotNull List<Requirement> requirements) implements Requirement {
    public AnyOf {
      requirements = List.copyOf(requirements);
    }

    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return requirements.stream().anyMatch(r -> r.satisfied(state));
    }
  }

  /** Inverts the inner requirement. */
  public record Not(@NotNull Requirement requirement) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return !requirement.satisfied(state);
    }
  }

  /** Player's current level in this job must be at least {@code minimumJobLevel}. */
  public record JobLevelRequirement(int minimumJobLevel) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return state.jobLevel() >= minimumJobLevel;
    }
  }

  /** The named node must be owned at least at {@code minimum} level. */
  public record NodeLevelRequirement(@NotNull String nodeKey, int minimum) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return state.levelOf(nodeKey) >= minimum;
    }
  }

  /** The named node must be unlocked (level >= 1). */
  public record NodeUnlockedRequirement(@NotNull String nodeKey) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return state.hasUnlocked(nodeKey);
    }
  }

  /** A namespaced state key must equal {@code value}. */
  public record StateEqualsRequirement(@NotNull Key key, @NotNull String value) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return value.equals(state.state().get(key));
    }
  }

  /** The player must have the given Bukkit permission. */
  public record PermissionRequirement(@NotNull String key) implements Requirement {
    @Override
    public boolean satisfied(@NotNull SkillTreeState state) {
      return state.hasPermission(key);
    }
  }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.RequirementTreeTest`
Expected: PASS (8 tests).

- [ ] **Step 6: Commit**

```bash
git add jobs-api/src/main/java/net/aincraft/upgrade/Requirement.java \
        jobs-api/src/main/java/net/aincraft/upgrade/Requirements.java \
        jobs-api/src/main/java/net/aincraft/upgrade/SkillTreeState.java \
        jobs-core/src/test/java/net/aincraft/upgrade/RequirementTreeTest.java
git commit -m "feat(api): add declarative requirement tree with all/any/not"
```

---

## Task 3: SkillTree graph model (traversal + derived children + excludes normalization)

**Files:**
- Create: `jobs-api/src/main/java/net/aincraft/upgrade/SkillTree.java`
- Test: `jobs-core/src/test/java/net/aincraft/upgrade/SkillTreeTest.java`

**Interfaces:**
- Consumes: `SkillNode`, `SkillTreeState`, `Requirement`, `Requirements` (Tasks 1–2).
- Produces:
  - `record SkillTree(Key key, String jobKey, String description, int skillPointsPerLevel, String rootNodeKey, Map<String, SkillNode> nodes)` with:
    - `Optional<SkillNode> node(String key)`
    - `Collection<SkillNode> nodes()`
    - `Collection<SkillNode> children(SkillNode node)` — derived by scanning `nodes` for `prerequisites.contains(nodeKey)`.
    - `Set<String> symmetricExcludes(String nodeKey)` — normalized from each node's `excludes` (both directions).
    - `int spentPoints(SkillTreeState state)` — sums `node.levelCost(level)` over `state.nodeLevels()` + major `cost` for majors owned at level 1.
    - `int availablePoints(SkillTreeState state)` — `state.totalSkillPoints() - spentPoints(state)`.
    - `Set<SkillNode> availableNodes(SkillTreeState state)` — nodes with all requirements satisfied, not excluded, not owned, cost ≤ available.
    - `boolean canPurchase(SkillTreeState state, String nodeKey)` — full gate check for a level or major.

**Steps:**

- [ ] **Step 1: Write the failing test**

Create `jobs-core/src/test/java/net/aincraft/upgrade/SkillTreeTest.java`:

```java
package net.aincraft.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.aincraft.upgrade.Requirements.NodeLevelRequirement;
import net.aincraft.upgrade.SkillNode.LevelEffectMode;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SkillTreeTest {

  private static SkillNode node(String jobKey, String nodeKey, SkillNodeKind kind,
                                int cost, List<NodeLevel> levels, List<Requirement> requirements,
                                Set<String> prerequisites, Set<String> excludes) {
    return new SkillNode(
        Key.key(jobKey, nodeKey), nodeKey, null,
        Material.DIAMOND, Material.DIAMOND, null, null,
        kind, cost, levels.size(), LevelEffectMode.REPLACE, levels, requirements,
        prerequisites, excludes, List.of(), null, List.of(), List.of());
  }

  private static SkillTree minerTree() {
    SkillNode root = node("miner", "root", SkillNodeKind.ROOT, 0, List.of(), List.of(), Set.of(), Set.of());
    SkillNode efficiency = node("miner", "efficiency", SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(1, List.of()), new NodeLevel(2, List.of())),
        List.of(), Set.of("root"), Set.of());
    SkillNode blasting = node("miner", "blasting", SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(3, List.of())),
        List.of(), Set.of("efficiency"), Set.of("deep_mine"));
    SkillNode deepMine = node("miner", "deep_mine", SkillNodeKind.SKILL, 0,
        List.of(new NodeLevel(3, List.of())),
        List.of(), Set.of("root"), Set.of("blasting"));
    SkillNode major = node("miner", "master_smith", SkillNodeKind.MAJOR, 5, List.of(), List.of(), Set.of("efficiency"), Set.of());

    return new SkillTree(
        Key.key("modularjobs", "upgrade_tree/miner"),
        "miner", null, 1, "root",
        Map.of("root", root, "efficiency", efficiency, "blasting", blasting,
            "deep_mine", deepMine, "master_smith", major));
  }

  @Test
  void childrenAreDerivedFromPrerequisites() {
    SkillTree tree = minerTree();
    assertEquals(
        Set.of("efficiency", "deep_mine"),
        tree.children(tree.node("root").orElseThrow()).stream()
            .map(n -> n.key().value()).collect(java.util.stream.Collectors.toSet()));
    assertEquals(
        Set.of("blasting", "master_smith"),
        tree.children(tree.node("efficiency").orElseThrow()).stream()
            .map(n -> n.key().value()).collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void spentPointsSumsLevelsAndMajors() {
    SkillTree tree = minerTree();
    SkillTreeState state = new SkillTreeState(
        "p1", "miner", 15,
        Map.of("efficiency", 2, "blasting", 1, "master_smith", 1),
        Map.of(), () -> 5, k -> true);
    // efficiency level1 (1) + level2 (2) + blasting (3) + major (5) = 11
    assertEquals(11, tree.spentPoints(state));
    assertEquals(4, tree.availablePoints(state));
  }

  @Test
  void symmetricExcludesNormalized() {
    SkillTree tree = minerTree();
    assertTrue(tree.symmetricExcludes("blasting").contains("deep_mine"));
    assertTrue(tree.symmetricExcludes("deep_mine").contains("blasting"));
  }

  @Test
  void canPurchaseGatesOnRequirementsCostAndExcludes() {
    SkillTree tree = minerTree();
    SkillTreeState rich = new SkillTreeState(
        "p1", "miner", 20,
        Map.of("root", 1, "efficiency", 2),
        Map.of(), () -> 5, k -> true);

    // blasting: prereq efficiency owned, cost 3 <= available 17, not excluded
    assertTrue(tree.canPurchase(rich, "blasting"));
    // deep_mine is purchasable while blasting is not owned.
    assertTrue(tree.canPurchase(rich, "deep_mine"));

    SkillTreeState excluded = new SkillTreeState(
        "p1", "miner", 20,
        Map.of("root", 1, "efficiency", 2, "blasting", 1),
        Map.of(), () -> 5, k -> true);
    assertFalse(tree.canPurchase(excluded, "deep_mine"));
  }

  @Test
  void requirementsGateMajorPurchase() {
    SkillTree tree = minerTree();
    SkillNode master = tree.node("master_smith").orElseThrow();
    SkillNode withRequirement = new SkillNode(
        master.key(), master.name(), master.description(),
        master.lockedIcon(), master.unlockedIcon(),
        master.lockedItemModel(), master.unlockedItemModel(),
        master.kind(), master.cost(), master.maxLevel(), master.mode(),
        master.levels(),
        List.of(new NodeLevelRequirement("efficiency", 2)),
        master.prerequisites(), master.excludes(), master.effects(), master.position(),
        master.pathPoints(), master.stateWrites());

    SkillTree tree2 = new SkillTree(tree.key(), tree.jobKey(), tree.description(),
        tree.skillPointsPerLevel(), tree.rootNodeKey(),
        Map.of("root", tree.node("root").orElseThrow(),
            "efficiency", tree.node("efficiency").orElseThrow(),
            "master_smith", withRequirement));

    SkillTreeState lowEfficiency = new SkillTreeState(
        "p1", "miner", 20, Map.of("root", 1, "efficiency", 1),
        Map.of(), () -> 5, k -> true);
    assertFalse(tree2.canPurchase(lowEfficiency, "master_smith"));

    SkillTreeState highEfficiency = new SkillTreeState(
        "p1", "miner", 20, Map.of("root", 1, "efficiency", 2),
        Map.of(), () -> 5, k -> true);
    assertTrue(tree2.canPurchase(highEfficiency, "master_smith"));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.SkillTreeTest`
Expected: FAIL — `SkillTree` missing.

- [ ] **Step 3: Implement `SkillTree`**

Create `jobs-api/src/main/java/net/aincraft/upgrade/SkillTree.java`:

```java
package net.aincraft.upgrade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable skill graph for one base job. Children are derived from each
 * node's prerequisites; excludes are normalized to a symmetric conflict set.
 */
public record SkillTree(
    @NotNull Key key,
    @NotNull String jobKey,
    String description,
    int skillPointsPerLevel,
    @NotNull String rootNodeKey,
    @NotNull Map<String, SkillNode> nodes
) implements Keyed {

  public SkillTree {
    nodes = Collections.unmodifiableMap(new HashMap<>(nodes));
  }

  public @NotNull Optional<SkillNode> node(@NotNull String nodeKey) {
    return Optional.ofNullable(nodes.get(nodeKey));
  }

  public @NotNull Collection<SkillNode> nodes() {
    return nodes.values();
  }

  /** Children derived from prerequisites: nodes whose prerequisites include this node's key. */
  public @NotNull Collection<SkillNode> children(@NotNull SkillNode node) {
    String nodeKey = node.key().value();
    return nodes.values().stream()
        .filter(n -> n.prerequisites().contains(nodeKey))
        .collect(Collectors.toUnmodifiableList());
  }

  /** Node keys that conflict with {@code nodeKey} (both directions). */
  public @NotNull Set<String> symmetricExcludes(@NotNull String nodeKey) {
    Set<String> result = new HashSet<>();
    for (SkillNode node : nodes.values()) {
      String otherKey = node.key().value();
      if (node.excludes().contains(nodeKey)) {
        result.add(otherKey);
      }
      if (otherKey.equals(nodeKey)) {
        result.addAll(node.excludes());
      }
    }
    return Set.copyOf(result);
  }

  /** Points already spent: per-level costs plus one-time major costs. */
  public int spentPoints(@NotNull SkillTreeState state) {
    int spent = 0;
    for (Map.Entry<String, Integer> entry : state.nodeLevels().entrySet()) {
      SkillNode node = nodes.get(entry.getKey());
      if (node == null) {
        continue;
      }
      int owned = entry.getValue();
      if (node.isSkill()) {
        for (int level = 1; level <= owned; level++) {
          spent += node.levelCost(level);
        }
      } else if (node.isMajor() && owned >= 1) {
        spent += node.cost();
      }
    }
    return spent;
  }

  public int availablePoints(@NotNull SkillTreeState state) {
    return state.totalSkillPoints() - spentPoints(state);
  }

  /** Nodes a player can currently purchase (next skill level or whole major). */
  public @NotNull Set<SkillNode> availableNodes(@NotNull SkillTreeState state) {
    Set<SkillNode> result = new HashSet<>();
    for (SkillNode node : nodes.values()) {
      if (canPurchase(state, node.key().value())) {
        result.add(node);
      }
    }
    return Set.copyOf(result);
  }

  /** Full purchase gate: requirements, prereqs, excludes, ownership, and cost. */
  public boolean canPurchase(@NotNull SkillTreeState state, @NotNull String nodeKey) {
    SkillNode node = nodes.get(nodeKey);
    if (node == null) {
      return false;
    }

    // Requirements (configurable condition tree)
    if (!node.preconditionSatisfied(state)) {
      return false;
    }

    // Prerequisites owned
    for (String prereq : node.prerequisites()) {
      if (!state.hasUnlocked(prereq)) {
        return false;
      }
    }

    // Not excluded by an owned node
    for (String excluded : node.excludes()) {
      if (state.hasUnlocked(excluded)) {
        return false;
      }
    }
    for (Map.Entry<String, Integer> owned : state.nodeLevels().entrySet()) {
      if (owned.getValue() > 0 && symmetricExcludes(owned.getKey()).contains(nodeKey)) {
        return false;
      }
    }

    // Ownership level gating
    int owned = state.levelOf(nodeKey);
    if (node.isSkill()) {
      if (owned >= node.maxLevel()) {
        return false;
      }
    } else if (owned >= 1) {
      return false; // already owns this major or root
    }

    // Cost gate
    int cost = node.isSkill() ? node.levelCost(owned + 1) : node.cost();
    return cost <= availablePoints(state);
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.SkillTreeTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add jobs-api/src/main/java/net/aincraft/upgrade/SkillTree.java \
        jobs-core/src/test/java/net/aincraft/upgrade/SkillTreeTest.java
git commit -m "feat(api): add skill tree graph with derived children and purchase gating"
```

---

## Task 4: SkillTreeConfigParser (v2 JSON → SkillTree)

**Files:**
- Create: `jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeRequirementParser.java`
- Create: `jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeEffectParser.java`
- Create: `jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeConfigParser.java`
- Test: `jobs-core/src/test/java/net/aincraft/upgrade/config/SkillTreeConfigParserTest.java`

**Interfaces:**
- Consumes: `SkillNode`, `SkillTree`, `Requirement`/`Requirements`, `NodeEffect` (API tasks); `BoostFactory`, `ConditionFactory` for `ruled_boost`.
- Produces:
  - `final class SkillTreeRequirementParser { Requirement parse(JsonElement el); }` — throws `IllegalArgumentException` on unknown `type`.
  - `final class SkillTreeEffectParser { NodeEffect parse(JsonElement el); }` — same.
  - `final class SkillTreeConfigParser { SkillTree parse(JsonObject root); }` — validates: version==2, `job`, `root`, and `nodes` exist, every prerequisite/exclude key resolves, state writes belong only to majors, and unknown requirement/effect types throw.

**JSON v2 shape (driven by spec sections 4, 5, 9):**

```json
{
  "version": 2,
  "job": "miner",
  "skill_points_per_level": 1,
  "root": "mining_basics",
  "nodes": {
    "mining_basics": { "kind": "root", "name": "Mining Basics" },
    "efficiency": {
      "kind": "skill",
      "name": "Efficiency",
      "prerequisites": ["mining_basics"],
      "level_effect_mode": "replace",
      "levels": [
        { "cost": 1, "effects": [ { "type": "boost", "target": "xp", "amount": 1.1 } ] },
        { "cost": 2, "effects": [ { "type": "boost", "target": "xp", "amount": 1.2 } ] },
        { "cost": 4, "effects": [ { "type": "boost", "target": "xp", "amount": 1.35 } ] }
      ]
    },
    "weaponsmith": {
      "kind": "major",
      "name": "Weaponsmith",
      "prerequisites": ["efficiency"],
      "cost": 5,
      "excludes": ["toolsmith"],
      "requirements": {
        "all": [ { "type": "node_level", "node": "efficiency", "minimum": 3 } ]
      },
      "state": [ { "set": { "tree.vocation": "weaponsmith" } } ]
    }
  }
}
```

**Steps:**

- [ ] **Step 1: Write the failing test**

Create `jobs-core/src/test/java/net/aincraft/upgrade/config/SkillTreeConfigParserTest.java`:

```java
package net.aincraft.upgrade.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.aincraft.boost.BoostFactoryImpl;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.upgrade.NodeEffect;
import net.aincraft.upgrade.Requirements.AllOf;
import net.aincraft.upgrade.Requirements.NodeLevelRequirement;
import net.aincraft.upgrade.SkillNode;
import net.aincraft.upgrade.SkillNodeKind;
import net.aincraft.upgrade.SkillTree;
import org.junit.jupiter.api.Test;

class SkillTreeConfigParserTest {

  private SkillTreeConfigParser parser() {
    BoostFactory boostFactory = BoostFactoryImpl.INSTANCE;
    ConditionFactory conditionFactory = BoostFactoryImpl.INSTANCE;
    return new SkillTreeConfigParser(boostFactory, conditionFactory);
  }

  private static final String TREE_JSON = """
      {
        "version": 2,
        "job": "miner",
        "skill_points_per_level": 1,
        "root": "mining_basics",
        "nodes": {
          "mining_basics": {
            "kind": "root",
            "name": "Mining Basics"
          },
          "efficiency": {
            "kind": "skill",
            "name": "Efficiency",
            "prerequisites": ["mining_basics"],
            "level_effect_mode": "replace",
            "levels": [
              { "cost": 1, "effects": [ { "type": "boost", "target": "xp", "amount": 1.1 } ] },
              { "cost": 2, "effects": [ { "type": "boost", "target": "xp", "amount": 1.2 } ] }
            ]
          },
          "weaponsmith": {
            "kind": "major",
            "name": "Weaponsmith",
            "prerequisites": ["efficiency"],
            "cost": 5,
            "excludes": ["toolsmith"],
            "requirements": {
              "all": [ { "type": "node_level", "node": "efficiency", "minimum": 2 } ]
            },
            "state": [ { "set": { "tree.vocation": "weaponsmith" } } ]
          },
          "toolsmith": {
            "kind": "major",
            "name": "Toolsmith",
            "prerequisites": ["efficiency"],
            "cost": 5,
            "excludes": ["weaponsmith"],
            "requirements": {
              "all": [ { "type": "node_level", "node": "efficiency", "minimum": 2 } ]
            },
            "state": [ { "set": { "tree.vocation": "toolsmith" } } ]
          }
        }
      }
      """;

  @Test
  void parsesV2TreeWithSkillLevelsMajorStateAndRequirements() {
    SkillTree tree = parser().parse(JsonParser.parseString(TREE_JSON).getAsJsonObject());

    assertEquals("miner", tree.jobKey());
    assertEquals("mining_basics", tree.rootNodeKey());
    assertEquals(4, tree.nodes().size());

    SkillNode efficiency = tree.node("efficiency").orElseThrow();
    assertEquals(SkillNodeKind.SKILL, efficiency.kind());
    assertEquals(2, efficiency.levels().size());
    assertEquals(1, efficiency.levelCost(1));
    assertEquals(2, efficiency.levelCost(2));
    NodeEffect effect = efficiency.activeEffects(1).get(0);
    assertTrue(effect instanceof NodeEffect.BoostEffect);

    SkillNode weaponsmith = tree.node("weaponsmith").orElseThrow();
    assertEquals(SkillNodeKind.MAJOR, weaponsmith.kind());
    assertEquals(5, weaponsmith.cost());
    assertTrue(weaponsmith.preconditionSatisfied(
        new net.aincraft.upgrade.SkillTreeState("p", "miner", 10,
            java.util.Map.of("mining_basics", 1, "efficiency", 2),
            java.util.Map.of(), () -> 5, k -> true)));
    assertEquals(1, weaponsmith.stateWrites().size());
  }

  @Test
  void rejectsUnknownRequirementType() {
    String bad = TREE_JSON.replace("node_level", "not_a_real_type");
    assertThrows(IllegalArgumentException.class, () ->
        parser().parse(JsonParser.parseString(bad).getAsJsonObject()));
  }

  @Test
  void rejectsUnknownEffectType() {
    String bad = TREE_JSON.replace("\"type\": \"boost\"", "\"type\": \"nope\"");
    assertThrows(IllegalArgumentException.class, () ->
        parser().parse(JsonParser.parseString(bad).getAsJsonObject()));
  }

  @Test
  void rejectsDanglingPrerequisite() {
    String bad = TREE_JSON.replace("\"prerequisites\": [\"mining_basics\"]", "\"prerequisites\": [\"missing_node\"]");
    assertThrows(IllegalArgumentException.class, () ->
        parser().parse(JsonParser.parseString(bad).getAsJsonObject()));
  }

  @Test
  void rejectsNonVersionTwoConfig() {
    String bad = TREE_JSON.replace("\"version\": 2", "\"version\": 1");
    assertThrows(IllegalArgumentException.class, () ->
        parser().parse(JsonParser.parseString(bad).getAsJsonObject()));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.config.SkillTreeConfigParserTest`
Expected: FAIL — classes missing.

- [ ] **Step 3: Implement the requirement parser**

Create `jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeRequirementParser.java`:

```java
package net.aincraft.upgrade.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.upgrade.Requirement;
import net.aincraft.upgrade.Requirements;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Parses the JSON requirement vocabulary into the sealed Requirement tree.
 * Unknown types throw so misconfigurations are loud.
 */
public final class SkillTreeRequirementParser {

  @NotNull
  public Requirement parse(@NotNull JsonElement element) {
    JsonObject obj = element.getAsJsonObject();
    String type = obj.has("type") ? obj.get("type").getAsString()
        : obj.has("all") ? "all"
        : obj.has("any") ? "any"
        : obj.has("not") ? "not"
        : "";

    return switch (type) {
      case "all" -> new Requirements.AllOf(parseList(
          obj.has("requirements") ? obj.getAsJsonArray("requirements") : obj.getAsJsonArray("all")));
      case "any" -> new Requirements.AnyOf(parseList(
          obj.has("requirements") ? obj.getAsJsonArray("requirements") : obj.getAsJsonArray("any")));
      case "not" -> new Requirements.Not(parse(
          obj.has("requirement") ? obj.get("requirement") : obj.get("not")));
      case "job_level" -> new Requirements.JobLevelRequirement(obj.get("minimum").getAsInt());
      case "node_level" -> new Requirements.NodeLevelRequirement(
          obj.get("node").getAsString(), obj.get("minimum").getAsInt());
      case "node_unlocked" -> new Requirements.NodeUnlockedRequirement(
          obj.get("node").getAsString());
      case "state_equals" -> new Requirements.StateEqualsRequirement(
          parseKey(obj.get("key").getAsString()), obj.get("value").getAsString());
      case "permission" -> new Requirements.PermissionRequirement(obj.get("key").getAsString());
      default -> throw new IllegalArgumentException("Unknown requirement type: " + type);
    };
  }

  private List<Requirement> parseList(JsonArray array) {
    List<Requirement> result = new ArrayList<>();
    if (array != null) {
      for (JsonElement el : array) {
        result.add(parse(el));
      }
    }
    return List.copyOf(result);
  }

  private Key parseKey(String raw) {
    int separator = raw.indexOf(':');
    if (separator > 0) {
      return Key.key(raw.substring(0, separator), raw.substring(separator + 1));
    }
    int dot = raw.indexOf('.');
    if (dot > 0) {
      return Key.key(raw.substring(0, dot), raw.substring(dot + 1));
    }
    throw new IllegalArgumentException("Namespaced key must use namespace.key or namespace:key: " + raw);
  }

}
```

- [ ] **Step 4: Implement the effect parser**

Create `jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeEffectParser.java`:

```java
package net.aincraft.upgrade.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import net.aincraft.boost.config.BoostSourceConfig;
import net.aincraft.boost.config.BoostSourceConfigParser;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.upgrade.NodeEffect;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

/**
 * Parses the JSON effect vocabulary into sealed NodeEffect instances.
 * Unknown types throw so misconfigurations are loud.
 */
public final class SkillTreeEffectParser {

  private final BoostSourceConfigParser boostSourceParser;

  public SkillTreeEffectParser(BoostFactory boostFactory, ConditionFactory conditionFactory) {
    this.boostSourceParser = new BoostSourceConfigParser(conditionFactory, boostFactory);
  }

  @NotNull
  public NodeEffect parse(@NotNull JsonElement element) {
    JsonObject obj = element.getAsJsonObject();
    String type = obj.get("type").getAsString();

    return switch (type) {
      case "boost" -> new NodeEffect.BoostEffect(
          obj.has("target") ? obj.get("target").getAsString() : NodeEffect.BoostEffect.TARGET_ALL,
          obj.has("amount") ? BigDecimal.valueOf(obj.get("amount").getAsDouble()) : BigDecimal.ONE);
      case "ruled_boost" -> parseRuledBoost(obj);
      case "permission" -> new NodeEffect.PermissionEffect(obj.get("key").getAsString());
      case "recipe_unlock" -> new NodeEffect.RecipeUnlockEffect(Key.key(obj.get("recipe").getAsString()));
      case "state_set" -> new NodeEffect.StateSetEffect(
          parseKey(obj.get("key").getAsString()),
          obj.get("value").getAsString(),
          obj.has("remove") && obj.get("remove").getAsBoolean());
      default -> throw new IllegalArgumentException("Unknown effect type: " + type);
    };
  }

  private NodeEffect parseRuledBoost(JsonObject obj) {
    String target = obj.has("target")
        ? obj.get("target").getAsString()
        : NodeEffect.BoostEffect.TARGET_ALL;
    JsonObject sourceJson = obj.deepCopy();
    sourceJson.remove("type");
    sourceJson.remove("target");
    if (!sourceJson.has("key")) {
      sourceJson.addProperty("key", "modularjobs:upgrade_tree/ruled_boost");
    }
    if (!sourceJson.has("description")) {
      sourceJson.addProperty("description", "Skill-tree ruled boost");
    }
    BoostSourceConfig sourceConfig =
        new Gson().fromJson(sourceJson, BoostSourceConfig.class);
    return new NodeEffect.RuledBoostEffect(target, boostSourceParser.parse(sourceConfig));
  }
  private Key parseKey(String raw) {
    int separator = raw.indexOf(':');
    if (separator > 0) {
      return Key.key(raw.substring(0, separator), raw.substring(separator + 1));
    }
    int dot = raw.indexOf('.');
    if (dot > 0) {
      return Key.key(raw.substring(0, dot), raw.substring(dot + 1));
    }
    throw new IllegalArgumentException("Namespaced key must use namespace.key or namespace:key: " + raw);
  }

}
```


- [ ] **Step 5: Implement the v2 config parser**

Create `jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeConfigParser.java`:

```java
package net.aincraft.upgrade.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.upgrade.NodeEffect;
import net.aincraft.upgrade.NodeLevel;
import net.aincraft.upgrade.NodeStateWrite;
import net.aincraft.upgrade.Position;
import net.aincraft.upgrade.Requirement;
import net.aincraft.upgrade.SkillNode;
import net.aincraft.upgrade.SkillNode.LevelEffectMode;
import net.aincraft.upgrade.SkillNodeKind;
import net.aincraft.upgrade.SkillTree;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Parses version-2 skill tree JSON into SkillTree instances. Validates node
 * keys, requirement/effect vocabulary, and prerequisite/exclude references.
 */
public final class SkillTreeConfigParser {

  private static final int VERSION = 2;

  private final SkillTreeRequirementParser requirementParser;
  private final SkillTreeEffectParser effectParser;

  public SkillTreeConfigParser(BoostFactory boostFactory, ConditionFactory conditionFactory) {
    this.requirementParser = new SkillTreeRequirementParser();
    this.effectParser = new SkillTreeEffectParser(boostFactory, conditionFactory);
  }

  @NotNull
  public SkillTree parse(@NotNull JsonObject root) {
    if (!root.has("version") || root.get("version").getAsInt() != VERSION) {
      throw new IllegalArgumentException("Skill tree must declare \"version\": 2");
    }
    if (!root.has("job") || !root.has("root") || !root.has("nodes")) {
      throw new IllegalArgumentException("Skill tree requires \"job\", \"root\", and \"nodes\"");
    }

    String jobKey = root.get("job").getAsString();
    String rootNodeKey = root.get("root").getAsString();
    int pointsPerLevel = root.has("skill_points_per_level")
        ? root.get("skill_points_per_level").getAsInt() : 1;
    String description = root.has("description") && !root.get("description").isJsonNull()
        ? root.get("description").getAsString() : null;

    JsonObject nodesObj = root.getAsJsonObject("nodes");
    Map<String, SkillNode> nodes = new HashMap<>();
    for (Map.Entry<String, JsonElement> entry : nodesObj.entrySet()) {
      nodes.put(entry.getKey(), parseNode(jobKey, entry.getKey(), entry.getValue().getAsJsonObject()));
    }

    validateReferences(jobKey, rootNodeKey, nodes);
    validateStateWriteConflicts(nodes);

    return new SkillTree(
        Key.key("modularjobs", "upgrade_tree/" + jobKey),
        jobKey, description, pointsPerLevel, rootNodeKey, nodes);
  }

  private SkillNode parseNode(String jobKey, String nodeKey, JsonObject obj) {
    SkillNodeKind kind = SkillNodeKind.valueOf(obj.get("kind").getAsString().toUpperCase());
    String name = obj.get("name").getAsString();
    String description = obj.has("description") && !obj.get("description").isJsonNull()
        ? obj.get("description").getAsString() : null;

    Set<String> prerequisites = parseStringSet(obj, "prerequisites");
    Set<String> excludes = parseStringSet(obj, "excludes");
    int cost = obj.has("cost") ? obj.get("cost").getAsInt() : 0;

    List<Requirement> requirements = new ArrayList<>();
    if (obj.has("requirements")) {
      requirements.add(requirementParser.parse(obj.get("requirements")));
    }

    List<NodeEffect> effects = new ArrayList<>();
    List<NodeLevel> levels = new ArrayList<>();
    if (obj.has("levels")) {
      JsonArray levelsArray = obj.getAsJsonArray("levels");
      for (JsonElement levelEl : levelsArray) {
        JsonObject levelObj = levelEl.getAsJsonObject();
        int levelCost = levelObj.has("cost") ? levelObj.get("cost").getAsInt() : 0;
        List<NodeEffect> levelEffects = new ArrayList<>();
        if (levelObj.has("effects")) {
          for (JsonElement effectEl : levelObj.getAsJsonArray("effects")) {
            levelEffects.add(effectParser.parse(effectEl));
          }
        }
        levels.add(new NodeLevel(levelCost, levelEffects));
      }
    } else if (obj.has("effects")) {
      for (JsonElement effectEl : obj.getAsJsonArray("effects")) {
        effects.add(effectParser.parse(effectEl));
      }
    }

    LevelEffectMode mode = LevelEffectMode.REPLACE;
    if (obj.has("level_effect_mode") && "cumulative".equalsIgnoreCase(obj.get("level_effect_mode").getAsString())) {
      mode = LevelEffectMode.CUMULATIVE;
    }

    List<NodeStateWrite> stateWrites = new ArrayList<>();
    if (obj.has("state")) {
      for (JsonElement stateEl : obj.getAsJsonArray("state")) {
        JsonObject stateObj = stateEl.getAsJsonObject();
        if (stateObj.has("set")) {
          JsonObject setObj = stateObj.getAsJsonObject("set");
          setObj.entrySet().forEach(e -> stateWrites.add(new NodeStateWrite(
              NodeStateWrite.Op.SET, parseKey(e.getKey()), e.getValue().getAsString())));
        } else if (stateObj.has("remove")) {
          JsonObject removeObj = stateObj.getAsJsonObject("remove");
          removeObj.entrySet().forEach(e -> stateWrites.add(new NodeStateWrite(
              NodeStateWrite.Op.REMOVE, parseKey(e.getKey()), "")));
        }
      }
    }
    if (!stateWrites.isEmpty() && kind != SkillNodeKind.MAJOR) {
      throw new IllegalArgumentException("Only major nodes may define state writes: " + nodeKey);
    }


    Material icon = Material.DIAMOND;
    try {
      if (obj.has("icon")) {
        icon = Material.valueOf(obj.get("icon").getAsString().toUpperCase());
      }
    } catch (IllegalArgumentException ignored) {
      // fall back to DIAMOND
    }

    Position position = null;
    if (obj.has("position")) {
      JsonObject posObj = obj.getAsJsonObject("position");
      position = new Position(posObj.get("x").getAsInt(), posObj.get("y").getAsInt());
    }

    return new SkillNode(
        Key.key(jobKey, nodeKey), name, description,
        icon, icon, null, null,
        kind, cost,
        kind == SkillNodeKind.SKILL ? levels.size() : 1,
        mode, levels, requirements, prerequisites, excludes, effects,
        position, List.of(), stateWrites);
  }

  private void validateStateWriteConflicts(Map<String, SkillNode> nodes) {
    List<SkillNode> majors = nodes.values().stream()
        .filter(SkillNode::isMajor)
        .toList();
    for (int i = 0; i < majors.size(); i++) {
      SkillNode left = majors.get(i);
      for (int j = i + 1; j < majors.size(); j++) {
        SkillNode right = majors.get(j);
        if (left.excludes().contains(right.key().value())
            || right.excludes().contains(left.key().value())) {
          continue;
        }
        Map<Key, String> leftWrites = new HashMap<>();
        for (NodeStateWrite write : left.stateWrites()) {
          leftWrites.put(write.key(), write.op() == NodeStateWrite.Op.REMOVE ? "" : write.value());
        }
        for (NodeStateWrite write : right.stateWrites()) {
          String leftValue = leftWrites.get(write.key());
          String rightValue = write.op() == NodeStateWrite.Op.REMOVE ? "" : write.value();
          if (leftValue != null && !leftValue.equals(rightValue)) {
            throw new IllegalArgumentException(
                "Non-exclusive majors write conflicting state key: " + write.key());
          }
        }
      }
    }
  }

  private void validateReferences(String jobKey, String rootNodeKey, Map<String, SkillNode> nodes) {
    if (!nodes.containsKey(rootNodeKey)) {
      throw new IllegalArgumentException("Root node '" + rootNodeKey + "' not found in tree " + jobKey);
    }
    for (SkillNode node : nodes.values()) {
      String nodeKey = node.key().value();
      for (String prereq : node.prerequisites()) {
        if (!nodes.containsKey(prereq)) {
          throw new IllegalArgumentException("Node '" + nodeKey + "' has unknown prerequisite '" + prereq + "'");
        }
      }
      for (String excluded : node.excludes()) {
        if (!nodes.containsKey(excluded)) {
          throw new IllegalArgumentException("Node '" + nodeKey + "' has unknown exclude '" + excluded + "'");
        }
      }
    }
  }

  private Set<String> parseStringSet(JsonObject obj, String field) {
    Set<String> result = new HashSet<>();
    if (obj.has(field)) {
      for (JsonElement el : obj.getAsJsonArray(field)) {
        result.add(el.getAsString());
      }
    }
    return result;
  }

  private Key parseKey(String raw) {
    int separator = raw.indexOf(':');
    if (separator > 0) {
      return Key.key(raw.substring(0, separator), raw.substring(separator + 1));
    }
    int dot = raw.indexOf('.');
    if (dot > 0) {
      return Key.key(raw.substring(0, dot), raw.substring(dot + 1));
    }
    throw new IllegalArgumentException("Namespaced key must use namespace.key or namespace:key: " + raw);
  }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.config.SkillTreeConfigParserTest`
Expected: PASS (5 tests).

- [ ] **Step 7: Commit**

```bash
git add jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeRequirementParser.java
        jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeEffectParser.java
        jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeConfigParser.java
        jobs-core/src/test/java/net/aincraft/upgrade/config/SkillTreeConfigParserTest.java
git commit -m "feat(core): parse version-2 skill tree JSON into SkillTree"
```

---

## Task 5: Persist SkillTreeState (node_levels JSON column + legacy migration)

**Files:**
- Create: `jobs-core/src/test/java/net/aincraft/upgrade/PlayerUpgradeRepositoryNewTest.java`
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/PlayerUpgradeDataImpl.java`
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/PlayerUpgradeRepository.java`
- Modify: `jobs-core/src/main/resources/sql/sqlite.sql`
- Modify: `jobs-api/src/main/java/net/aincraft/upgrade/PlayerUpgradeData.java`

**Interfaces:**
- Consumes: `SkillTreeState`, `PlayerUpgradeRepository` (Tasks 1; existing).
- Produces:
  - `PlayerUpgradeDataImpl` gains `state()` and `nodeLevels()` backed by an internal `SkillTreeState`; the existing legacy `(playerId, jobKey, totalSkillPoints, unlockedNodes)` constructor remains source-compatible and delegates to a state-based constructor.
  - `PlayerUpgradeRepository` gains:
    - `@Nullable SkillTreeState loadState(@NotNull String playerId, @NotNull String jobKey);`
    - `void saveState(@NotNull SkillTreeState state);`
- `PlayerUpgradeRepository.loadState` reads `total_skill_points` + `node_levels` (JSON map) and falls back to legacy `unlocked_nodes` → `node_levels: {key: 1}` when the JSON column is empty.
- `PlayerUpgradeRepository.saveState` writes `total_skill_points` + `node_levels` JSON.
  - `sqlite.sql` `player_upgrades` gains `node_levels TEXT NOT NULL DEFAULT ''`.

**Steps:**

- [ ] **Step 1: Update the schema**

Edit `jobs-core/src/main/resources/sql/sqlite.sql` — replace the `player_upgrades` block:

```sql
CREATE TABLE IF NOT EXISTS player_upgrades
(
    player_id          TEXT    NOT NULL,
    job_key            TEXT    NOT NULL,
    total_skill_points INTEGER NOT NULL DEFAULT 0,
    unlocked_nodes     TEXT    NOT NULL DEFAULT '',
    node_levels        TEXT    NOT NULL DEFAULT '',
    PRIMARY KEY (player_id, job_key)
);
```

**Important:** `CREATE TABLE IF NOT EXISTS` does NOT add columns to an existing table. Existing installations created the table without `node_levels`; without a migration the INSERT/SELECT would fail with "no such column: node_levels". Add an idempotent startup migration that checks table metadata and ALTERs:

In `jobs-core/src/main/java/net/aincraft/upgrade/PlayerUpgradeRepository.java`, run the migration at construction time:

```java
  private static final String MIGRATE_ADD_NODE_LEVELS =
      "ALTER TABLE player_upgrades ADD COLUMN node_levels TEXT NOT NULL DEFAULT ''";

  public PlayerUpgradeRepository(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
    migrateSchema();
  }

  private void migrateSchema() {
    try (Connection connection = connectionSource.getConnection()) {
      // SQLite: PRAGMA table_info; MySQL/MariaDB: information_schema.columns
      boolean hasColumn;
      try (java.sql.ResultSet rs = connection.getMetaData().getColumns(
          null, null, "player_upgrades", "node_levels")) {
        hasColumn = rs.next();
      }
      if (!hasColumn) {
        try (java.sql.Statement st = connection.createStatement()) {
          st.execute(MIGRATE_ADD_NODE_LEVELS);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to migrate player_upgrades schema", e);
    }
  }
```

The `ConnectionSource` used for upgrades is created by `ConnectionSourceFactory` at `PluginContext:209-210` BEFORE any repository code runs, so the table already exists; the ALTER is additive and idempotent (guarded by the metadata check). Because `ConnectionSource.getConnection()` is reused-per-call and the schema batch already committed, this is safe on both SQLite and relational DBs.

- [ ] **Step 2: Extend the repository interface**

Modify `jobs-core/src/main/java/net/aincraft/upgrade/PlayerUpgradeRepository.java` — add:

```java
  /**
   * Load a player's skill tree state for a job (v2 format).
   *
   * @param playerId the player's UUID
   * @param jobKey   the job key
   * @return the skill tree state, or null if none exists
   */
  @Nullable SkillTreeState loadState(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Save a player's skill tree state for a job (v2 format).
   *
   * @param state the state to save
   */
  void saveState(@NotNull SkillTreeState state);
```

- [ ] **Step 3: Extend PlayerUpgradeDataImpl and PlayerUpgradeData**

Modify `PlayerUpgradeDataImpl` to hold an internal `SkillTreeState` and derive the legacy view:

```java
  private final SkillTreeState state;

  public SkillTreeState state() {
    return state;
  }

  public Map<String, Integer> nodeLevels() {
    return state.nodeLevels();
  }

  @Override
  public int spentSkillPoints() {
    // Legacy compatibility view; v2 point arithmetic stays in SkillTree.
    return state.nodeLevels().values().stream().mapToInt(Integer::intValue).sum();
  }
```

Keep the existing `unlockedNodes()` set derived from `nodeLevels()` for the
legacy GUI path. Add abstract `SkillTreeState state()` to `PlayerUpgradeData`;
provide `nodeLevels()` as a default delegate to `state().nodeLevels()`. The
only production implementation, `PlayerUpgradeDataImpl`, supplies the state.

- [ ] **Step 4: Implement loadState/saveState in the SQL impl**

Modify `PlayerUpgradeRepository` — add:

```java
  private static final String SELECT_STATE_QUERY =
      "SELECT total_skill_points, unlocked_nodes, node_levels FROM player_upgrades WHERE player_id = ? AND job_key = ?";

  private static final String UPSERT_STATE_QUERY =
      "INSERT INTO player_upgrades (player_id, job_key, total_skill_points, unlocked_nodes, node_levels) " +
          "VALUES (?, ?, ?, '', ?) " +
          "ON CONFLICT(player_id, job_key) DO UPDATE SET " +
          "total_skill_points = excluded.total_skill_points, " +
          "node_levels = excluded.node_levels";

  @Override
  public @Nullable SkillTreeState loadState(@NotNull String playerId, @NotNull String jobKey) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SELECT_STATE_QUERY)) {
      ps.setString(1, playerId);
      ps.setString(2, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        int total = rs.getInt("total_skill_points");
        String nodeLevelsStr = rs.getString("node_levels");
        Map<String, Integer> nodeLevels;
        if (nodeLevelsStr == null || nodeLevelsStr.isBlank()) {
          // Legacy row: unlocked_nodes -> level 1 each
          nodeLevels = new HashMap<>();
          for (String key : parseNodeSet(rs.getString("unlocked_nodes"))) {
            nodeLevels.put(key, 1);
          }
        } else {
          nodeLevels = parseNodeLevels(nodeLevelsStr);
        }
        return new SkillTreeState(playerId, jobKey, total, nodeLevels, Map.of());
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load player skill tree state for " + playerId + "/" + jobKey, e);
    }
  }

  @Override
  public void saveState(@NotNull SkillTreeState state) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(UPSERT_STATE_QUERY)) {
      ps.setString(1, state.playerId());
      ps.setString(2, state.jobKey());
      ps.setInt(3, state.totalSkillPoints());
      ps.setString(4, serializeNodeLevels(state.nodeLevels()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save player skill tree state for " + state.playerId() + "/" + state.jobKey(), e);
    }
  }

  /**
   * Hydrate a loaded state's derived state map from its purchased majors.
   * {@link #loadState} returns an empty state map; the service recomputes it
   * from {@code node_levels} + the tree so major state survives a restart.
   * (Sample implementation the service calls; ordering follows node_levels
   * iteration order.)
   */
  public static SkillTreeState hydrate(SkillTree tree, SkillTreeState persisted) {
    Map<Key, String> hydrated = new HashMap<>();
    Map<String, Integer> levels = new HashMap<>(persisted.nodeLevels());
    for (Map.Entry<String, Integer> entry : levels.entrySet()) {
      if (entry.getValue() < 1) continue;
      SkillNode node = tree.node(entry.getKey()).orElse(null);
      if (node == null || !node.isMajor()) continue;
      for (NodeStateWrite write : node.stateWrites()) {
        if (write.op() == NodeStateWrite.Op.SET) {
          hydrated.put(write.key(), write.value());
        }
      }
    }
    return new SkillTreeState(
        persisted.playerId(), persisted.jobKey(), persisted.totalSkillPoints(),
        persisted.nodeLevels(), hydrated,
        persisted.currentJobLevel(), persisted.permissionCheck());
  }

  private Map<String, Integer> parseNodeLevels(String str) {
    // JSON object {"node": level}; empty -> empty map
    if (str == null || str.isBlank()) {
      return new HashMap<>();
    }
    Map<String, Integer> result = new HashMap<>();
    com.google.gson.JsonObject obj =
        new com.google.gson.Gson().fromJson(str, com.google.gson.JsonObject.class);
    if (obj != null) {
      obj.entrySet().forEach(e -> result.put(e.getKey(), e.getValue().getAsInt()));
    }
    return result;
  }

  private String serializeNodeLevels(Map<String, Integer> nodeLevels) {
    if (nodeLevels == null || nodeLevels.isEmpty()) {
      return "";
    }
    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
    nodeLevels.forEach(obj::addProperty);
    return new com.google.gson.Gson().toJson(obj);
  }
```

- [ ] **Step 5: Write the repository test**

Create `jobs-core/src/test/java/net/aincraft/upgrade/PlayerUpgradeRepositoryNewTest.java`:

```java
package net.aincraft.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import net.aincraft.repository.ConnectionSource;
import net.aincraft.repository.DatabaseType;
import net.aincraft.repository.NonClosableConnection;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Drives the v2 persistence: node_levels JSON column round-trip and legacy
 * unlocked_nodes migration fallback.
 */
class PlayerUpgradeRepositoryNewTest {

  private Connection connection;
  private PlayerUpgradeRepository repository;

  @BeforeEach
  void setUp() throws Exception {
    Class.forName("org.sqlite.JDBC");
    Connection raw = DriverManager.getConnection("jdbc:sqlite::memory:");
    connection = NonClosableConnection.create(raw);
    try (Statement st = connection.createStatement()) {
      st.execute("""
          CREATE TABLE player_upgrades (
            player_id          TEXT    NOT NULL,
            job_key            TEXT    NOT NULL,
            total_skill_points INTEGER NOT NULL DEFAULT 0,
            unlocked_nodes     TEXT    NOT NULL DEFAULT '',
            node_levels        TEXT    NOT NULL DEFAULT '',
            PRIMARY KEY (player_id, job_key)
          )
          """);
    }
    repository = new PlayerUpgradeRepository(new FixedConnectionSource(connection));
  }

  @AfterEach
  void tearDown() throws Exception {
    if (connection instanceof NonClosableConnection nc) {
      nc.shutdown();
    } else if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }

  @Test
  void saveAndLoadStateRoundTripsNodeLevels() {
    SkillTreeState state = new SkillTreeState(
        "p1", "miner", 10, Map.of("efficiency", 2, "root", 1), Map.of());
    repository.saveState(state);

    SkillTreeState loaded = repository.loadState("p1", "miner");
    assertNotNull(loaded);
    assertEquals(10, loaded.totalSkillPoints());
    assertEquals(2, loaded.levelOf("efficiency"));
    assertEquals(1, loaded.levelOf("root"));
    assertEquals(0, loaded.levelOf("missing"));
  }

  @Test
  void loadStateFallsBackToLegacyUnlockedNodes() throws Exception {
    try (var ps = connection.prepareStatement(
        "INSERT INTO player_upgrades (player_id, job_key, total_skill_points, unlocked_nodes, node_levels) VALUES (?, ?, ?, ?, '')")) {
      ps.setString(1, "p2");
      ps.setString(2, "miner");
      ps.setInt(3, 7);
      ps.setString(4, "root,efficiency");
      ps.executeUpdate();
    }

    SkillTreeState loaded = repository.loadState("p2", "miner");
    assertNotNull(loaded);
    assertEquals(7, loaded.totalSkillPoints());
    assertEquals(1, loaded.levelOf("root"));
    assertEquals(1, loaded.levelOf("efficiency"));
    assertNull(repository.loadState("nobody", "miner"));
  }

  @Test
  void constructorMigratesPreV2TableByAddingNodeLevelsColumn() throws Exception {
    // Fresh DB modeled on a pre-v2 install: no node_levels column.
    try (var st = connection.createStatement()) {
      st.execute("DROP TABLE player_upgrades");
      st.execute("""
          CREATE TABLE player_upgrades (
            player_id          TEXT    NOT NULL,
            job_key            TEXT    NOT NULL,
            total_skill_points INTEGER NOT NULL DEFAULT 0,
            unlocked_nodes     TEXT    NOT NULL DEFAULT '',
            PRIMARY KEY (player_id, job_key)
          )
          """);
      st.execute("INSERT INTO player_upgrades (player_id, job_key, total_skill_points, unlocked_nodes) " +
          "VALUES ('p3', 'miner', 4, 'root')");
    }

    repository = new PlayerUpgradeRepository(new FixedConnectionSource(connection));

    SkillTreeState loaded = repository.loadState("p3", "miner");
    assertNotNull(loaded);
    assertEquals(4, loaded.totalSkillPoints());
    assertEquals(1, loaded.levelOf("root"));
    try (var rs = connection.getMetaData().getColumns(
        null, null, "player_upgrades", "node_levels")) {
      assertTrue(rs.next(), "node_levels column must exist after migration");
    }
  }

  @Test
  void hydrateRecomputesStateMapFromPurchasedMajorsOnReload() {
    SkillTree tree = new SkillTree(
        Key.key("modularjobs", "upgrade_tree/miner"), "miner", null, 1, "root",
        java.util.Map.of(
            "root", new SkillNode(
                Key.key("miner", "root"), "Root", null,
                org.bukkit.Material.STONE, org.bukkit.Material.STONE, null, null,
                SkillNodeKind.ROOT, 0, 1, SkillNode.LevelEffectMode.REPLACE,
                java.util.List.of(), java.util.List.of(), java.util.Set.of(), java.util.Set.of(),
                java.util.List.of(), null, java.util.List.of(), java.util.List.of()),
            "weaponsmith", new SkillNode(
                Key.key("miner", "weaponsmith"), "Weaponsmith", null,
                org.bukkit.Material.NETHERITE_INGOT, org.bukkit.Material.NETHERITE_INGOT, null, null,
                SkillNodeKind.MAJOR, 5, 1, SkillNode.LevelEffectMode.REPLACE,
                java.util.List.of(), java.util.List.of(), java.util.Set.of("root"), java.util.Set.of(),
                java.util.List.of(), null, java.util.List.of(),
                java.util.List.of(new NodeStateWrite(
                    NodeStateWrite.Op.SET, Key.key("tree", "vocation"), "weaponsmith")))));

    SkillTreeState persisted = new SkillTreeState(
        "p1", "miner", 10, java.util.Map.of("root", 1, "weaponsmith", 1),
        java.util.Map.of(), () -> 5, k -> true);

    SkillTreeState hydrated = PlayerUpgradeRepository.hydrate(tree, persisted);
    assertEquals("weaponsmith", hydrated.state().get(Key.key("tree", "vocation")));
    assertEquals(1, hydrated.levelOf("weaponsmith"));
  }

  private static final class FixedConnectionSource implements ConnectionSource {
    private final Connection connection;

    FixedConnectionSource(Connection connection) {
      this.connection = connection;
    }

    @Override
    public @NotNull Connection getConnection() {
      return connection;
    }

    @Override
    public void shutdown() {}

    @Override
    public boolean isClosed() {
      return false;
    }

    @Override
    public DatabaseType getType() {
      return DatabaseType.SQLITE;
    }

    @Override
    public boolean isSetup() {
      return true;
    }
  }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.PlayerUpgradeRepositoryNewTest`
Expected: PASS (4 tests).

- [ ] **Step 7: Commit**

```bash
git add jobs-core/src/main/resources/sql/sqlite.sql
        jobs-core/src/main/java/net/aincraft/upgrade/PlayerUpgradeRepository.java
        jobs-core/src/main/java/net/aincraft/upgrade/PlayerUpgradeDataImpl.java
        jobs-api/src/main/java/net/aincraft/upgrade/PlayerUpgradeData.java
        jobs-core/src/test/java/net/aincraft/upgrade/PlayerUpgradeRepositoryNewTest.java
git commit -m "feat(core): persist node_levels JSON with legacy unlocked_nodes migration"
```

---

## Task 6: UpgradeService v2 purchase/reset engine

**Files:**
- Create: `jobs-core/src/test/java/net/aincraft/upgrade/UpgradeServiceImplNewTest.java`
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeServiceImpl.java`
- Modify: `jobs-api/src/main/java/net/aincraft/upgrade/UpgradeService.java`

**Interfaces:**
- Consumes: `SkillTree`, `SkillTreeState`, `UpgradeTree` (legacy), repository, `UpgradeEffectApplier`.
- Produces:
  - `UpgradeService` additions:
    - `@NotNull SkillTreeState getSkillTreeState(@NotNull String playerId, @NotNull String jobKey);`
    - `@NotNull PurchaseResult purchaseSkillLevel(@NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey);`
    - `@NotNull PurchaseResult purchaseMajor(@NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey);`
    - `boolean resetTree(@NotNull String playerId, @NotNull String jobKey);`  // refunds skill levels only; majors/state are permanent and preserved
    - `void clearTreeState(@NotNull String playerId, @NotNull String jobKey);` // leave-job hard clear (wipes levels, majors, state, points)
  - `sealed interface PurchaseResult permits Success, InsufficientPoints, RequirementsNotMet, PrerequisitesNotMet, ExcludedByChoice, AlreadyOwned, NodeNotFound, TreeNotFound` (in UpgradeService).
  - `UpgradeServiceImpl` implements both legacy `unlock` (delegates to v2 purchase when a v2 tree exists) and new purchase methods.

**Steps:**

- [ ] **Step 1: Write the failing test (service-level)**

Create `jobs-core/src/test/java/net/aincraft/upgrade/UpgradeServiceImplNewTest.java`:

```java
package net.aincraft.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.aincraft.service.JobService;
import net.aincraft.upgrade.UpgradeService.PurchaseResult;
import net.aincraft.upgrade.UpgradeService.PurchaseResult.Success;
import net.aincraft.upgrade.SkillNode.LevelEffectMode;
import net.aincraft.upgrade.SkillNodeKind;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * Drives the v2 purchase engine against an in-memory SkillTree:
 * level purchases spend per-level costs, majors are one-time, resets refund
 * skill levels only (majors preserved).
 */
class UpgradeServiceImplNewTest {

  private SkillTree tree() {
    SkillNode root = new SkillNode(
        Key.key("miner", "root"), "Root", null,
        Material.STONE, Material.STONE, null, null,
        SkillNodeKind.ROOT, 0, 1, LevelEffectMode.REPLACE,
        List.of(), List.of(), Set.of(), Set.of(), List.of(), null, List.of(), List.of());
    SkillNode efficiency = new SkillNode(
        Key.key("miner", "efficiency"), "Efficiency", null,
        Material.IRON_PICKAXE, Material.IRON_PICKAXE, null, null,
        SkillNodeKind.SKILL, 0, 2, LevelEffectMode.REPLACE,
        List.of(new NodeLevel(1, List.of()), new NodeLevel(2, List.of())),
        List.of(), Set.of("root"), Set.of(), List.of(), null, List.of(), List.of());
    SkillNode major = new SkillNode(
        Key.key("miner", "master"), "Master", null,
        Material.NETHERITE_INGOT, Material.NETHERITE_INGOT, null, null,
        SkillNodeKind.MAJOR, 5, 1, LevelEffectMode.REPLACE,
        List.of(), List.of(), Set.of("efficiency"), Set.of(),
        List.of(),
        null, List.of(),
        List.of(new NodeStateWrite(
            NodeStateWrite.Op.SET, Key.key("tree", "rank"), "master")));

    return new SkillTree(Key.key("modularjobs", "upgrade_tree/miner"),
        "miner", null, 1, "root",
        Map.of("root", root, "efficiency", efficiency, "master", major));
  }

  private TestUpgradeService service() {
    return new TestUpgradeService(tree());
  }

  @Test
  void purchasesSkillLevelsSpendingPerLevelCosts() {
    TestUpgradeService service = service();

    PurchaseResult buyRoot = service.purchaseSkillLevel("p1", "miner", "root");
    assertTrue(buyRoot instanceof Success);

    PurchaseResult buyEff1 = service.purchaseSkillLevel("p1", "miner", "efficiency");
    assertTrue(buyEff1 instanceof Success);
    assertEquals(1, service.stateFor("p1").levelOf("efficiency"));

    PurchaseResult buyEff2 = service.purchaseSkillLevel("p1", "miner", "efficiency");
    assertTrue(buyEff2 instanceof Success);
    assertEquals(2, service.stateFor("p1").levelOf("efficiency"));

    // total 10 points; spent = root 0 + eff1 1 + eff2 2 = 3
    assertEquals(7, service.stateFor("p1").totalSkillPoints() - service.tree().spentPoints(service.stateFor("p1")));
  }

  @Test
  void purchasesMajorOnceAndWritesState() {
    TestUpgradeService service = service();
    service.purchaseSkillLevel("p1", "miner", "root");
    service.purchaseSkillLevel("p1", "miner", "efficiency");
    service.purchaseSkillLevel("p1", "miner", "efficiency");

    PurchaseResult major = service.purchaseMajor("p1", "miner", "master");
    assertTrue(major instanceof Success);
    assertEquals("master", service.stateFor("p1").state().get(Key.key("tree", "rank")));

    // second purchase rejected
    PurchaseResult again = service.purchaseMajor("p1", "miner", "master");
    assertFalse(again instanceof Success);
  }

  @Test
  void resetRefundsSkillsButPreservesMajorState() {
    TestUpgradeService service = service();
    service.purchaseSkillLevel("p1", "miner", "root");
    service.purchaseSkillLevel("p1", "miner", "efficiency");
    service.purchaseSkillLevel("p1", "miner", "efficiency");
    service.purchaseMajor("p1", "miner", "master");

    assertTrue(service.resetTree("p1", "miner"));

    SkillTreeState state = service.stateFor("p1");
    // Ordinary skills refunded; root and major choices remain in node_levels.
    assertEquals(Map.of("root", 1, "master", 1), state.nodeLevels());
    // Major state preserved: rank still master
    assertEquals("master", state.state().get(Key.key("tree", "rank")));
    // Spent is now only the major cost (5 of 10)
    assertEquals(5, service.tree().spentPoints(state));
    assertEquals(5, service.tree().availablePoints(state));
  }

  /** Minimal in-memory service harness implementing only the v2 surface. */
  static class TestUpgradeService implements UpgradeService {
    private final SkillTree tree;
    private final Map<String, SkillTreeState> states = new java.util.HashMap<>();

    TestUpgradeService(SkillTree tree) {
      this.tree = tree;
      states.put("p1", new SkillTreeState("p1", "miner", 10, Map.of(), Map.of(), () -> 5, k -> true));
    }

    SkillTreeState stateFor(String playerId) { return states.get(playerId); }
    SkillTree tree() { return tree; }

    @Override public java.util.Optional<UpgradeTree> getTree(String jobKey) { return java.util.Optional.empty(); }
    @Override public java.util.Collection<UpgradeTree> getAllTrees() { return java.util.List.of(); }
    @Override public PlayerUpgradeData getPlayerData(String playerId, String jobKey) { return null; }
    @Override public java.util.Set<UpgradeNode> getAvailableNodes(String playerId, String jobKey) { return java.util.Set.of(); }
    @Override public UpgradeService.UnlockResult unlock(String playerId, String jobKey, String nodeKey) { return new UpgradeService.UnlockResult.TreeNotFound(jobKey); }
    @Override public void awardSkillPoints(String playerId, String jobKey, int points) {
      SkillTreeState s = states.get(playerId);
      states.put(playerId, new SkillTreeState(playerId, jobKey, s.totalSkillPoints() + points, s.nodeLevels(), s.state(), s::jobLevel, k -> true));
    }
    @Override public boolean resetUpgrades(String playerId, String jobKey) { return resetTree(playerId, jobKey); }

    @Override public SkillTreeState getSkillTreeState(String playerId, String jobKey) {
      return states.get(playerId);
    }

    @Override public PurchaseResult purchaseSkillLevel(String playerId, String jobKey, String nodeKey) {
      SkillTreeState s = states.get(playerId);
      SkillNode node = tree.node(nodeKey).orElseThrow();
      if (!tree.canPurchase(s, nodeKey)) {
        return new UpgradeService.PurchaseResult.PrerequisitesNotMet(Set.of());
      }
      int next = s.levelOf(nodeKey) + 1;
      Map<String, Integer> levels = new java.util.HashMap<>(s.nodeLevels());
      levels.put(nodeKey, next);
      SkillTreeState updated = new SkillTreeState(
          playerId, jobKey, s.totalSkillPoints(), levels, s.state(), s::jobLevel, k -> true);
      states.put(playerId, updated);
      return new PurchaseResult.Success(node, tree.availablePoints(updated));
    }

    @Override public PurchaseResult purchaseMajor(String playerId, String jobKey, String nodeKey) {
      SkillTreeState s = states.get(playerId);
      SkillNode node = tree.node(nodeKey).orElseThrow();
      if (!tree.canPurchase(s, nodeKey)) {
        return new UpgradeService.PurchaseResult.PrerequisitesNotMet(Set.of());
      }
      Map<String, Integer> levels = new java.util.HashMap<>(s.nodeLevels());
      levels.put(nodeKey, 1);
      Map<Key, String> nextState = new java.util.HashMap<>(s.state());
      for (NodeStateWrite write : node.stateWrites()) {
        if (write.op() == NodeStateWrite.Op.SET) nextState.put(write.key(), write.value());
      }
      SkillTreeState updated = new SkillTreeState(
          playerId, jobKey, s.totalSkillPoints(), levels, nextState, s::jobLevel, k -> true);
      states.put(playerId, updated);
      return new PurchaseResult.Success(node, tree.availablePoints(updated));
    }

    @Override public boolean resetTree(String playerId, String jobKey) {
      SkillTreeState s = states.get(playerId);
      // Refund ordinary SKILL levels; preserve ROOT and MAJOR levels and state.
      Map<String, Integer> levels = new java.util.HashMap<>();
      for (String ownedKey : s.nodeLevels().keySet()) {
        tree.node(ownedKey).ifPresent(node -> {
          if (!node.isSkill()) {
            levels.put(ownedKey, 1);
          }
        });
      }
      SkillTreeState refunded = new SkillTreeState(
          playerId, jobKey, s.totalSkillPoints(), levels, s.state(), s::jobLevel, k -> true);
      states.put(playerId, refunded);
      return true;
    }

    @Override public void clearTreeState(String playerId, String jobKey) {
      states.put(playerId, SkillTreeState.empty(playerId, jobKey));
    }
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.UpgradeServiceImplNewTest`
Expected: FAIL — `PurchaseResult` missing.

- [ ] **Step 3: Extend UpgradeService interface**

Modify `jobs-api/src/main/java/net/aincraft/upgrade/UpgradeService.java` — add:

```java
  /**
   * Get a player's current skill tree state for a job.
   */
  @NotNull SkillTreeState getSkillTreeState(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Purchase the next level of a skill node.
   */
  @NotNull PurchaseResult purchaseSkillLevel(@NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey);

  /**
   * Purchase (permanently choose) a major node. Requires confirmation UI-side.
   */
  @NotNull PurchaseResult purchaseMajor(@NotNull String playerId, @NotNull String jobKey, @NotNull String nodeKey);

  /**
   * Reset all skill levels, refunding their spent points. Major nodes and
   * their state writes are permanent and preserved.
   */
  boolean resetTree(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Hard-clear a player's tree state (on leaving the job). Deletes persisted data.
   */
  void clearTreeState(@NotNull String playerId, @NotNull String jobKey);

  /**
   * Result of a v2 purchase attempt.
   */
  sealed interface PurchaseResult permits
      PurchaseResult.Success,
      PurchaseResult.InsufficientPoints,
      PurchaseResult.RequirementsNotMet,
      PurchaseResult.PrerequisitesNotMet,
      PurchaseResult.ExcludedByChoice,
      PurchaseResult.AlreadyOwned,
      PurchaseResult.NodeNotFound,
      PurchaseResult.TreeNotFound {

    record Success(@NotNull SkillNode node, int remainingPoints) implements PurchaseResult { }

    record InsufficientPoints(int required, int available) implements PurchaseResult { }

    record RequirementsNotMet(@NotNull Set<String> unmet) implements PurchaseResult { }

    record PrerequisitesNotMet(@NotNull Set<String> missing) implements PurchaseResult { }

    record ExcludedByChoice(@NotNull Set<String> conflicting) implements PurchaseResult { }

    record AlreadyOwned(@NotNull String nodeKey) implements PurchaseResult { }

    record NodeNotFound(@NotNull String nodeKey) implements PurchaseResult { }

    record TreeNotFound(@NotNull String jobKey) implements PurchaseResult { }
  }
```

`awardSkillPoints` must also branch on the v2 registry: load the current
`SkillTreeState`, add the awarded points, preserve its runtime hooks, and call
`repository.saveState`; retain the existing legacy implementation otherwise.

- [ ] **Step 4: Implement in UpgradeServiceImpl**

Modify `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeServiceImpl.java` — add a v2 path that checks for a v2 `SkillTree` first, falls back to the legacy `UpgradeTree` behavior for old trees, and implements the new methods. Key logic:

```java
  @Override
  public @NotNull SkillTreeState getSkillTreeState(
      @NotNull String playerId, @NotNull String jobKey) {
    return loadOrCreateState(playerId, jobKey);
  }

  private SkillTreeState loadOrCreateState(String playerId, String jobKey) {
    SkillTreeState loaded = repository.loadState(playerId, jobKey);
    if (loaded != null) {
      // Repository state has an empty state map; hydrate it from purchased
      // majors so major state (e.g. tree.vocation) survives a restart.
      return skillTreeFor(jobKey)
          .map(tree -> PlayerUpgradeRepository.hydrate(tree, loaded))
          .orElse(loaded);
    }
    // Retroactive points + legacy migration happen in the legacy path.
    return skillTreeFor(jobKey)
        .map(tree -> SkillTreeState.empty(playerId, jobKey))
        .orElseGet(() -> SkillTreeState.empty(playerId, jobKey));
  }

  private Optional<SkillTree> skillTreeFor(String jobKey) {
    return skillTreeRegistry.stream()
        .filter(tree -> tree.jobKey().equals(jobKey))
        .findFirst();
  }
```

Add the purchase methods mirroring the test harness, writing each new state
through `repository.saveState(...)`. Effect synchronization is added in Task 7
after the state mutation contract exists; do not add an interim sync
implementation. `clearTreeState` calls `repository.deletePlayerData`.

Modify `jobs-core/src/main/java/net/aincraft/commands/UpgradesCommand.java`
`executeReset` to call the v2 reset method and accurately describe its
permanent-major behavior:

```java
boolean success = upgradeService.resetTree(playerId, jobKey);
if (success) {
  Mint.sendThemedMessage(player, "<accent>Skill upgrades reset for " + job.getPlainName()
      + "<neutral>. Major choices preserved.");
  player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
} else {
  Mint.sendThemedMessage(player, "<error>Failed to reset skill upgrades.");
  player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
}
```
- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.UpgradeServiceImplNewTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add jobs-api/src/main/java/net/aincraft/upgrade/UpgradeService.java
        jobs-core/src/main/java/net/aincraft/upgrade/UpgradeServiceImpl.java
        jobs-core/src/test/java/net/aincraft/upgrade/UpgradeServiceImplNewTest.java
git commit -m "feat(core): add v2 skill tree purchase/reset engine"
```

---

## Task 7: Derived Effect Sync (NodeEffectApplier + UpgradeEffectApplier v2)

**Files:**
- Create: `jobs-api/src/main/java/net/aincraft/upgrade/NodeEffectApplier.java`
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeEffectApplier.java`
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeServiceImpl.java`
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/UpgradePermissionRestoreListener.java`
- Test: `jobs-core/src/test/java/net/aincraft/upgrade/NodeEffectApplierTest.java`

**Interfaces:**
- Consumes: `SkillNode`, `SkillTreeState`, `NodeEffect`, `UpgradeTree`/`UpgradeNode` (legacy).
- Produces:
  - `void syncEffects(Player player, SkillTreeState previous, SkillTreeState current, SkillTree tree)` — the SINGLE mutation path. It:
    1. computes `oldSet = derive(previous, tree)`,
    2. computes `newSet = derive(current, tree)`,
    3. revokes every effect in `oldSet - newSet` (via `permissionManager.revokePermission`),
    4. grants every effect in `newSet - oldSet` (via `permissionManager.grantPermission`).
    This survives the replace-level 1→2 case: level-1 permission is in `oldSet`, absent from `newSet`, so it is revoked — even though `current` never contained it.
  - `void restoreAllForTrees(Player player, Map<SkillTree, SkillTreeState> byTree)` — the login/reload path. It unions the derived sets across ALL active trees, revokes every plugin-owned contribution once, then grants the full union. Because every grant is a Bukkit `PermissionAttachment` owned by this plugin (via `UpgradePermissionManager`), `permissionManager.cleanupPlayer` removes exactly what this plugin added — no snapshot needed, nothing stale survives a restart. This is deliberately the ONLY login entry point: a per-tree `restoreAll` would wipe other jobs' permissions then reapply only one tree.
  - `void unapplyAll(Player player, SkillTreeState state, SkillTree tree)` — whole-set clear for the given state, used ONLY by Task 10's leave path (revoke everything the current state derived before its data is deleted). Never used for mutations (`syncEffects`) or login (`restoreAllForTrees`).
- `Set<NodeEffect> derive(SkillTreeState state, SkillTree tree)` — pure computation returning the full active effect set (cumulative/replace per node and major effects); major `NodeStateWrite` entries are derived into `SkillTreeState.state` by purchase/hydration, not applied as permission effects.
  - `UpgradeEffectApplier` implements `NodeEffectApplier`:
    - `syncEffects` is the incremental mutation path; `restoreAllForTrees` is the whole-set login path; `unapplyAll` is the leave path.
    - NO per-player last-applied snapshot is persisted or cached as player progression; the attachment itself is the source of truth for revocation.

**Steps:**

- [ ] **Step 1: Write the failing test**

Create `jobs-core/src/test/java/net/aincraft/upgrade/NodeEffectApplierTest.java`:

```java
package net.aincraft.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.aincraft.test.MockBukkitSupport;
import net.aincraft.upgrade.SkillNode.LevelEffectMode;
import net.aincraft.upgrade.SkillNodeKind;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

class NodeEffectApplierTest {

  @Test
  void derivedCumulativeAndReplaceEffects() {
    SkillNode efficiency = new SkillNode(
        Key.key("miner", "efficiency"), "Efficiency", null,
        Material.IRON_PICKAXE, Material.IRON_PICKAXE, null, null,
        SkillNodeKind.SKILL, 0, 2, LevelEffectMode.CUMULATIVE,
        List.of(
            new NodeLevel(1, List.of(new NodeEffect.PermissionEffect("jobs.miner.eff1"))),
            new NodeLevel(2, List.of(new NodeEffect.PermissionEffect("jobs.miner.eff2")))),
        List.of(), Set.of("root"), Set.of(), List.of(), null, List.of(), List.of());

    SkillTree tree = new SkillTree(Key.key("modularjobs", "upgrade_tree/miner"),
        "miner", null, 1, "root",
        Map.of(
            "root", new SkillNode(Key.key("miner", "root"), "Root", null,
                Material.STONE, Material.STONE, null, null, SkillNodeKind.ROOT, 0, 1,
                LevelEffectMode.REPLACE, List.of(), List.of(), Set.of(), Set.of(),
                List.of(), null, List.of(), List.of()),
            "efficiency", efficiency));

    SkillTreeState state = new SkillTreeState(
        "p1", "miner", 10, Map.of("root", 1, "efficiency", 2), Map.of(),
        () -> 5, k -> true);

    List<NodeEffect> active = new java.util.ArrayList<>();
    for (SkillNode node : tree.nodes()) {
      active.addAll(node.activeEffects(state.levelOf(node.key().value())));
    }

    // cumulative at level 2 -> both permissions
    assertEquals(2, active.size());
    assertTrue(active.contains(new NodeEffect.PermissionEffect("jobs.miner.eff1")));
    assertTrue(active.contains(new NodeEffect.PermissionEffect("jobs.miner.eff2")));
  }

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void syncEffectsRevokesOldLevelOnReplaceUpgrade() {
    // REPLACE skill: level 1 grants eff1, level 2 grants eff2.
    // First establish the level-1 permission, then upgrade 1 -> 2.
    Player player = MockBukkitSupport.mockServer().addPlayer();
    UpgradePermissionManager permissionManager =
        new UpgradePermissionManager(new PluginMock());
    UpgradeEffectApplier applier = new UpgradeEffectApplier(permissionManager);

    SkillTree tree = new SkillTree(Key.key("modularjobs", "upgrade_tree/miner"),
        "miner", null, 1, "root",
        Map.of(
            "root", new SkillNode(Key.key("miner", "root"), "Root", null,
                Material.STONE, Material.STONE, null, null, SkillNodeKind.ROOT, 0, 1,
                LevelEffectMode.REPLACE, List.of(), List.of(), Set.of(), Set.of(),
                List.of(), null, List.of(), List.of()),
            "replace", new SkillNode(
                Key.key("miner", "replace"), "Replace", null,
                Material.IRON_PICKAXE, Material.IRON_PICKAXE, null, null,
                SkillNodeKind.SKILL, 0, 2, LevelEffectMode.REPLACE,
                List.of(
                    new NodeLevel(1, List.of(new NodeEffect.PermissionEffect("jobs.miner.r1"))),
                    new NodeLevel(2, List.of(new NodeEffect.PermissionEffect("jobs.miner.r2")))),
                List.of(), Set.of("root"), Set.of(), List.of(), null, List.of(), List.of())));

    SkillTreeState level1 = new SkillTreeState(
        "p1", "miner", 10, Map.of("root", 1, "replace", 1), Map.of(),
        () -> 5, k -> true);
    SkillTreeState level2 = new SkillTreeState(
        "p1", "miner", 10, Map.of("root", 1, "replace", 2), Map.of(),
        () -> 5, k -> true);

    SkillTreeState empty = new SkillTreeState(
        "p1", "miner", 10, Map.of(), Map.of(), () -> 5, k -> true);
    applier.syncEffects(player, empty, level1, tree);
    applier.syncEffects(player, level1, level2, tree);

    assertTrue(player.hasPermission("jobs.miner.r2"), "level-2 permission must be granted");
    assertFalse(player.hasPermission("jobs.miner.r1"), "level-1 permission must be revoked on replace upgrade");
  }

  @Test
  void restoreAllForTreesUnionsAcrossJobsAndCleansUp() {
    // Two jobs, two trees, one player: restoring must keep BOTH jobs' effects
    // (a per-tree restore would wipe the other job's permissions).
    Player player = MockBukkitSupport.mockServer().addPlayer();
    UpgradePermissionManager permissionManager =
        new UpgradePermissionManager(new PluginMock());
    UpgradeEffectApplier applier = new UpgradeEffectApplier(permissionManager);
    permissionManager.grantPermission(player, "jobs.stale");
    assertTrue(player.hasPermission("jobs.stale"));

    SkillTree miner = singleTree("miner", "jobs.miner.eff");
    SkillTree farmer = singleTree("farmer", "jobs.farmer.eff");

    SkillTreeState minerState = new SkillTreeState(
        "p1", "miner", 10, Map.of("root", 1, "eff", 1), Map.of(), () -> 5, k -> true);
    SkillTreeState farmerState = new SkillTreeState(
        "p1", "farmer", 10, Map.of("root", 1, "eff", 1), Map.of(), () -> 5, k -> true);

    applier.restoreAllForTrees(player, Map.of(miner, minerState, farmer, farmerState));

    assertTrue(player.hasPermission("jobs.miner.eff"), "miner tree effect must survive union restore");
    assertTrue(player.hasPermission("jobs.farmer.eff"), "farmer tree effect must survive union restore");
    assertFalse(player.hasPermission("jobs.stale"), "stale plugin permission must be removed before union restore");
  }

  private static SkillTree singleTree(String jobKey, String permission) {
    SkillNode root = new SkillNode(
        Key.key(jobKey, "root"), "Root", null,
        Material.STONE, Material.STONE, null, null,
        SkillNodeKind.ROOT, 0, 1, LevelEffectMode.REPLACE,
        List.of(), List.of(), Set.of(), Set.of(), List.of(), null, List.of(), List.of());
    SkillNode eff = new SkillNode(
        Key.key(jobKey, "eff"), "Eff", null,
        Material.DIAMOND, Material.DIAMOND, null, null,
        SkillNodeKind.SKILL, 0, 1, LevelEffectMode.REPLACE,
        List.of(new NodeLevel(1, List.of(new NodeEffect.PermissionEffect(permission)))),
        List.of(), Set.of("root"), Set.of(), List.of(), null, List.of(), List.of());
    return new SkillTree(Key.key("modularjobs", "upgrade_tree/" + jobKey),
        jobKey, null, 1, "root", Map.of("root", root, "eff", eff));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.NodeEffectApplierTest`
Expected: FAIL — `NodeEffectApplier` missing.

- [ ] **Step 3: Implement**

Create `jobs-api/src/main/java/net/aincraft/upgrade/NodeEffectApplier.java`:

```java
package net.aincraft.upgrade;

import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Syncs a player's active effects to the DERIVED set of their skill tree
 * state. Effects are never one-time mutations: {@link #syncEffects} diffs
 * previous and current states so a replace-level downgrade revokes the old
 * level's effects; {@link #restoreAllForTrees} clears all plugin-owned
 * contributions then applies the union of current sets (login/reload). No
 * last-applied snapshot is persisted or cached as player progression.
 */
public interface NodeEffectApplier {

  /** The full active effect set the given state implies (pure computation). */
  @NotNull Set<NodeEffect> derive(@NotNull SkillTreeState state, @NotNull SkillTree tree);

  /** Incremental mutation path: revoke old-set - new-set, grant new-set - old-set. */
  void syncEffects(
      @NotNull Player player,
      @NotNull SkillTreeState previous,
      @NotNull SkillTreeState current,
      @NotNull SkillTree tree);

  /** Login/reload path: union across all active trees; clear attachment once, apply union. */
  void restoreAllForTrees(
      @NotNull Player player,
      @NotNull Map<SkillTree, SkillTreeState> byTree);

  /** Whole-set clear for the given state (e.g. job leave before data deletion). */
  void unapplyAll(@NotNull Player player, @NotNull SkillTreeState state, @NotNull SkillTree tree);
}
```

- [ ] **Step 4: Implement in UpgradeEffectApplier**

Modify `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeEffectApplier.java` — implement `NodeEffectApplier`:

```java
  @Override
  public @NotNull Set<NodeEffect> derive(SkillTreeState state, SkillTree tree) {
    java.util.Set<NodeEffect> result = new java.util.HashSet<>();
    for (SkillNode node : tree.nodes()) {
      int level = state.levelOf(node.key().value());
      result.addAll(node.activeEffects(level));
    }
    return java.util.Set.copyOf(result);
  }

  @Override
  public void syncEffects(Player player, SkillTreeState previous, SkillTreeState current, SkillTree tree) {
    Set<NodeEffect> oldSet = derive(previous, tree);
    Set<NodeEffect> newSet = derive(current, tree);

    java.util.Set<NodeEffect> toRevoke = new java.util.HashSet<>(oldSet);
    toRevoke.removeAll(newSet);
    for (NodeEffect effect : toRevoke) {
      revokeEffect(player, effect);
    }

    java.util.Set<NodeEffect> toGrant = new java.util.HashSet<>(newSet);
    toGrant.removeAll(oldSet);
    for (NodeEffect effect : toGrant) {
      applyEffect(player, effect);
    }
  }

  @Override
  public void restoreAllForTrees(Player player, Map<SkillTree, SkillTreeState> byTree) {
    Set<NodeEffect> union = new java.util.HashSet<>();
    for (Map.Entry<SkillTree, SkillTreeState> entry : byTree.entrySet()) {
      union.addAll(derive(entry.getValue(), entry.getKey()));
    }
    permissionManager.cleanupPlayer(player.getUniqueId()); // clears THIS plugin's attachment only
    for (NodeEffect effect : union) {
      applyEffect(player, effect);
    }
    // Derived set only contains PermissionEffect contributions; boosts and
    // ruled_boosts are NOT applied here — they flow exclusively through
    // UpgradeBoostDataService (Task 8) which reads the same SkillTreeState.
    // state_set writes are not applied here either: the service persisted
    // node_levels and hydration recomputes the derived state map on load.
  }

  @Override
  public void unapplyAll(Player player, SkillTreeState state, SkillTree tree) {
    for (NodeEffect effect : derive(state, tree)) {
      revokeEffect(player, effect);
    }
  }

  private void applyEffect(Player player, NodeEffect effect) {
    if (effect instanceof NodeEffect.PermissionEffect perm) {
      for (String permission : perm.permissions()) {
        permissionManager.grantPermission(player, permission);
      }
      return;
    }
    // BoostEffect / RuledBoostEffect / StateSetEffect are NOT applied here.
    // They are pure derived data:
    //   - boosts -> UpgradeBoostDataService reads SkillTreeState (Task 8)
    //   - StateSetEffect -> purchaseMajor writes it into the state map directly
    // The applier's contract covers PermissionEffect only; everything else is
    // handled by its owning subsystem, and derive() still reports them for
    // callers that observe the full set.
  }

  private void revokeEffect(Player player, NodeEffect effect) {
    if (effect instanceof NodeEffect.PermissionEffect perm) {
      for (String permission : perm.permissions()) {
        permissionManager.revokePermission(player, permission);
      }
    }
  }
```

Add the legacy `applyNodeEffects`/`unapplyNodeEffects` unchanged (they still
back the old GUI path temporarily).

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.NodeEffectApplierTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Wire the sync into UpgradeServiceImpl**

In `UpgradeServiceImpl.purchaseSkillLevel/purchaseMajor/resetTree`, capture `previousState` before the mutation and, after persisting the new state, call:

```java
Player player = Bukkit.getPlayer(UUID.fromString(playerId));
if (player != null && player.isOnline()) {
  effectApplier.syncEffects(player, previousState, newState, tree);
}
```

On `UpgradePermissionRestoreListener.onPlayerJoin` (login), call a single union-restore method on the applier (which performs the cleanup internally — the listener never manages cleanup itself):

```java
import net.aincraft.registry.Registry;

// UpgradePermissionRestoreListener fields/constructor:
private final Registry<SkillTree> skillTreeRegistry;

public UpgradePermissionRestoreListener(
    UpgradeService upgradeService,
    UpgradeEffectApplier effectApplier,
    UpgradePermissionManager permissionManager,
    Registry<SkillTree> skillTreeRegistry) {
  this.upgradeService = upgradeService;
  this.effectApplier = effectApplier;
  this.permissionManager = permissionManager;
  this.skillTreeRegistry = skillTreeRegistry;
}

// Inside onPlayerJoin:
Map<SkillTree, SkillTreeState> byTree = new java.util.HashMap<>();
for (SkillTree tree : skillTreeRegistry) {
  byTree.put(tree, upgradeService.getSkillTreeState(playerId, tree.jobKey()));
}
effectApplier.restoreAllForTrees(player, byTree);
```

(`restoreAllForTrees` is defined on `NodeEffectApplier` and implemented in `UpgradeEffectApplier` as shown in Steps 3–4 above. `previousState` in the mutation hooks is the snapshot taken before the mutation and lives only for the call duration — no persistent snapshot.)

- [ ] **Step 7: Commit**

```bash
git add jobs-api/src/main/java/net/aincraft/upgrade/NodeEffectApplier.java
        jobs-core/src/main/java/net/aincraft/upgrade/UpgradeEffectApplier.java
        jobs-core/src/main/java/net/aincraft/upgrade/UpgradeServiceImpl.java
        jobs-core/src/main/java/net/aincraft/upgrade/UpgradePermissionRestoreListener.java
        jobs-core/src/test/java/net/aincraft/upgrade/NodeEffectApplierTest.java
git commit -m "feat(core): derive and sync active effects from skill tree state"
```

---

## Task 8: UpgradeBoostDataService from SkillTreeState

**Files:**
- Modify: `jobs-api/src/main/java/net/aincraft/upgrade/UpgradeBoostDataService.java`
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeBoostDataServiceImpl.java`
- Test: `jobs-core/src/test/java/net/aincraft/upgrade/UpgradeBoostDataServiceNewTest.java`

**Interfaces:**
- Consumes: `SkillTreeState`, `SkillTree`, `BoostSource` composition (existing `BoostSourceConfigParser` for `ruled_boost`).
- Produces:
  - `UpgradeBoostDataService` gains:
    - `List<BoostSource> getBoostSources(@NotNull UUID playerId, @NotNull Key jobKey);` (existing, now state-driven)
  - Internally: `buildBoostSourcesForState(SkillTreeState, SkillTree)`.

**Steps:**


- [ ] **Step 1: Write the failing test**

Create `jobs-core/src/test/java/net/aincraft/upgrade/UpgradeBoostDataServiceNewTest.java`:

```java
package net.aincraft.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.aincraft.container.BoostSource;
import net.aincraft.upgrade.SkillNode.LevelEffectMode;
import net.aincraft.upgrade.SkillNodeKind;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class UpgradeBoostDataServiceNewTest {

  private static SkillTree treeWithBoost() {
    SkillNode root = new SkillNode(
        Key.key("miner", "root"), "Root", null,
        Material.STONE, Material.STONE, null, null,
        SkillNodeKind.ROOT, 0, 1, LevelEffectMode.REPLACE,
        List.of(), List.of(), Set.of(), Set.of(), List.of(), null, List.of(), List.of());
    SkillNode efficiency = new SkillNode(
        Key.key("miner", "efficiency"), "Efficiency", null,
        Material.IRON_PICKAXE, Material.IRON_PICKAXE, null, null,
        SkillNodeKind.SKILL, 0, 1, LevelEffectMode.REPLACE,
        List.of(new NodeLevel(1, List.of(new NodeEffect.BoostEffect("xp", java.math.BigDecimal.valueOf(1.25))))),
        List.of(), Set.of("root"), Set.of(), List.of(), null, List.of(), List.of());

    return new SkillTree(Key.key("modularjobs", "upgrade_tree/miner"),
        "miner", null, 1, "root",
        Map.of("root", root, "efficiency", efficiency));
  }

  @Test
  void boostSourcesDerivedFromState() {
    SkillTree tree = treeWithBoost();
    SkillTreeState state = new SkillTreeState(
        "p1", "miner", 10, Map.of("root", 1, "efficiency", 1), Map.of(),
        () -> 5, k -> true);

    List<BoostSource> sources = UpgradeBoostDataServiceImpl.buildBoostSourcesForState(state, tree);
    assertEquals(1, sources.size());
    assertTrue(sources.get(0).key().asString().contains("efficiency"));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.UpgradeBoostDataServiceNewTest`
Expected: FAIL — method missing.

- [ ] **Step 3: Implement state-driven aggregation**

Modify `UpgradeBoostDataServiceImpl`:
- Add a `Registry<SkillTree> skillTreeRegistry` constructor dependency while retaining the legacy `Registry<UpgradeTree>` for fallback.
- Check the v2 registry before the legacy registry; this keeps boost lookup state-driven for v2 jobs and preserves the existing legacy path.

- Convert `getBoostSources(UUID, Key)` to:
  1. `repository.loadState(playerIdStr, jobKeyStr)` (v2) — fall back to legacy `loadPlayerData` if null.
  2. Find the matching `SkillTree` in the registry.
  3. Build `BoostSource`s from `node.activeEffects(level)` for each owned node, mapping `BoostEffect` → `SimpleUpgradeBoostSource` and `RuledBoostEffect` → its embedded `BoostSource`.
- Expose `static List<BoostSource> buildBoostSourcesForState(SkillTreeState state, SkillTree tree)` for direct testing.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.UpgradeBoostDataServiceNewTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jobs-api/src/main/java/net/aincraft/upgrade/UpgradeBoostDataService.java
        jobs-core/src/main/java/net/aincraft/upgrade/UpgradeBoostDataServiceImpl.java
        jobs-core/src/main/java/net/aincraft/upgrade/config/SkillTreeEffectParser.java
        jobs-core/src/test/java/net/aincraft/upgrade/UpgradeBoostDataServiceNewTest.java
git commit -m "feat(core): derive boost sources from skill tree state"
```

---

## Task 9: Loader v2 detection + PluginContext wiring

**Files:**
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/config/UpgradeTreeLoader.java`
- Modify: `jobs-core/src/main/java/net/aincraft/PluginContext.java`
- Test: `jobs-core/src/test/java/net/aincraft/upgrade/config/UpgradeTreeLoaderV2Test.java`

**Interfaces:**
- Consumes: `SkillTreeConfigParser`, existing loaders.
- Produces:
  - `UpgradeTreeLoader` gains a `Registry<SkillTree> skillTreeRegistry`, a `SkillTreeConfigParser`, and a `load()` path that checks `version == 2` on each tree file before delegating to legacy parsers.
  - `PluginContext` constructs and passes the `SkillTree` registry + parser, passes it into `UpgradeServiceImpl`, `UpgradeBoostDataServiceImpl`, `UpgradeLevelUpListener`, and the login restore listener.
- `UpgradeTreeLoader.convertLegacy(UpgradeTree)` returns a `SkillTree` migration adapter for legacy trees; its implementation is owned by this task and committed with `UpgradeTreeLoader.java`.

**Steps:**

- [ ] **Step 1: Write the failing test (loader integration, no real files)**

Create `jobs-core/src/test/java/net/aincraft/upgrade/config/UpgradeTreeLoaderV2Test.java`:

```java
package net.aincraft.upgrade.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import net.aincraft.boost.BoostFactoryImpl;
import net.aincraft.upgrade.SkillTree;
import org.junit.jupiter.api.Test;

class UpgradeTreeLoaderV2Test {

  @Test
  void loaderDelegatesV2ToSkillTreeParser() {
    JsonObject root = JsonParser.parseString("""
        {
          "version": 2,
          "job": "miner",
          "root": "root",
          "nodes": {
            "root": { "kind": "root", "name": "Root" }
          }
        }
        """).getAsJsonObject();

    SkillTree tree = new SkillTreeConfigParser(
        BoostFactoryImpl.INSTANCE, BoostFactoryImpl.INSTANCE).parse(root);

    assertEquals("miner", tree.jobKey());
    assertTrue(tree.node("root").isPresent());
    assertEquals(1, tree.nodes().size());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.config.UpgradeTreeLoaderV2Test`
Expected: PASS already (parser works); this test exists to guard the loader delegation in Step 4.

- [ ] **Step 3: Extend the loader**

Modify `UpgradeTreeLoader`:

- Add fields: `private final SkillTreeConfigParser skillTreeParser;`
- Add constructor param `Registry<SkillTree> skillTreeRegistry` (and a `SkillTreeConfigParser` built from existing factories).
- In `loadFromFolder` and `loadLegacyFormat`/`loadFlatWynncraftFormat`, before the existing `layout`/legacy checks, add:

```java
if (treeObj.has("version") && treeObj.get("version").getAsInt() == 2) {
  SkillTree tree = skillTreeParser.parse(treeObj);
  skillTreeRegistry.register(tree);
  plugin.getLogger().info("Loaded v2 skill tree: " + treeId + " (jobKey=" + tree.jobKey() + ")");
  continue;
}
```

Add `convertLegacy(UpgradeTree legacy)` to `UpgradeTreeLoader`:

- Group legacy nodes by `perkId` and sort each group by `level`.
- Build one v2 `SkillNode` per perk with per-level `cost`/`effects`, preserving
  MAX versus ADDITIVE policy as `REPLACE` versus `CUMULATIVE`.
- Map numeric legacy keys such as `efficiency_1` into level slots; map
  `exclusive` to explicit `excludes`; derive children; preserve positions.
- Register the converted tree in `skillTreeRegistry` while retaining the
  original `UpgradeTree` in the legacy registry for existing callers.

- [ ] **Step 4: Wire PluginContext**

Modify `PluginContext.java`:

```java
Registry<SkillTree> skillTreeRegistry = new SimpleRegistryImpl<>();
UpgradeTreeLoader upgradeTreeLoader = new UpgradeTreeLoader(
    plugin, gson, upgradeTreeRegistry, skillTreeRegistry, conditionFactory, boostFactory);
upgradeTreeLoader.load();
```
Update the existing `UpgradeBoostDataServiceImpl` construction to pass both
registries:

```java
UpgradeBoostDataService upgradeBoostDataService =
    new UpgradeBoostDataServiceImpl(
        playerUpgradeRepository, upgradeTreeRegistry, skillTreeRegistry);
```

and pass `skillTreeRegistry` into `UpgradeServiceImpl` (new constructor param).
and construct the listener with the same registry so login hydration restores
the union across every v2 tree:

```java
listenerList.add(new UpgradePermissionRestoreListener(
    upgradeService, effectApplier, permissionManager, skillTreeRegistry));
```
Update the registered level-up listener constructor to pass the same registry:

```java
listenerList.add(new UpgradeLevelUpListener(upgradeService, skillTreeRegistry));
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.config.UpgradeTreeLoaderV2Test`
Expected: PASS.

- [ ] **Step 6: Full module test**

Run: `./gradlew :jobs-core:test`
Expected: all previous + new tests pass (no regressions in `UpgradeTreeConfigParserTest`, `BoostEngineAggregationTest`).

- [ ] **Step 7: Commit**

```bash
git add jobs-core/src/main/java/net/aincraft/upgrade/config/UpgradeTreeLoader.java
        jobs-core/src/main/java/net/aincraft/PluginContext.java
        jobs-core/src/test/java/net/aincraft/upgrade/config/UpgradeTreeLoaderV2Test.java
git commit -m "feat(core): load v2 skill trees alongside legacy trees"
```

---

## Task 10: Leave-job clears tree state

**Files:**
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeLevelUpListener.java` (registered listener)
- Modify: `jobs-core/src/main/java/net/aincraft/upgrade/UpgradeServiceImpl.java` (clearTreeState already exists; ensure it revokes effects)
- Test: `jobs-core/src/test/java/net/aincraft/upgrade/UpgradeLevelUpListenerLeaveTest.java`

**Interfaces:**
- Consumes: `UpgradeService.clearTreeState`, `JobLeaveEvent`.
- Produces: leave path clears v2 tree data and revokes effect permissions on `JobLeaveEvent`.
- `UpgradeLevelUpListener` also receives `Registry<SkillTree>` so v2 jobs use their tree's `skillPointsPerLevel`; legacy jobs retain the existing `UpgradeTree` path.

**Steps:**

Before adding `onJobLeave`, update `onJobLevelUp` to check the v2 registry
first. For a matching `SkillTree`, compare `newLevel * tree.skillPointsPerLevel()`
with `upgradeService.getSkillTreeState(...).totalSkillPoints()`, award only the
difference through `awardSkillPoints`, and report availability from the updated
state. Return without entering the legacy `UpgradeTree` branch.

- [ ] **Step 1: Write the failing test (pure service-level: clearTreeState deletes and revokes)**

Create `jobs-core/src/test/java/net/aincraft/upgrade/UpgradeLevelUpListenerLeaveTest.java` — construct a mocked `UpgradeService` (or reuse `TestUpgradeService` from Task 6) and assert that the registered leave handler calls `clearTreeState` with the player's UUID and job key. Keep the service-level assertion that a subsequent `getSkillTreeState` returns `SkillTreeState.empty` and persisted data is deleted.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.UpgradeLevelUpListenerLeaveTest`
Expected: FAIL until `clearTreeState` is implemented in `UpgradeServiceImpl`.

- [ ] **Step 3: Implement clearTreeState**

In `UpgradeServiceImpl.clearTreeState`:

```java
  @Override
  public void clearTreeState(String playerId, String jobKey) {
    SkillTreeState state = getSkillTreeState(playerId, jobKey);
    Player player = Bukkit.getPlayer(UUID.fromString(playerId));
    if (player != null && player.isOnline()) {
      skillTreeFor(jobKey).ifPresent(tree ->
          effectApplier.unapplyAll(player, state, tree));
    }
    repository.deletePlayerData(playerId, jobKey);
    Map<String, PlayerUpgradeDataImpl> jobMap = cache.get(playerId);
    if (jobMap != null) {
      jobMap.remove(jobKey);
    }
  }
```

- [ ] **Step 4: Wire the leave event**

Modify `UpgradeLevelUpListener.onJobLeave`:

```java
import net.aincraft.event.JobLeaveEvent;

@EventHandler(priority = EventPriority.MONITOR)
public void onJobLeave(JobLeaveEvent event) {
  String playerId = event.getPlayer().getUniqueId().toString();
  String jobKey = event.getJob().key().value();
  // Leaving wipes levels, majors, state, and points.
  upgradeService.clearTreeState(playerId, jobKey);
}
```

The registered listener already holds `UpgradeService`; no constructor change is needed.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.UpgradeLevelUpListenerLeaveTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add jobs-core/src/main/java/net/aincraft/upgrade/UpgradeLevelUpListener.java
        jobs-core/src/main/java/net/aincraft/upgrade/UpgradeServiceImpl.java
        jobs-core/src/test/java/net/aincraft/upgrade/UpgradeLevelUpListenerLeaveTest.java
git commit -m "feat(core): clear skill tree state when leaving a job"
```

---

## Task 11: GUI renders SkillTree + major confirmation flow

**Files:**
- Modify: `jobs-core/src/main/java/net/aincraft/gui/UpgradeTreeGui.java`
- Test: `jobs-core/src/test/java/net/aincraft/gui/UpgradeTreeGuiConfirmationTest.java`

**Interfaces:**
- Consumes: `SkillTree`, `SkillTreeState`, `UpgradeService.purchaseSkillLevel/purchaseMajor`.
- Produces: GUI shows level badges for skills, a confirm slot for majors (uses the free control-row slot 52), and routes clicks to the new purchase methods.

**Steps:**

- [ ] **Step 1: Add a confirmation session field**

In `GuiSession`, add:

```java
String pendingMajorKey; // set when a major is clicked, null otherwise
```

- [ ] **Step 2: Render from SkillTreeState**

Rewrite `renderNodes` to iterate `SkillTree.nodes()` (when the tree has a v2 `SkillTree`; fall back to the legacy `UpgradeTree` path when only legacy exists) and use `data.nodeLevels()` for status:

- `UNLOCKED` when `level > 0`.
- `AVAILABLE` when `tree.canPurchase(state, key)` and not a major.
- `LOCKED` otherwise.
- For majors that `canPurchase`, render the node with an explicit `Confirm` hint and a gold `?` badge.

- [ ] **Step 3: Click routing with confirmation**

In `onInventoryClick`:

- If a node is a major and not yet confirmed (no `pendingMajorKey`), set `session.pendingMajorKey = nodeKey`, refresh the GUI, and put a `CONFIRM` item in control-row slot 52.
- If slot 52 (`CONFIRM` action) is clicked and `pendingMajorKey` is set, call `upgradeService.purchaseMajor(...)`; on success clear pending and refresh.
- Otherwise call `upgradeService.purchaseSkillLevel(...)`.
- `AlreadyOwned`/`ExcludedByChoice`/`RequirementsNotMet` map to themed messages like the legacy switch.

- [ ] **Step 4: Write the confirmation test**

Create `jobs-core/src/test/java/net/aincraft/gui/UpgradeTreeGuiConfirmationTest.java` — use MockBukkit to open the GUI, click a major node, assert a `CONFIRM` item appears in slot 52, click it, assert `purchaseMajor` was invoked (via a fake `UpgradeService` recording calls).

MockBukkit setup mirrors the existing `commit`-style tests: `MockBukkit.mock()`, register the GUI as a listener, `player.getOpenInventory()`, simulate `InventoryClickEvent`.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.gui.UpgradeTreeGuiConfirmationTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add jobs-core/src/main/java/net/aincraft/gui/UpgradeTreeGui.java
        jobs-core/src/test/java/net/aincraft/gui/UpgradeTreeGuiConfirmationTest.java
git commit -m "feat(core): render v2 trees and add major purchase confirmation"
```

---

## Task 12: Convert example miner tree to v2 + legacy migration adapter test

**Files:**
- Modify: `jobs-core/src/main/resources/upgrade_trees/miner.json`
- Create: `jobs-core/src/test/java/net/aincraft/upgrade/config/LegacyToV2MigrationTest.java`

**Interfaces:**
- Consumes: `UpgradeTreeLoader.convertLegacy(UpgradeTree)` from Task 9, `SkillTreeConfigParser`, and `UpgradeTreeConfigParser` (legacy).
- Produces: a documented v2 `miner.json` example; a migration test proving legacy `unlocked_nodes` → `node_levels` conversion.

**Steps:**

- [ ] **Step 1: Write the migration test**

Create `jobs-core/src/test/java/net/aincraft/upgrade/config/LegacyToV2MigrationTest.java` — parse the legacy format through `UpgradeTreeConfigParser`, then assert a legacy `unlocked_nodes` row loads through `PlayerUpgradeRepository.loadState` into `node_levels: {key: 1}` (already covered in Task 5; extend with a tree-level check that a legacy `efficiency_1` node maps to a v2 `efficiency` level-1 node).

- [ ] **Step 2: Run test to verify the Task 9 converter contract**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.config.LegacyToV2MigrationTest`
Expected: FAIL if Task 9's converter is incomplete; PASS after Task 9.

- [ ] **Step 3: Validate the existing conversion helper**

Use `UpgradeTreeLoader.convertLegacy(legacy)`; do not add another converter
class or duplicate conversion logic in Task 12. Assert:

- Legacy nodes group by `perkId` and sort by `level`.
- Numeric legacy keys become v2 level slots, with costs/effects preserved.
- Legacy `exclusive` becomes v2 `excludes`, children are derived, and positions
  remain intact.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :jobs-core:test --tests net.aincraft.upgrade.config.LegacyToV2MigrationTest`
Expected: PASS.

- [ ] **Step 5: Convert miner.json to v2**

Rewrite `jobs-core/src/main/resources/upgrade_trees/miner.json` in v2 format:

- `nether_lava_immunity`, `far_gather`, `explosion_collection` become levelled `kind: skill` nodes with their per-level costs.
- `allay_branch`/`bat_branch`/`goat_branch`/`copper_golem_branch` become `kind: major` nodes with `excludes` covering the other three branches.
- Keep positions from the current layout.

- [ ] **Step 6: Run the full module suite**

Run: `./gradlew :jobs-core:test`
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add jobs-core/src/main/resources/upgrade_trees/miner.json
        jobs-core/src/test/java/net/aincraft/upgrade/config/LegacyToV2MigrationTest.java
git commit -m "feat(core): convert miner example tree to v2 with migration adapter"
```

---

## Self-Review

**Spec coverage:**
- Node model with root/skill/major → Task 1.
- Per-level costs + cumulative/replace → Tasks 1, 3.
- Requirement tree all/any/not → Task 2.
- Excludes explicit+symmetric → Task 3.
- State writes major-only → Tasks 1, 6.
- Derived effect evaluation → Task 7.
- Economy skill-points-only, plain-int → Tasks 1, 3, 6.
- Player state persisted node_levels → Task 5.
- Leave clears tree → Task 10.
- Pet `/upgrade` untouched → explicit non-goal honored (no edits to `UpgradeCommand`/`PetSelectionGui`).
- GUI confirmation → Task 11.

**Placeholder scan:** no incomplete or deferred implementation remains.

**Type consistency check:**
- `SkillNode.LevelEffectMode` used identically in Tasks 1, 3, 6, 7, 8.
- `SkillTree.canPurchase(SkillTreeState, String)` consistent across Tasks 3, 6, 11.
- `SkillTreeState` has a 5-argument persisted-state constructor plus a 7-argument constructor with runtime job-level and permission suppliers; all call sites use the matching form.
- `repository.loadState/saveState` defined in Task 5, consumed in 6, 8, 10.
- `NodeEffectApplier.syncEffects/derive/restoreAllForTrees` defined in Task 7, consumed in 6 (mutations), 10 (leave), login listener (union restore).
- `NodeEffectApplier.unapplyAll` kept for Task 10's leave path only: it revokes the whole derived set of the CURRENT state (the only state available before its data is deleted). It is intentionally NOT used for mutations (which use `syncEffects`) and NOT used on login (which uses `restoreAllForTrees`) — leaving no stale-effect window.