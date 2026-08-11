# Module Layout (`api` / `common` / `paper` / `web`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Historical note (2026-08-10):** Retained as an implementation record; its paths and dependency assumptions are not current distribution guidance.

**Goal:** Restructure the monorepo to modules `api`, `common`, `paper`, and `web` (with session-api nested under web), and make `paper` the only module that imports Paper/Bukkit while `api` remains the pure public contract.

**Architecture:** Mechanical `git mv` renames first, then introduce pure `common` DTOs, then systematically replace Bukkit types in `api` with UUID / string keys / pure events. Paper maps at the edge (UUID↔Player, string↔Material, pure event + optional Bukkit wrapper). Work runs in an isolated git worktree that includes current master WIP.

**Tech Stack:** Java 25 toolchain, Gradle multi-module, Paper 26.2 (paper module only), Adventure API (api/common, no Paper), JUnit 5, MockBukkit (paper tests only), Rust/axum session-api, React session-editor, Astro docs.

**Spec:** `docs/superpowers/specs/2026-08-06-module-layout-design.md`

## Global Constraints

- Java package root stays `net.aincraft` (no package rename).
- `api` and `common`: **zero** `org.bukkit` / `io.papermc` imports; no `libs.paper.api` / MockBukkit.
- `paper`: only Gradle module with Paper/MockBukkit.
- Adventure (`net.kyori.adventure.*`) is allowed in `api` (pure library); declare explicit Maven deps once Paper is removed from `api`.
- Do **not** change Postgres schema ownership, session auth semantics, or payload field names (`camelCase` JSON).
- Prefer `git mv` for renames so history is preserved.
- Atomic commits per task; keep intermediate trees green when possible.
- After renames, grep for stale paths: `jobs-api`, `jobs-core`, `jobs-web`, `jobs-session-api`.

## Locked decisions (from spec + this plan)

| Topic | Decision |
|-------|----------|
| Shadow jar name | Accept `paper-all.jar`; update CI artifact name/path |
| Rust crate name | `session-api` (rename in `Cargo.toml`) |
| Bukkit events for other plugins | **Keep dual-fire in v1:** pure events on `EventBus` in `api`; thin Bukkit wrappers in `paper` under `net.aincraft.paper.event` still fired for in-server listeners |
| Editor DTOs | Move plugin-side records to `common` (`net.aincraft.common.editor`) |
| `Bridge.plugin()` | Removed from `api`; paper uses bootstrap/`JavaPlugin` reference directly for schedulers |
| WIP | Capture onto restructure branch before renames |

## File structure (end state)

```
api/                          # was jobs-api — pure public API
common/                       # NEW — pure DTOs
paper/                        # was jobs-core — Paper plugin
web/                          # was jobs-web
  session-editor/
  session-api/                # was jobs-session-api
settings.gradle.kts           # include("api", "common", "paper")
.github/workflows/ci.yml      # :api :common :paper paths
scripts/apply-postgres-schema.sh  # paper/.../postgres.sql
```

**Key files created or heavily rewritten:**

| Path | Role |
|------|------|
| `common/build.gradle.kts` | Pure Java library |
| `common/src/main/java/net/aincraft/common/editor/*` | Editor payload DTOs (from paper `editor/json`) |
| `api/build.gradle.kts` | No paper; depends on `:common` + adventure + annotations |
| `api/.../Bridge.java` | Static holder, no Bukkit |
| `api/.../event/EventBus.java` | Pure event publish/subscribe |
| `api/.../event/*Event.java` | Pure event types (UUID, no Bukkit Event) |
| `api/.../container/Context.java` | Pure sealed contexts (string keys / coords) |
| `paper/.../paper/event/*` | Optional Bukkit `Event` wrappers + dual-fire helper |
| `paper/.../paper/BukkitContexts.java` | Bukkit → pure `Context` mappers |
| `web/session-api/` | Rust crate (moved) |

**Dependency graph:**

```
common  (JDK + jetbrains annotations + gson for @SerializedName on editor DTOs)
  ↑
api     (common + adventure-api + jetbrains annotations)
  ↑
paper   (api + common + paper-api compileOnly + existing plugin deps)
```

---

## Task 1: Worktree + capture WIP

**Files:**
- None in repo content except whatever is in current WIP
- Create worktree: `.worktrees/module-layout`

- [ ] **Step 1: Confirm isolation baseline**

```bash
cd /home/jlo/dev/modularjobs
GIT_DIR=$(cd "$(git rev-parse --git-dir)" && pwd -P)
GIT_COMMON=$(cd "$(git rev-parse --git-common-dir)" && pwd -P)
echo "GIT_DIR=$GIT_DIR"
echo "GIT_COMMON=$GIT_COMMON"
git status -sb
git check-ignore -q .worktrees && echo "OK ignored"
```

Expected: main checkout (`GIT_DIR == GIT_COMMON` or normal repo), `.worktrees` ignored, dirty WIP may exist.

- [ ] **Step 2: Commit or stash WIP on master so it can ride the branch**

If there is uncommitted work, commit it on a temporary holding commit **or** include it when creating the branch. Preferred:

```bash
# On master: stage all intentional WIP (do not add secrets)
git add -A
git status
# If there are changes:
git commit -m "wip: capture in-progress work before module-layout restructure"
```

If the user prefers not to commit WIP on master, instead:

```bash
git stash push -u -m "module-layout-wip"
```

- [ ] **Step 3: Create worktree branch with WIP**

```bash
git worktree add .worktrees/module-layout -b refactor/module-layout
cd .worktrees/module-layout
# If stashed instead of committed:
# git stash pop
git status -sb
git log -3 --oneline
```

Expected: branch `refactor/module-layout`, WIP present, ready to edit.

- [ ] **Step 4: Sanity build before moves**

```bash
./gradlew :jobs-api:test :jobs-core:test --console=plain
```

Expected: BUILD SUCCESSFUL (or known pre-existing failures documented in the commit message). If WIP breaks the build, fix WIP first or note baseline failures before renames.

- [ ] **Step 5: Commit point (if only worktree bookkeeping remains)**

No extra commit required if Step 2 already captured WIP. Proceed to Task 2.

---

## Task 2: Mechanical Gradle module renames

**Files:**
- Rename: `jobs-api/` → `api/`
- Rename: `jobs-core/` → `paper/`
- Rename: `jobs-web/` → `web/`
- Modify: `settings.gradle.kts`
- Modify: `paper/build.gradle.kts` (was jobs-core; `project(":api")`)
- Modify: `.github/workflows/ci.yml`
- Modify: any scripts/docs that reference paths (minimal in this task; full docs in Task 12)

- [ ] **Step 1: git mv modules**

```bash
cd /home/jlo/dev/modularjobs/.worktrees/module-layout
git mv jobs-api api
git mv jobs-core paper
git mv jobs-web web
```

- [ ] **Step 2: Update `settings.gradle.kts`**

Replace:

```kotlin
include("jobs-api", "jobs-core")
```

with:

```kotlin
include("api", "common", "paper")
```

Note: `common` is included early but the directory is added in Task 4. **If Gradle fails on missing project dir**, either (a) create empty `common/` in this task, or (b) only `include("api", "paper")` until Task 4. **Prefer (a):** create stub now:

```bash
mkdir -p common/src/main/java
```

`common/build.gradle.kts` (minimal):

```kotlin
dependencies {
    // pure module — populated in Task 4
}
```

- [ ] **Step 3: Update `paper/build.gradle.kts`**

Replace:

```kotlin
implementation(project(":jobs-api"))
```

with:

```kotlin
implementation(project(":api"))
```

- [ ] **Step 4: Update CI**

In `.github/workflows/ci.yml`:

```yaml
      - name: Unit tests
        run: ./gradlew :api:test :paper:test --console=plain

      - name: Shadow jar
        run: ./gradlew :paper:shadowJar --console=plain

      - name: Upload shadow jar
        uses: actions/upload-artifact@v4
        with:
          name: paper-all
          path: paper/build/libs/*-all.jar
```

- [ ] **Step 5: Grep and fix remaining Gradle project refs**

```bash
rg -n "jobs-api|jobs-core|:jobs-api|:jobs-core" --glob '!**/build/**' --glob '!**/node_modules/**' --glob '!**/.git/**'
```

Fix any `project(":jobs-api")` leftovers. Leave documentation path strings for Task 12 if they do not break the build.

- [ ] **Step 6: Build**

```bash
./gradlew :api:test :paper:test :paper:shadowJar --console=plain
```

Expected: BUILD SUCCESSFUL. Artifact under `paper/build/libs/` (`paper-all.jar` or similar).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: rename Gradle modules to api and paper (stub common)"
```

---

## Task 3: Nest session-api under web

**Files:**
- Rename: `jobs-session-api/` → `web/session-api/`
- Modify: `web/session-api/Cargo.toml` (package name)
- Modify: `web/session-api/README.md`
- Modify: path refs in root docs only if required for CI (full docs Task 12)

- [ ] **Step 1: git mv**

```bash
git mv jobs-session-api web/session-api
```

- [ ] **Step 2: Rename crate in `web/session-api/Cargo.toml`**

```toml
[package]
name = "session-api"
version = "0.1.0"
edition = "2021"
description = "Secure REST API for ModularJobs web editor sessions backed by PostgreSQL"
```

Update any path comments in `web/session-api/README.md` that say `jobs-session-api` or `jobs-core/.../postgres.sql` to `web/session-api` and `paper/src/main/resources/sql/postgres.sql`.

- [ ] **Step 3: Verify Rust build**

```bash
cd web/session-api && cargo test --lib 2>&1 | tail -30
# Integration tests may need DATABASE_URL; lib/unit compile is enough here
cargo check
cd ../..
```

Expected: `cargo check` succeeds. Integration tests may skip without Postgres — do not fail the task solely for missing DB.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: nest session-api under web/"
```

---

## Task 4: Populate `common` with editor DTOs

**Files:**
- Create/Modify: `common/build.gradle.kts`
- Create: `common/src/main/java/net/aincraft/common/editor/EditorPayload.java`
- Create: `common/src/main/java/net/aincraft/common/editor/EditorMetadata.java`
- Create: `common/src/main/java/net/aincraft/common/editor/JobData.java`
- Create: `common/src/main/java/net/aincraft/common/editor/TaskData.java`
- Create: `common/src/main/java/net/aincraft/common/editor/PayableData.java`
- Modify: `paper/build.gradle.kts` — `implementation(project(":common"))` if not already via api later
- Modify: paper editor classes to use `common` types (or thin re-export) and delete old `paper/.../editor/json/*` records after move
- Create: `common/src/test/java/net/aincraft/common/editor/EditorPayloadTest.java`
- Modify: `api/build.gradle.kts` — `api(project(":common"))` when api needs common (can wait until peel; paper depends on common now)

- [ ] **Step 1: Finalize `common/build.gradle.kts`**

```kotlin
dependencies {
    api(libs.jetbrains.annotations)
    // Gson annotations only — serialization remains caller's concern (paper already has gson)
    compileOnly(libs.gson)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.gson)
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Write failing test for payload shape**

Create `common/src/test/java/net/aincraft/common/editor/EditorPayloadTest.java`:

```java
package net.aincraft.common.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EditorPayloadTest {

  @Test
  void roundTripCamelCaseJson() {
    EditorMetadata meta =
        EditorMetadata.create("2026-01-01T00:00:00Z", "player-uuid", "token-1", "server");
    EditorPayload payload =
        EditorPayload.create(
            meta,
            Map.of(
                "miner",
                new JobData(
                    "Miner",
                    List.of(
                        new TaskData(
                            "break",
                            "minecraft:stone",
                            List.of(new PayableData("exp", "10")))))),
            List.of("break"),
            List.of("exp"));

    Gson gson = new Gson();
    String json = gson.toJson(payload);
    EditorPayload back = gson.fromJson(json, EditorPayload.class);

    assertEquals(1, back.version());
    assertEquals("token-1", back.metadata().sessionToken());
    assertEquals("Miner", back.jobs().get("miner").displayName());
    assertEquals("break", back.jobs().get("miner").tasks().getFirst().actionTypeKey());
  }
}
```

- [ ] **Step 3: Run test — expect FAIL (classes missing)**

```bash
./gradlew :common:test --tests net.aincraft.common.editor.EditorPayloadTest --console=plain
```

Expected: compile failure or test failure until DTOs exist.

- [ ] **Step 4: Implement DTOs in `common`**

Copy field layout from `paper/src/main/java/net/aincraft/editor/json/*` into `net.aincraft.common.editor`, **same JSON property names** (`@SerializedName`). Example root:

```java
package net.aincraft.common.editor;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public record EditorPayload(
    @SerializedName("version") int version,
    @SerializedName("metadata") @NotNull EditorMetadata metadata,
    @SerializedName("jobs") @NotNull Map<String, JobData> jobs,
    @SerializedName("registeredActionTypes") @NotNull List<String> registeredActionTypes,
    @SerializedName("registeredPayableTypes") @NotNull List<String> registeredPayableTypes) {

  public static final int CURRENT_VERSION = 1;

  public static EditorPayload create(
      @NotNull EditorMetadata metadata,
      @NotNull Map<String, JobData> jobs,
      @NotNull List<String> registeredActionTypes,
      @NotNull List<String> registeredPayableTypes) {
    return new EditorPayload(
        CURRENT_VERSION, metadata, jobs, registeredActionTypes, registeredPayableTypes);
  }
}
```

Mirror `EditorMetadata`, `JobData`, `TaskData`, `PayableData` the same way (read current paper files for exact fields).

- [ ] **Step 5: Point paper editor at common**

In `paper/build.gradle.kts` add:

```kotlin
implementation(project(":common"))
```

Update `EditorService` and any imports from `net.aincraft.editor.json.*` → `net.aincraft.common.editor.*`.

Delete obsolete records under `paper/src/main/java/net/aincraft/editor/json/` **only after** all references updated. Keep `GsonProvider` in paper if it is paper-specific.

- [ ] **Step 6: Run tests**

```bash
./gradlew :common:test :paper:test --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(common): add pure editor payload DTOs and wire paper export"
```

---

## Task 5: Pure `Bridge` holder + drop `Plugin` from api

**Files:**
- Modify: `api/src/main/java/net/aincraft/Bridge.java`
- Modify: `paper/src/main/java/net/aincraft/BridgeImpl.java`
- Modify: `paper/src/main/java/net/aincraft/ModularJobsBootstrap.java`
- Modify: all call sites using `Bridge.bridge().plugin()` → paper-local plugin reference
- Modify: `paper/src/test/java/net/aincraft/BootstrapLifecycleTest.java` if it asserts ServicesManager load text

**Design:**

```java
public interface Bridge {
  static Bridge bridge() {
    Bridge b = Holder.INSTANCE;
    if (b == null) {
      throw new IllegalStateException("Bridge not registered (plugin not enabled)");
    }
    return b;
  }

  /** Paper-only registration — called from ModularJobsBootstrap. */
  static void register(Bridge bridge) {
    Holder.INSTANCE = bridge;
  }

  static void unregister() {
    Holder.INSTANCE = null;
  }

  // NO plugin() method

  RegistryContainer registryContainer();
  // ... remaining pure service accessors unchanged ...
}

final class Holder {
  static volatile Bridge INSTANCE;
  private Holder() {}
}
```

Put `Holder` as a private static nested class inside `Bridge` to avoid a public type.

- [ ] **Step 1: Find all `plugin()` call sites**

```bash
rg -n "Bridge\.bridge\(\)\.plugin\(\)|\.plugin\(\)" api paper/src --type java
```

- [ ] **Step 2: Rewrite `Bridge` as pure static holder (remove Bukkit imports and `plugin()`)**

Implement the design above in `api/.../Bridge.java`.

- [ ] **Step 3: Register/unregister from bootstrap**

In `ModularJobsBootstrap` enable path, after creating context:

```java
Bridge.register(created.bridge);
// keep Bukkit ServicesManager.register for third-party discovery of Bridge IF desired;
// pure Bridge.bridge() no longer reads ServicesManager
```

On disable:

```java
Bridge.unregister();
Bukkit.getServicesManager().unregisterAll(this);
```

- [ ] **Step 4: Replace scheduler call sites**

Anywhere that used `Bridge.bridge().plugin()` for `runTask`, inject/`ModularJobsBootstrap` plugin instance or store `JavaPlugin` on a paper-only holder, e.g.:

```java
// paper-only
public final class PluginProvider {
  private static JavaPlugin plugin;
  public static void set(JavaPlugin p) { plugin = p; }
  public static JavaPlugin get() { return plugin; }
}
```

Set from bootstrap; use `PluginProvider.get()` in paper listeners/commands.

- [ ] **Step 5: Fix `ActionTypes` NamespacedKey usage (temporary still-Bukkit)**

Still in api until Task 7–8: if `ActionTypes` uses `Bridge.bridge().plugin()` for `NamespacedKey`, switch to Adventure `Key.key("modularjobs", keyString)` / registry lookup by `Key` so `plugin()` is unnecessary:

```java
return registry.getOrThrow(Key.key("modularjobs", keyString));
```

(Adjust namespace string to match existing registry keys — inspect `RegistryKeys` / how action types are registered.)

- [ ] **Step 6: Compile and test**

```bash
./gradlew :api:test :paper:test --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(api): pure Bridge holder without Bukkit Plugin"
```

---

## Task 6: UUID player surface on services and views

**Files (representative — update all implementors/call sites):**
- Modify: `api/.../JobProgressionView.java` — `UUID playerId()` instead of `OfflinePlayer player()`
- Modify: `api/.../service/JobService.java` — methods take `UUID`
- Modify: `api/.../service/PreferencesService.java` — methods take `UUID`
- Modify: `api/.../container/EconomyProvider.java` — `deposit(UUID, …)`
- Modify: `api/.../container/PayableHandler.java` — `PayableContext` uses `UUID`
- Modify: `api/.../container/ExperiencePayableHandler.java` — bar context uses `UUID`
- Modify: `api/.../container/BoostContext.java` — `UUID playerId`, `String worldName` (no `Player`/`World`)
- Modify: `api/.../container/boost/TimedBoostDataService.java` — UUID
- Modify: all `paper` implementors + tests

- [ ] **Step 1: Change API signatures (batch)**

Example:

```java
// JobProgressionView
import java.util.UUID;

public interface JobProgressionView {
  BigDecimal experienceForLevel(int level);
  Job job();
  UUID playerId();
  BigDecimal experience();
  int level();
}
```

```java
// JobService excerpts
List<JobProgression> getProgressions(UUID playerId);
List<JobProgression> getArchivedProgressions(UUID playerId);
// apply same UUID pattern to every OfflinePlayer/Player parameter in this interface
```

```java
// BoostContext
public record BoostContext(
    ActionType type, JobProgressionView progression, UUID playerId, String worldName, Payable payable) {}
```

- [ ] **Step 2: Fix paper compile errors**

```bash
./gradlew :paper:compileJava --console=plain 2>&1 | head -80
```

At each error: use `player.getUniqueId()` when calling api; use `Bukkit.getOfflinePlayer(uuid)` / `Bukkit.getPlayer(uuid)` only inside paper when Bukkit API is required (Vault deposit, etc.).

- [ ] **Step 3: Test**

```bash
./gradlew :api:test :paper:test --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(api): replace Player/OfflinePlayer with UUID on public services"
```

---

## Task 7: Pure `Context` + paper mappers

**Files:**
- Modify: `api/.../container/Context.java` — pure sealed variants
- Create: `paper/.../paper/BukkitContexts.java` — factories from Bukkit types
- Modify: every paper site that constructs `new Context.BlockContext(block)` etc.

**Target pure shape:**

```java
package net.aincraft.container;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface Context
    permits Context.BlockContext,
        Context.ChunkContext,
        Context.DyeContext,
        Context.EnchantmentContext,
        Context.EntityContext,
        Context.ItemContext,
        Context.MaterialContext,
        Context.PotionContext {

  /** Block at location; materialKey like "minecraft:stone". */
  record BlockContext(String worldName, int x, int y, int z, String materialKey) implements Context {}

  /** Item material key (+ optional amount). */
  record ItemContext(String materialKey, int amount) implements Context {}

  @Deprecated
  record MaterialContext(String materialKey) implements Context {}

  /** Entity type key like "minecraft:zombie". */
  record EntityContext(String entityTypeKey) implements Context {}

  record DyeContext(String dyeColorName) implements Context {}

  record EnchantmentContext(String enchantmentKey, int level) implements Context {}

  record PotionContext(String potionTypeKey) implements Context {}

  record ChunkContext(String worldName, int chunkX, int chunkZ) implements Context {}
}
```

Paper mapper sketch:

```java
package net.aincraft.paper;

import net.aincraft.container.Context;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public final class BukkitContexts {
  private BukkitContexts() {}

  public static Context.BlockContext block(Block block) {
    return new Context.BlockContext(
        block.getWorld().getName(),
        block.getX(),
        block.getY(),
        block.getZ(),
        block.getType().getKey().toString());
  }

  public static Context.ItemContext item(ItemStack stack) {
    return new Context.ItemContext(stack.getType().getKey().toString(), stack.getAmount());
  }
  // entity, chunk, dye, enchant, potion similarly
}
```

- [ ] **Step 1: Rewrite `Context` in api (pure)**

- [ ] **Step 2: Add `BukkitContexts` and update paper construction sites**

```bash
rg -n "new Context\.|BlockContext\(|ItemContext\(|EntityContext\(|MaterialContext\(" paper/src --type java
```

- [ ] **Step 3: Compile/test**

```bash
./gradlew :api:compileJava :paper:test --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(api): pure Context types with paper Bukkit mappers"
```

---

## Task 8: Pure conditions, factories, resources, action keys

**Files:**
- Modify: `api/.../container/boost/Condition.java` — factory params as `String` keys/names
- Modify: `api/.../container/boost/factories/ConditionFactory.java`
- Modify: `api/.../container/boost/PlayerResourceType.java` — remove Bukkit functional extractors; paper evaluates
- Modify: `api/.../container/boost/PotionConditionType.java` — pure enum without `PotionEffect` method refs
- Modify: `api/.../container/ActionTypes.java` / `PayableTypes.java` — use Adventure `Key`, not `NamespacedKey`
- Modify: paper condition implementations to resolve strings → Bukkit types at evaluation time

- [ ] **Step 1: Change factory signatures to strings**

Example:

```java
static Condition biome(String biomeKey) { return factory().biome(biomeKey); }
static Condition world(String worldName) { return factory().world(worldName); }
static Condition liquid(String materialKey) { return factory().liquid(materialKey); }
static Condition potionType(String potionEffectTypeKey) { return factory().potionType(potionEffectTypeKey); }
```

- [ ] **Step 2: Move Bukkit evaluation into paper impl of ConditionFactory**

Paper already has `ConditionFactory` impl — parse `Material.matchMaterial`, `Registry.BIOME`, etc. there.

- [ ] **Step 3: Simplify `PlayerResourceType` / `PotionConditionType` to pure enums**

If they currently store `Function<Player, Double>`, replace with enum constants only; paper switch-evaluates HEALTH/FOOD/etc.

- [ ] **Step 4: Replace `NamespacedKey` in ActionTypes/PayableTypes with `Key.key(...)`**

- [ ] **Step 5: Test**

```bash
./gradlew :api:test :paper:test --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(api): pure boost conditions and Adventure keys"
```

---

## Task 9: Pure upgrade icons (Material → string)

**Files:**
- Modify: `api/.../upgrade/UpgradeNode.java` — `String iconMaterial` / `unlockedIconMaterial` (or keep fields named `icon` as String)
- Modify: `api/.../upgrade/ConnectorNode.java`
- Modify: `api/.../upgrade/wynncraft/IconConfig.java` — drop `toMaterial()`; expose `id()` only
- Modify: paper GUI code to `Material.matchMaterial(icon)` when rendering

- [ ] **Step 1: Change node icon fields to String material names**

```java
// UpgradeNode record fields (illustrative)
@NotNull String icon,
@NotNull String unlockedIcon,
```

- [ ] **Step 2: Update paper GUI / loader to parse Material**

```java
Material icon = Material.matchMaterial(node.icon());
if (icon == null) icon = Material.BARRIER;
```

- [ ] **Step 3: Test**

```bash
./gradlew :paper:test --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(api): upgrade node icons as material name strings"
```

---

## Task 10: Pure domain events + paper dual-fire

**Files:**
- Create: `api/.../event/EventBus.java`
- Create: `api/.../event/Cancellable.java` (pure interface with `boolean cancelled()` / `void setCancelled`)
- Rewrite: `api/.../event/*Event.java` — plain classes/records with `UUID playerId`, no Bukkit `Event`
- Delete or gut: `api/.../event/AbstractEvent.java` (Bukkit HandlerList)
- Create: `paper/.../paper/event/PaperEventBridge.java` (or similar) to map pure → Bukkit and `callEvent`
- Create: `paper/.../paper/event/BukkitJobLevelEvent.java` etc. **or** keep Bukkit event class names under `net.aincraft.paper.event` for external listeners
- Modify: paper fire sites (`JobServiceImpl`, `LevelCommand`, payment handlers, …) to publish via `EventBus` + dual-fire
- Modify: paper `@EventHandler` listeners that listened to old `net.aincraft.event.*` Bukkit events — either listen to new paper wrappers or call pure handlers directly

**Pure event example:**

```java
package net.aincraft.event;

import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.container.Payable;

public final class JobsPaymentEvent implements Cancellable {
  private final UUID playerId;
  private final Payable base;
  private boolean cancelled;

  public JobsPaymentEvent(UUID playerId, Payable base) {
    this.playerId = playerId;
    this.base = base;
  }

  public UUID playerId() { return playerId; }
  public Payable base() { return base; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
```

**EventBus:**

```java
package net.aincraft.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class EventBus {
  private final List<Consumer<Object>> listeners = new ArrayList<>();

  public void subscribe(Consumer<Object> listener) {
    listeners.add(Objects.requireNonNull(listener));
  }

  public <T> T publish(T event) {
    for (Consumer<Object> listener : List.copyOf(listeners)) {
      listener.accept(event);
    }
    return event;
  }
}
```

Expose bus from `Bridge` **or** paper singleton; prefer `Bridge` accessor only if pure — e.g. `EventBus eventBus()` on Bridge implemented by paper.

**Dual-fire pattern in paper:**

```java
JobsPaymentEvent pure = new JobsPaymentEvent(player.getUniqueId(), payable);
bridge.eventBus().publish(pure);
if (!pure.isCancelled()) {
  // optional Bukkit wrapper for third-party plugins depending on Bukkit bus
  Bukkit.getPluginManager().callEvent(new net.aincraft.paper.event.JobsPaymentBukkitEvent(player, payable, pure));
}
```

Propagate cancel from Bukkit wrapper back to pure if needed for parity.

- [ ] **Step 1: Introduce pure events + EventBus in api**

- [ ] **Step 2: Wire paper publishers and internal listeners**

Internal paper listeners that used `@EventHandler` on old events should either:
- subscribe on `EventBus`, or
- keep `@EventHandler` on **paper** Bukkit wrapper types only.

- [ ] **Step 3: Test**

```bash
./gradlew :api:test :paper:test --console=plain
```

Expected: BUILD SUCCESSFUL. Add a small unit test in api:

```java
@Test
void eventBusDelivers() {
  EventBus bus = new EventBus();
  AtomicReference<Object> got = new AtomicReference<>();
  bus.subscribe(got::set);
  JobsPaymentEvent e = new JobsPaymentEvent(UUID.randomUUID(), /* mock or simple payable */ null);
  // use a minimal payable test double if Payable is an interface
  bus.publish(e);
  assertSame(e, got.get());
}
```

(Adjust if `Payable` cannot be null — use a test stub.)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(api): pure domain events with paper dual-fire bridge"
```

---

## Task 11: Remove Paper from `api` build + isolation guard

**Files:**
- Modify: `api/build.gradle.kts` — remove paper/mockbukkit/mcmmo; add adventure + common
- Modify: `gradle/libs.versions.toml` — add adventure library entries if missing
- Create: `api/src/test/java/net/aincraft/ArchitectureIsolationTest.java` (or script in CI)
- Modify: `paper/build.gradle.kts` — ensure paper still has everything it needs

- [ ] **Step 1: Add Adventure to version catalog**

In `gradle/libs.versions.toml`:

```toml
# under [versions]
adventure = "4.21.0"

# under [libraries]
adventure-api = { module = "net.kyori:adventure-api", version.ref = "adventure" }
```

(If compile fails on API mismatch with Paper's bundled Adventure, align version with Paper 26.2's Adventure — check paper javadoc/deps — and set that version.)

- [ ] **Step 2: Rewrite `api/build.gradle.kts`**

```kotlin
dependencies {
    api(project(":common"))
    api(libs.adventure.api)
    api(libs.jetbrains.annotations)

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

No `libs.paper.api`, no MockBukkit, no mcmmo.

- [ ] **Step 3: Isolation test**

```java
package net.aincraft;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ArchitectureIsolationTest {

  @Test
  void apiAndCommonSourcesMustNotImportBukkitOrPaper() throws IOException {
    List<Path> roots = List.of(
        Path.of("src/main/java"),
        // common is sibling — also scan from repo via relative path when test runs from api project
        Path.of("../common/src/main/java"));
    List<String> offenders = new ArrayList<>();
    for (Path root : roots) {
      if (!Files.isDirectory(root)) continue;
      try (Stream<Path> walk = Files.walk(root)) {
        walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
          try {
            String text = Files.readString(p);
            if (text.contains("import org.bukkit")
                || text.contains("import io.papermc")
                || text.contains("import org.spigotmc")) {
              offenders.add(p.toString());
            }
          } catch (IOException e) {
            fail(e);
          }
        });
      }
    }
    assertTrue(offenders.isEmpty(), "Bukkit/Paper imports in pure modules: " + offenders);
  }
}
```

- [ ] **Step 4: Grep enforcement**

```bash
rg -n "import org\.bukkit|import io\.papermc|import org\.spigotmc" api/src common/src --type java
```

Expected: **no matches**. Fix any remaining.

- [ ] **Step 5: Full Gradle verify**

```bash
./gradlew :common:test :api:test :paper:test :paper:shadowJar --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "build(api): drop Paper dependency; enforce pure-module isolation"
```

---

## Task 12: Docs, scripts, CHANGELOG

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `.claude/CLAUDE.md` (module table / paths)
- Modify: `docs/database-schema.md`
- Modify: `scripts/apply-postgres-schema.sh`
- Modify: `CHANGELOG.md`
- Modify: `web/session-editor/README.md`
- Modify: `web/session-api/README.md`
- Modify: `paper/src/main/resources/database.yml` comments if they reference old paths

- [ ] **Step 1: Update module tables**

README / AGENTS module table:

| Path | Role |
|------|------|
| `api` | Pure public contracts (no Paper) |
| `common` | Shared DTOs (editor payload, …) |
| `paper` | Paper plugin implementation (shadow jar) |
| `web` | Docs + session-editor + session-api |

Build commands:

```bash
./gradlew :paper:build
# artifact: paper/build/libs/paper-all.jar (or *-all.jar)

./gradlew :api:test :common:test :paper:test

cd web/session-api && cargo test
cd web/session-editor && npm test && npm run build
```

Schema:

```bash
psql "$DATABASE_URL" -f paper/src/main/resources/sql/postgres.sql
# or ./scripts/apply-postgres-schema.sh
```

- [ ] **Step 2: Fix `scripts/apply-postgres-schema.sh`**

```bash
SCHEMA="${ROOT}/paper/src/main/resources/sql/postgres.sql"
```

- [ ] **Step 3: CHANGELOG entry**

```markdown
## Unreleased

### Breaking
- Module paths renamed: `jobs-api`→`api`, `jobs-core`→`paper`, `jobs-web`→`web`; session API at `web/session-api`.
- Public `api` module no longer depends on Bukkit/Paper. Integrators use UUID/string keys and pure events (`EventBus`). Optional Bukkit event wrappers live under `net.aincraft.paper.event` in the plugin jar.
- `Bridge.plugin()` removed; `Bridge.bridge()` uses in-process registration, not only ServicesManager.
```

- [ ] **Step 4: Final stale-path grep**

```bash
rg -n "jobs-api|jobs-core|jobs-web|jobs-session-api" --glob '!**/build/**' --glob '!**/node_modules/**' --glob '!**/target/**' --glob '!**/.git/**' --glob '!**/docs/superpowers/**'
```

Expected: only historical notes in specs/plans if intentionally kept; fix live docs/scripts/CI.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "docs: update paths for api/common/paper/web layout"
```

---

## Task 13: Full verification gate

**Files:** none (verification only)

- [ ] **Step 1: Java**

```bash
./gradlew clean :common:test :api:test :paper:test :paper:shadowJar --console=plain
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Isolation grep**

```bash
rg -n "import org\.bukkit|import io\.papermc" api/src common/src --type java; test $? -eq 1
```

Expected: no matches (rg exit 1).

- [ ] **Step 3: Rust**

```bash
cargo check --manifest-path web/session-api/Cargo.toml
```

Expected: success.

- [ ] **Step 4: Session editor (if node_modules available)**

```bash
cd web/session-editor && npm test && cd ../..
```

Expected: tests pass (install deps first if needed: `npm ci`).

- [ ] **Step 5: Success criteria checklist**

- [ ] Gradle includes `:api`, `:common`, `:paper` only
- [ ] `web/session-api` exists; no top-level `jobs-session-api`
- [ ] Zero Bukkit/Paper imports in `api` / `common`
- [ ] Only `paper` declares Paper API
- [ ] Shadow jar builds
- [ ] Docs/scripts/CI match new paths
- [ ] Work performed on `refactor/module-layout` worktree with prior WIP included

- [ ] **Step 6: Final commit only if verification fixes were needed**

```bash
git status -sb
# if fixes:
git add -A && git commit -m "fix: verification follow-ups for module layout"
```

---

## Spec coverage (self-review)

| Spec requirement | Task(s) |
|------------------|---------|
| Rename to api/common/paper/web | 2, 3, 4 |
| session-api under web | 3 |
| paper-only Paper imports | 7–11 |
| api public, no Bukkit | 5–11 |
| common DTOs | 4 |
| Worktree + WIP | 1 |
| Docs/CI/scripts | 2 (CI), 12 |
| Dual-fire / pure events | 10 |
| Verification | 13 |

## Placeholder / consistency scan

- No TBD steps; open version pin for Adventure called out with alignment instruction.
- Module names consistent: `api`, `common`, `paper`, `web/session-api`.
- UUID / string-key substitutions consistent across Tasks 6–9.
- Event package: pure `net.aincraft.event`, Bukkit wrappers `net.aincraft.paper.event`.

## Risk notes for executors

- **Context + payment path** is the highest churn — budget time in Task 7.
- **WIP on master** may already move interfaces; resolve conflicts during Task 1–2, do not drop WIP.
- Prefer **compile after each peel task**; if Task 6–10 are too red mid-way, combine 6+7 into one commit rather than leaving master broken mid-task.
- Do not reintroduce `CREATE TABLE` or multi-DB support while touching paths.
