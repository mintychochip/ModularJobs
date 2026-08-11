# Unified MapGUI Skill Tree Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the split Craftux skill-tree/player editor surfaces with one optional MapGUI graph/detail/editor opened from `/jobs upgrade`, while preserving the v2 skill-tree service and PostgreSQL state contracts.

**Architecture:** Keep `SkillTree`, `SkillTreeState`, `UpgradeService`, and `SkillTreeConfigParser` as the domain authority. Add a Paper-only mutable v2 JSON document plus validated atomic save path. Put MapGUI imports behind the same reflective runtime bridge used by `../modular-territories`; ordinary players see purchase/detail controls, and `jobs.command.admin.treeeditor` viewers see draft editing controls. Keep Craftux as a viewer/purchase fallback when MapGUI is absent, and retain `/jobs treeeditor` only as a compatibility route to the same opener.

**Tech Stack:** Java 25 Paper 26.2; MapGUI API 1.0.0 (`io.github.flog99:mapgui-api`); Gson; JUnit 5; MockBukkit; Craftux fallback; PostgreSQL-backed `UpgradeService` state.

## Global Constraints

- MapGUI is `compileOnly`, never shaded into the ModularJobs jar.
- Runtime MapGUI access is optional and must fail closed to Craftux without taking down ModularJobs.
- v2 `SkillTree` JSON is the only authoring format after a successful save.
- Legacy/Wynncraft input remains loadable; legacy migration must not invent unsupported fields.
- Player `node_levels` state is never rewritten by editor saves.
- Save validates before disk replacement and registers the new tree only after replacement succeeds.
- PostgreSQL remains connect-only; no schema or boot-time DDL changes.
- Keep each production behavior change and its focused tests in one atomic commit.
- Skip formatters, linters, and project-wide test suites during individual implementation tasks; run them once at the end.

---

## Task 1: Add the optional MapGUI dependency boundary

**Files:**
- Modify: `paper/build.gradle.kts`
- Create or modify: `paper/src/main/resources/paper-plugin.yml`
- Modify: `paper/src/main/resources/plugin.yml` only for permission wording if needed
- Create: `paper/src/main/java/net/aincraft/upgrade/map/MapGuiUnavailableException.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/map/SkillTreeMapScreenBridge.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/map/SkillTreeMapScreenRuntime.java`
- Test: `paper/src/test/java/net/aincraft/upgrade/map/SkillTreeMapScreenBridgeTest.java`

**Interfaces:**
- Produces `SkillTreeMapScreenBridge.open(Player, UpgradeService, UpgradeTreeLoader, Job): boolean` with no MapGUI imports.
- Produces a runtime-only `SkillTreeMapScreenRuntime.open(Player, UpgradeService, UpgradeTreeLoader, Job)` entrypoint for later tasks.

- [ ] **Step 1: Add the failing bridge test**

Create a bridge test that invokes the bridge in the normal unit-test runtime, where MapGUI is not installed, and asserts that it returns `false` rather than throwing a `NoClassDefFoundError`, `ClassNotFoundException`, or `IllegalStateException`.

```java
@Test
void missingMapGuiFailsClosed() {
  boolean opened = SkillTreeMapScreenBridge.open(player, upgradeService, treeLoader, job);
  assertFalse(opened);
}
```

Use mocked or minimal `Player`, `UpgradeService`, `UpgradeTreeLoader`, and `Job` fixtures; do not start a Paper server for this bridge-only contract.

- [ ] **Step 2: Run the focused test and confirm it fails**

Run:

```bash
./gradlew :paper:test --tests '*SkillTreeMapScreenBridgeTest'
```

Expected: compilation/test failure because the bridge class does not exist.

- [ ] **Step 3: Add the compile-only dependency and optional descriptor**

Add the exact dependency used by `../modular-territories`:

```kotlin
compileOnly("io.github.flog99:mapgui-api:1.0.0")
```

Add a complete `paper-plugin.yml`; a dependency-only fragment is not valid because
Paper prefers this descriptor over `plugin.yml`. Preserve the current plugin
identity, lifecycle registration, optional integrations, and permission metadata:

```yaml
name: ModularJobs
main: net.aincraft.ModularJobsBootstrap
version: '2.0.0'
authors: [ ModularJobs contributors ]
api-version: '26.2'
load: POSTWORLD
dependencies:
  server:
    MapGUI:
      load: BEFORE
      required: false
      join-classpath: true
    PlaceholderAPI:
      load: OMIT
      required: false
      join-classpath: true
    Mint:
      load: OMIT
      required: false
      join-classpath: true
    mcMMO:
      load: OMIT
      required: false
      join-classpath: true
    Bolt:
      load: OMIT
      required: false
      join-classpath: true
    LWC:
      load: OMIT
      required: false
      join-classpath: true
    Choco:
      load: OMIT
      required: false
      join-classpath: true
permissions:
  modularjobs.admin:
    description: Admin commands for ModularJobs (level, experience, boost, editor, applyedits)
    default: op
  jobs.command.browse:
    description: Browse jobs GUI
    default: true
  jobs.command.list:
    description: List available jobs
    default: true
  jobs.command.stats:
    description: View own job stats
    default: true
  jobs.command.admin.stats:
    description: View other players' job stats
    default: op
  jobs.command.archive:
    description: View own archived job progress
    default: true
  jobs.command.admin.archive:
    description: View other players' archived job progress
    default: op
  jobs.command.leaveall:
    description: Leave all jobs at once
    default: true
  jobs.command.admin.treeeditor:
    description: Edit skill trees from the upgrade screen
    default: op
  jobs.command.admin.boost:
    description: Manage timed and item boosts (alias of modularjobs.admin)
    default: op
    children:
      modularjobs.admin: true
  jobs.command.admin.editor:
    description: Export/import job tasks via web editor (alias of modularjobs.admin)
    default: op
    children:
      modularjobs.admin: true
```

Keep the legacy `plugin.yml` with the same identity/permissions for servers that
still read it, changing only the tree-editor description there. Do not put MapGUI
in `shadowJar`.

- [ ] **Step 4: Implement the no-import bridge and runtime shell**

Implement the bridge using the sibling pattern:

```java
private static final String RUNTIME_CLASS =
    "net.aincraft.upgrade.map.SkillTreeMapScreenRuntime";

public static boolean open(Player player, UpgradeService service,
    UpgradeTreeLoader loader, Job job) {
  if (player == null || service == null || loader == null || job == null) {
    return false;
  }
  try {
    Class<?> runtime = Class.forName(RUNTIME_CLASS,
        true, SkillTreeMapScreenBridge.class.getClassLoader());
    runtime.getMethod("open", Player.class, UpgradeService.class,
        UpgradeTreeLoader.class, Job.class)
        .invoke(null, player, service, loader, job);
    return true;
  } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
    return false;
  } catch (InvocationTargetException e) {
    Throwable cause = e.getCause();
    if (cause instanceof MapGuiUnavailableException
        || cause instanceof LinkageError) {
      return false;
    }
    if (cause instanceof RuntimeException runtime) {
      throw runtime;
    }
    if (cause instanceof Error error) {
      throw error;
    }
    throw new IllegalStateException("MapGUI skill-tree screen failed", cause);
  } catch (LinkageError e) {
    return false;
  }
}
```

`MapGuiUnavailableException` is a project-owned runtime exception with no MapGUI
imports. In `SkillTreeMapScreenRuntime.open`, isolate only the `MapGui.get()`
lookup:

```java
MapGui mapGui;
try {
  mapGui = MapGui.get();
} catch (IllegalStateException unavailable) {
  throw new MapGuiUnavailableException(unavailable);
}
mapGui.open(player, new SkillTreeMapScreen(upgradeService, treeLoader, job));
```

Do not wrap screen construction or `mapGui.open(...)` in that
`IllegalStateException` catch. Screen/editor/purchase defects must propagate to
the caller and remain visible in tests/logs.

The runtime shell is the first class allowed to import `de.flog99.mapgui.*`; it will construct the screen in Task 3.

- [ ] **Step 5: Run the focused bridge test and commit**

Run the focused test again and expect PASS.

```bash
./gradlew :paper:test --tests '*SkillTreeMapScreenBridgeTest'
git add paper/build.gradle.kts paper/src/main/resources/paper-plugin.yml paper/src/test/java/net/aincraft/upgrade/map/SkillTreeMapScreenBridgeTest.java paper/src/main/java/net/aincraft/upgrade/map/SkillTreeMapScreenBridge.java paper/src/main/java/net/aincraft/upgrade/map/SkillTreeMapScreenRuntime.java paper/src/main/java/net/aincraft/upgrade/map/MapGuiUnavailableException.java
git commit -m "feat: add optional MapGUI integration boundary"
```

---

## Task 2: Create the mutable v2 document and validated save path

**Files:**
- Create: `paper/src/main/java/net/aincraft/upgrade/editor/SkillTreeDocument.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/editor/SkillTreeDocumentFactory.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/config/SkillTreeSaveResult.java`
- Modify: `paper/src/main/java/net/aincraft/upgrade/config/UpgradeTreeLoader.java`
- Modify: `paper/src/main/java/net/aincraft/upgrade/config/SkillTreeConfigParser.java` only for validation diagnostics needed by the document
- Test: `paper/src/test/java/net/aincraft/upgrade/editor/SkillTreeDocumentTest.java`
- Test: `paper/src/test/java/net/aincraft/upgrade/config/UpgradeTreeLoaderTest.java`

**Interfaces:**

```java
public final class SkillTreeDocument {
  public static SkillTreeDocument parse(
      String treeId, JsonObject root, SkillTreeConfigParser parser);
  public static SkillTreeDocument fromLegacy(
      String treeId, UpgradeTree legacy, SkillTreeConfigParser parser);
  public SkillTree tree();
  public String treeId();
  public JsonObject json();
  public SkillTreeDocument copy();
  public void setNodePosition(String nodeKey, Position position);
  public void addNode(String nodeKey, JsonObject node);
  public void removeNode(String nodeKey);
  public void addPrerequisite(String nodeKey, String prerequisite);
  public void removePrerequisite(String nodeKey, String prerequisite);
  public List<String> validate();
}
```

```java
public sealed interface SkillTreeSaveResult
    permits SkillTreeSaveResult.Saved, SkillTreeSaveResult.Invalid,
            SkillTreeSaveResult.Failed {
  record Saved(SkillTree tree) implements SkillTreeSaveResult {}
  record Invalid(List<String> messages) implements SkillTreeSaveResult {}
  record Failed(String message, Throwable cause) implements SkillTreeSaveResult {}
}
```

- [ ] **Step 1: Write document behavior tests first**

Cover observable contracts:

```java
@Test
void parseCopyAndMutationPreserveV2Fields() {
  SkillTreeDocument original = SkillTreeDocument.parse("miner", minerJson(), parser);
  SkillTreeDocument copy = original.copy();

  copy.setNodePosition("far_gather", new Position(8, 4));
  copy.addPrerequisite("allay_branch", "far_gather");

  assertEquals(new Position(4, 0), original.tree().node("far_gather").orElseThrow().position());
  assertEquals(new Position(8, 4), copy.tree().node("far_gather").orElseThrow().position());
  assertTrue(copy.tree().node("allay_branch").orElseThrow()
      .prerequisites().contains("far_gather"));
  assertEquals("all", copy.json().getAsJsonObject("nodes")
      .getAsJsonObject("allay_branch").getAsJsonObject("requirements")
      .get("type").getAsString());
}

@Test
void invalidReferenceIsRejectedWithoutChangingTheDocument() {
  SkillTreeDocument document = SkillTreeDocument.parse("miner", minerJson(), parser);
  JsonObject invalid = document.json().deepCopy();
  invalid.getAsJsonObject("nodes").getAsJsonObject("far_gather")
      .getAsJsonArray("prerequisites").add("missing");

  assertThrows(IllegalArgumentException.class,
      () -> SkillTreeDocument.parse("miner", invalid, parser));
  assertTrue(document.validate().isEmpty());
}
```

Add a migration test asserting legacy `maxedPrerequisites` become v2
`node_level` requirements and that no unsupported legacy fields are invented in
the migrated JSON.

- [ ] **Step 2: Run the focused document tests and confirm failure**

```bash
./gradlew :paper:test --tests '*SkillTreeDocumentTest' --tests '*UpgradeTreeLoaderTest'
```

Expected: compilation failure because the document/result types do not exist.

- [ ] **Step 3: Implement v2 JSON document mutation**

Deep-copy the root `JsonObject` on construction. Keep the parsed `SkillTree` as a refreshed view. Every structural mutation updates only the JSON document, then reparses through `SkillTreeConfigParser`; reject mutation when it would leave the graph impossible to display and return the parser message to the caller.

`removeNode` must remove the node and references to it from every `prerequisites` and `excludes` array, but must not rewrite persisted player state. `addPrerequisite` must reject self-links and duplicate links. `setNodePosition` must write `{ "x": ..., "y": ... }`.

- [ ] **Step 4: Refactor legacy conversion into the document factory**

Extract the existing `UpgradeTreeLoader.convertLegacy` grouping/mapping logic into `SkillTreeDocumentFactory.fromLegacy`. Preserve these exact mappings:

- legacy perk groups → one v2 node with ordered `levels`;
- legacy prerequisites → v2 `prerequisites`;
- legacy exclusives → v2 `excludes`;
- legacy maxed prerequisites → `Requirements.NodeLevelRequirement` JSON;
- legacy positions → v2 `position`;
- legacy effects → supported v2 effects.

Do not synthesize unsupported fields. Make `convertLegacy` register `factory.fromLegacy(...).tree()` so runtime and editor migration use one implementation.

- [ ] **Step 5: Implement typed validated atomic save**

Add `UpgradeTreeLoader.loadSkillTreeDocument(String treeId, String jobKey)` and:

```java
public SkillTreeSaveResult saveSkillTree(
    String treeId, JsonObject candidate) {
  // parse and validate before touching disk
  // write sibling temp file
  // move temp over target
  // register parsed tree after successful move
}
```

Use `Files.createTempFile(target.getParentFile().toPath(), treeId + ".", ".json")`, write via Gson, and move with `StandardCopyOption.ATOMIC_MOVE` plus `REPLACE_EXISTING`; retry with `REPLACE_EXISTING` if the filesystem rejects atomic moves. Delete the temp file on all failure paths. The old `saveTree(String, String)` remains only for legacy callers until Task 5 removes them from the editor path.

- [ ] **Step 6: Run focused tests and commit**

```bash
./gradlew :paper:test --tests '*SkillTreeDocumentTest' --tests '*UpgradeTreeLoaderTest'
git add paper/src/main/java/net/aincraft/upgrade/editor/SkillTreeDocument.java paper/src/main/java/net/aincraft/upgrade/editor/SkillTreeDocumentFactory.java paper/src/main/java/net/aincraft/upgrade/config/SkillTreeSaveResult.java paper/src/main/java/net/aincraft/upgrade/config/UpgradeTreeLoader.java paper/src/test/java/net/aincraft/upgrade/editor/SkillTreeDocumentTest.java paper/src/test/java/net/aincraft/upgrade/config/UpgradeTreeLoaderTest.java
git commit -m "feat: add validated v2 skill tree documents"
```

---

## Task 3: Add the pure graph projection and MapGUI player screens

**Files:**
- Create: `paper/src/main/java/net/aincraft/upgrade/map/SkillTreeGraphModel.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/map/SkillTreeMapScreen.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/map/SkillNodeDetailScreen.java`
- Modify: `paper/src/main/java/net/aincraft/upgrade/map/SkillTreeMapScreenRuntime.java`
- Test: `paper/src/test/java/net/aincraft/upgrade/map/SkillTreeGraphModelTest.java`

**Interfaces:**

```java
public record SkillTreeGraphModel(
    List<GraphNode> nodes,
    List<GraphEdge> edges,
    int originX,
    int originY
) {
  public enum Status { LOCKED, AVAILABLE, OWNED, MAXED, EXCLUDED }
  public record GraphNode(String key, Position position, Status status, int level) {}
  public record GraphEdge(String prerequisite, String child) {}

  public static SkillTreeGraphModel build(
      SkillTree tree, SkillTreeState state, int panX, int panY);
}
```

`SkillTreeMapScreen` constructor:

```java
public SkillTreeMapScreen(
    UpgradeService upgradeService,
    UpgradeTreeLoader treeLoader,
    Job job)
```

- [ ] **Step 1: Write graph projection tests**

Test edge direction, centering, status precedence, and pan offset:

```java
@Test
void edgesPointFromPrerequisiteToChild() {
  SkillTreeGraphModel model = SkillTreeGraphModel.build(tree, state, 0, 0);
  assertTrue(model.edges().contains(new GraphEdge("mining_basics", "far_gather")));
}

@Test
void availableBeatsLockedWhenRequirementsAndPrerequisitesPass() {
  SkillTreeGraphModel model = SkillTreeGraphModel.build(treeWithAvailableNode(), state, 0, 0);
  assertEquals(Status.AVAILABLE, model.nodes().stream()
      .filter(node -> node.key().equals("far_gather"))
      .findFirst().orElseThrow().status());
}
```

- [ ] **Step 2: Run the graph tests and confirm failure**

```bash
./gradlew :paper:test --tests '*SkillTreeGraphModelTest'
```

Expected: compilation failure because the graph model does not exist.

- [ ] **Step 3: Implement the graph projection**

Derive nodes from `SkillTree.nodes()`, read `SkillTreeState.levelOf`, and use `tree.canPurchase(state, key)` for availability. Use `tree.symmetricExcludes(key)` to classify exclusions. Compute bounds from all non-null positions and center them in the map content rectangle, matching the sibling `ProjectGraphScreen` geometry. Emit one edge for each resolved prerequisite.

- [ ] **Step 4: Implement the MapGUI graph screen**

Use the sibling screen’s API pattern:

```java
@Override
public Click activateOn() { return Click.BOTH; }

@Override
protected Node build() {
  return Overlay(
      Draw(this::paintGraph)
          .tracksCursor(true)
          .caption(this::caption)
          .onClick(this::clickAt)
          .fill(),
      panButton("Left", -1, 0),
      panButton("Right", 1, 0),
      panButton("Up", 0, -1),
      panButton("Down", 0, 1),
      resetButton()
  ).fill();
}
```

Render edges before cells, draw hover outlines, and push `SkillNodeDetailScreen` on node clicks. Load the latest `SkillTreeState` during `build()` so purchase results are immediately visible after invalidation. Render the admin toolbar only after checking `player().hasPermission("jobs.command.admin.treeeditor")`; Task 4 fills its actions.

- [ ] **Step 5: Implement the player detail screen**

Display name/description, owned level/max level, cost, requirement/prerequisite text, exclusions, and active effects. Use `Button` only when a player can buy the next action. Route results through a single method that handles every `PurchaseResult` variant without throwing:

```java
private void apply(PurchaseResult result) {
  switch (result) {
    case PurchaseResult.Success success -> invalidateAndHint("Purchased " + success.node().name());
    case PurchaseResult.InsufficientPoints result -> hint("Need " + result.required() + " SP");
    case PurchaseResult.RequirementsNotMet result -> hint("Requirements not met");
    case PurchaseResult.PrerequisitesNotMet result -> hint("Missing prerequisites");
    case PurchaseResult.ExcludedByChoice result -> hint("Conflicts with a chosen node");
    case PurchaseResult.AlreadyOwned result -> hint("Already owned");
    case PurchaseResult.NodeNotFound result -> hint("Node no longer exists; reopen the tree");
    case PurchaseResult.TreeNotFound result -> hint("Tree no longer exists; reopen the tree");
  }
}
```

Keep major confirmation as a pushed confirmation screen or a two-step button action.

- [ ] **Step 6: Wire the runtime shell and run focused tests**

Make `SkillTreeMapScreenRuntime.open(...)` call:

```java
MapGui.get().open(player, new SkillTreeMapScreen(upgradeService, treeLoader, job));
```

Run:

```bash
./gradlew :paper:test --tests '*SkillTreeGraphModelTest' --tests '*SkillTreeMapScreenBridgeTest'
```

Commit:

```bash
git add paper/src/main/java/net/aincraft/upgrade/map paper/src/test/java/net/aincraft/upgrade/map/SkillTreeGraphModelTest.java
git commit -m "feat: render skill trees through MapGUI"
```

---

## Task 4: Add the permission-gated v2 editor draft and controls

**Files:**
- Modify: `paper/src/main/java/net/aincraft/upgrade/map/SkillTreeMapScreen.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/map/SkillTreeEditorSession.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/map/SkillTreeNodeEditorScreen.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/map/SkillTreeSettingsScreen.java`
- Modify: `paper/src/main/java/net/aincraft/upgrade/editor/SkillTreeDocument.java` only for undo/redo snapshot support
- Test: `paper/src/test/java/net/aincraft/upgrade/map/SkillTreeEditorSessionTest.java`

**Interfaces:**

```java
public final class SkillTreeEditorSession {
  public SkillTreeEditorSession(SkillTreeDocument initial);
  public SkillTreeDocument document();
  public Optional<String> selectedNode();
  public void select(String nodeKey);
  public void snapshot();
  public boolean undo();
  public boolean redo();
  public void discardDraft();
  public void replaceWith(SkillTreeDocument document);
}
```

- [ ] **Step 1: Write editor session tests**

```java
@Test
void undoRestoresThePriorDocumentWithoutChangingTheLoadedTree() {
  SkillTreeDocument document = SkillTreeDocument.parse("miner", minerJson(), parser);
  SkillTreeEditorSession session = new SkillTreeEditorSession(document);

  session.snapshot();
  session.document().setNodePosition("far_gather", new Position(9, 9));

  assertTrue(session.undo());
  assertEquals(new Position(4, 0), session.document().tree()
      .node("far_gather").orElseThrow().position());
}

@Test
void permissionIsCheckedBeforeEveryAdminAction() {
  assertFalse(SkillTreeMapScreen.canEdit(false));
  assertTrue(SkillTreeMapScreen.canEdit(true));
}
```

- [ ] **Step 2: Run the editor tests and confirm failure**

```bash
./gradlew :paper:test --tests '*SkillTreeEditorSessionTest'
```

Expected: compilation failure because the editor session does not exist.

- [ ] **Step 3: Implement draft history and structural controls**

Keep an independent deep-copy undo stack and clear redo on new snapshots. Add MapGUI toolbar buttons only when the current `Player` still has `jobs.command.admin.treeeditor`:

- Add: create a unique `node_<n>` skill node at the next visible empty position.
- Move: select a node, then click an empty graph position.
- Link/unlink: select a source, then click a target; reject self-links and cycles with a status hint.
- Delete: reject root deletion; remove references through `SkillTreeDocument.removeNode`.
- Path mode: toggle path-point insertion/removal.
- Undo/redo: mutate the draft only.
- Save: invoke `UpgradeTreeLoader.saveSkillTree` and replace the session document only on `Saved`.

Use `invalidate()` after every local mutation. Do not call `UpgradeService` from editor actions.

- [ ] **Step 4: Implement node and tree property screens**

Use MapGUI `Field` prompts for text/integer values, `Toggle` for boolean choices, and pushed choice screens for enum/effect kinds. Preserve the current editor’s user-visible capabilities but write v2 JSON fields only:

- node name, description, locked/unlocked icons, item model, kind, position;
- major/skill costs and per-level cost rows;
- prerequisites and excludes;
- supported requirement tree nodes;
- supported node effects;
- tree job key, root key, description, and skill-points-per-level;
- path points.

Each field mutation snapshots first, validates its local value, and leaves the previous value intact on invalid input. Do not expose unsupported legacy-only perk/archetype fields.

- [ ] **Step 5: Implement save result handling**

On Save:

```java
switch (treeLoader.saveSkillTree(document.treeId(), session.document().json())) {
  case SkillTreeSaveResult.Saved saved -> {
    session.replaceWith(SkillTreeDocument.parse(
        document.treeId(), session.document().json(), parser));
    hint("Skill tree saved and reloaded");
    invalidate();
  }
  case SkillTreeSaveResult.Invalid invalid -> hint(invalid.messages().getFirst());
  case SkillTreeSaveResult.Failed failed -> hint(failed.message());
}
```

Recheck permission immediately before saving. Keep the draft open after all failures.

- [ ] **Step 6: Run focused editor tests and commit**

```bash
./gradlew :paper:test --tests '*SkillTreeEditorSessionTest' --tests '*SkillTreeDocumentTest'
git add paper/src/main/java/net/aincraft/upgrade/map paper/src/main/java/net/aincraft/upgrade/editor/SkillTreeDocument.java paper/src/test/java/net/aincraft/upgrade/map/SkillTreeEditorSessionTest.java
git commit -m "feat: add permission-gated MapGUI skill editor"
```

---

## Task 5: Route commands, fallback, and compatibility through one opener

**Files:**
- Modify: `paper/src/main/java/net/aincraft/commands/UpgradesCommand.java`
- Create: `paper/src/main/java/net/aincraft/upgrade/map/UpgradeScreenOpener.java`
- Modify: `paper/src/main/java/net/aincraft/commands/TreeEditorCommand.java`
- Modify: `paper/src/main/java/net/aincraft/PluginContext.java`
- Modify: `paper/src/main/resources/plugin.yml`
- Modify: `paper/src/main/java/net/aincraft/gui/UpgradeTreeGui.java` only for the documented no-MapGUI message
- Delete after references are removed: `paper/src/main/java/net/aincraft/upgrade/editor/TreeEditorGui.java`, `TreeEditorNodeGui.java`, `TreeEditorSettingsGui.java`, `EditorTree.java`, `EditorNode.java`, `EditorSession.java`, `EditorEffect.java`, `TreeEditorExporter.java`
- Modify: `paper/src/main/java/net/aincraft/gui/craftux/CraftuxUiHost.java` to remove unused editor action constants and registrations
- Test: `paper/src/test/java/net/aincraft/commands/AdminCommandPermissionTest.java`

**Interfaces:**

```java
public final class UpgradeScreenOpener {
  public UpgradeScreenOpener(UpgradeService service, UpgradeTreeLoader loader,
      UpgradeTreeGui fallback);
  public boolean open(Player player, Job job);
}
```

- [ ] **Step 1: Add command routing tests**

Test that both command paths invoke the same opener and that a missing MapGUI uses the fallback. Test that the existing edit permission is required for editor-only controls, not for opening/purchasing.

- [ ] **Step 2: Run command tests and confirm failure**

```bash
./gradlew :paper:test --tests '*AdminCommandPermissionTest'
```

Expected: failure until the opener is wired.

- [ ] **Step 3: Implement the shared opener**

The opener resolves a v2 tree (including a converted legacy adapter), calls `SkillTreeMapScreenBridge.open`, and falls back to `UpgradeTreeGui.open` when it returns false. If the player has `jobs.command.admin.treeeditor` and MapGUI is absent, send the explicit installation guidance after opening the fallback.

- [ ] **Step 4: Update both commands and composition root**

`UpgradesCommand` calls `UpgradeScreenOpener.open`. `TreeEditorCommand` remains registered under its old literal only as a compatibility route and calls the same opener; it does not construct or reference any old editor class. Remove old editor object construction, Craftux editor action registrations, and the old editor close listener from `PluginContext`.

Keep `jobs.command.admin.treeeditor` in `plugin.yml` with the description `Edit skill trees from the upgrade screen`.

- [ ] **Step 5: Remove obsolete editor classes and run focused tests**

Confirm no source references remain to deleted editor types, then run:

```bash
./gradlew :paper:test --tests '*AdminCommandPermissionTest' --tests '*SkillTreeEditorSessionTest'
```

Commit:

```bash
git add paper/src/main/java/net/aincraft/commands paper/src/main/java/net/aincraft/PluginContext.java paper/src/main/java/net/aincraft/gui/UpgradeTreeGui.java paper/src/main/java/net/aincraft/gui/craftux/CraftuxUiHost.java paper/src/main/resources/plugin.yml
# Include the explicit deleted editor paths in the same staged change.
git commit -m "refactor: route upgrade commands through one screen"
```

---

## Task 6: Add MapGUI visual checks, operator docs, and release wiring

**Files:**
- Modify: `paper/build.gradle.kts`
- Modify: `docs/living-specs/skill-tree.md`
- Modify: `README.md` if it documents `/jobs treeeditor` or upgrade GUI usage
- Modify: `CHANGELOG.md`
- Create: `paper/src/visualTest/java/net/aincraft/upgrade/map/SkillTreeMapScreenVisualTest.java`
- Modify: `paper/src/test/java/net/aincraft/upgrade/map/SkillTreeMapScreenBridgeTest.java` as needed

- [ ] **Step 1: Add the opt-in visual source set**

Mirror the sibling setup without changing the normal test classpath:

```kotlin
val visualTestSourceSet = sourceSets.create("visualTest")
visualTestSourceSet.compileClasspath += sourceSets.main.get().output
visualTestSourceSet.runtimeClasspath += sourceSets.main.get().output
configurations[visualTestSourceSet.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
dependencies {
  add(visualTestSourceSet.implementationConfigurationName,
      "io.github.flog99:mapgui-api:1.0.0")
  add(visualTestSourceSet.implementationConfigurationName,
      "io.github.flog99:mapgui-layout:1.0.0")
}
tasks.named(visualTestSourceSet.compileJavaTaskName) {
  dependsOn(tasks.named("compileJava"))
}
tasks.register<Test>("visualTest") {
  dependsOn(visualTestSourceSet.classesTaskName)
  testClassesDirs = visualTestSourceSet.output.classesDirs
  classpath = visualTestSourceSet.runtimeClasspath
  useJUnitPlatform()
}
```

The visual test must instantiate the graph screen with a fixed miner fixture and assert that its build path produces a non-null node tree; it must not require a live Paper server.

- [ ] **Step 2: Add MapGUI to `runServer` downloads**

Add the exact release asset used by the sibling project:

```kotlin
url("https://github.com/FloG99/MapGUI/releases/download/v1.0.0/MapGUI-1.0.0.jar")
```

Keep existing plugin downloads intact.

- [ ] **Step 3: Update living spec and operator copy**

Mark the skill-tree catalog as current for the unified MapGUI graph/editor, document the optional dependency and Craftux viewer fallback, and state that `/jobs treeeditor` is only a compatibility route. Remove any stale statement that the old Craftux editor is the canonical authoring surface.

- [ ] **Step 4: Run focused visual/build checks and commit**

```bash
./gradlew :paper:visualTest
./gradlew :paper:build

git add paper/build.gradle.kts docs/living-specs/skill-tree.md README.md CHANGELOG.md paper/src/visualTest
# Include only files actually changed by the documentation scan.
git commit -m "docs: document MapGUI skill tree workflow"
```

---

## Task 7: Full verification and manual server scenario

**Files:**
- No source changes expected; fix only failures discovered in the preceding tasks.
- Artifacts: `paper/build/libs/paper-all.jar`, run-server logs, focused test reports.

- [ ] **Step 1: Run module tests**

```bash
./gradlew :api:test :common:test :paper:test
```

Expected: all selected module tests pass.

- [ ] **Step 2: Run static/build verification**

```bash
./gradlew :paper:check :paper:build
```

Expected: Checkstyle/PMD/SpotBugs and the Paper shadow build pass without MapGUI being shaded into the jar.

- [ ] **Step 3: Inspect the built artifact**

```bash
jar tf paper/build/libs/paper-all.jar | awk '$0 ~ /^net\/aincraft\/upgrade\/map\// { print }'
if jar tf paper/build/libs/paper-all.jar | awk '$0 ~ /^de\/flog99\/mapgui\// { found=1 } END { exit found ? 0 : 1 }'; then
  echo "MapGUI classes are shaded into the ModularJobs jar" >&2
  exit 1
fi
```

Expected: the first command lists ModularJobs map classes; the second command
exits successfully only when no `de/flog99/mapgui/` entry is present.

- [ ] **Step 4: Run the server scenario with MapGUI**

```bash
./gradlew :paper:runServer
```

Exercise as a normal player:

1. `/jobs upgrade miner` opens the MapGUI graph.
2. Hover/click a node and purchase a skill level.
3. Open a major and confirm/cancel purchase.

Exercise as an operator with `jobs.command.admin.treeeditor`:

1. Open `/jobs upgrade miner` and confirm editor controls are present.
2. Add, move, link, unlink, and delete a draft node.
3. Undo and redo.
4. Save, close, reopen, and confirm the graph uses the new JSON immediately.
5. Restart and confirm the v2 file and player `node_levels` load intact.
6. Run `/jobs treeeditor miner` and confirm it opens the same screen.

- [ ] **Step 5: Verify the no-MapGUI fallback**

Stop the server, remove the MapGUI jar, and run ModularJobs alone. Confirm:

- plugin startup succeeds;
- `/jobs upgrade miner` opens Craftux viewer/purchase UI;
- an admin sees the MapGUI installation guidance;
- no `NoClassDefFoundError` appears in the log.

- [ ] **Step 6: Commit only fixes discovered by verification**

If verification finds a defect, add a focused regression test with the fix and commit it as a separate atomic `fix:` commit. Do not fold unrelated cleanup into this step.

---

## Completion checklist

- [ ] One unified MapGUI graph/detail/editor is reachable from `/jobs upgrade`.
- [ ] `/jobs treeeditor` only delegates to that same screen.
- [ ] Normal players cannot see or execute editor controls.
- [ ] `jobs.command.admin.treeeditor` controls editing.
- [ ] v2 JSON is canonical after save; legacy input remains loadable and migrates without invented fields.
- [ ] Save validation and immediate reload are atomic and preserve player state.
- [ ] MapGUI is optional, not shaded, and fallback behavior is verified.
- [ ] Player purchases, major confirmation, editor actions, and no-MapGUI behavior are tested.
- [ ] Documentation and living-spec checkboxes match the shipped behavior.
- [ ] Full module tests, quality checks, build, visual checks, and manual server scenarios have current passing evidence.
