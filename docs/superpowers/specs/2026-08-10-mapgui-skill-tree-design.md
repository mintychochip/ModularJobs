# Unified MapGUI skill-tree editor

> Status: approved design
> Date: 2026-08-10
> Scope: ModularJobs Paper skill-tree UI and editor

## 1. Goal

Replace the separate Craftux player skill-tree screen and admin tree-editor command
with one MapGUI graph/detail session opened by `/jobs upgrade <job>`.

The screen is mode-aware, not role-separated:

- ordinary players inspect nodes and purchase available skill levels or majors;
- players with `jobs.command.admin.treeeditor` additionally see structural and
  content-editing controls;
- the old `/jobs treeeditor` entrypoint remains only as a compatibility route to the same unified screen; it opens no separate editor UI.

The v2 `SkillTree` model, `UpgradeService`, and PostgreSQL player state remain the
runtime authority. MapGUI is a presentation and interaction layer only.

## 2. Decisions

- MapGUI is an optional runtime dependency, matching the integration in
  `../modular-territories`.
- MapGUI is compiled with `compileOnly("io.github.flog99:mapgui-api:1.0.0")`.
- The server run task downloads `MapGUI-1.0.0.jar` for manual verification.
- When MapGUI is absent, `/jobs upgrade` falls back to the existing Craftux
  viewer/purchase surface. Admins receive an explicit message that MapGUI is
  required for editing.
- `jobs.command.admin.treeeditor` is retained as the edit capability and its
  permission description is updated. It gates editing from both upgrade
  entrypoints.
- v2 `SkillTree` JSON is the canonical editable format.
- Legacy and Wynncraft trees remain loadable for compatibility. Opening one for
  editing converts it to a v2 document; saving permanently writes v2 JSON.
- Save validates and applies immediately. Invalid content never reaches disk or
  the active registry.
- Player `node_levels` state is not rewritten by editor saves. Existing keys are
  preserved; removed keys become inert/diagnostic rather than being reassigned.

## 3. Current system and replacement boundary

Current ModularJobs has two UI paths:

- `UpgradesCommand` opens `UpgradeTreeGui`, a Craftux inventory surface that
  already supports v2 purchase state and legacy fallback.
- `TreeEditorCommand` opens `TreeEditorGui`, a separate Craftux editor backed by
  the legacy mutable `EditorTree`/`EditorNode` model and Wynncraft export.

The replacement keeps the domain and persistence paths but removes the split UI:

- `UpgradesCommand` becomes the single entrypoint.
- `TreeEditorCommand` becomes a compatibility entrypoint that delegates to the
  same unified screen; it owns no separate UI.
- `UpgradeTreeGui` remains a no-MapGUI viewer/purchase fallback.
- The full editor is implemented only in the unified MapGUI session.
- Existing legacy editor classes are not used to write v2 data.

## 4. MapGUI components

Use the optional-classpath boundary from `../modular-territories`:

### 4.1 Dependency and runtime bridge

Add `io.github.flog99:mapgui-api:1.0.0` as `compileOnly` in `paper` and declare
MapGUI as an optional `BEFORE` server dependency with `join-classpath: true` in
Paper plugin metadata. Keep MapGUI out of the shadow jar.

Add a no-MapGUI bridge with no MapGUI imports:

- `SkillTreeMapScreenBridge.open(...)` reflectively loads the runtime entrypoint;
- absent plugin, missing class, linkage mismatch, or uninitialized runtime returns
  `false`;
- unexpected screen failures are surfaced rather than silently swallowed.

The MapGUI-only runtime entrypoint calls `MapGui.get().open(...)` and constructs the
screen. This prevents JVM linkage failures when the optional plugin is not present.

### 4.2 Graph screen

`SkillTreeMapScreen` extends `de.flog99.mapgui.Screen` and owns one session for a
job/player/editor draft.

- `Draw` paints prerequisite edges before node cells.
- Nodes use their configured positions, with a bounded pan offset and center/reset
  behavior for graphs larger than the hand map.
- Cursor tracking paints an explicit hover outline and caption.
- Status colors distinguish locked, available, owned/active, maxed, and excluded.
- Pan controls follow the sibling project pattern: edge arrow buttons and a compact
  toolbar.
- Clicking a node pushes `SkillNodeDetailScreen`.

### 4.3 Detail and editor screens

`SkillNodeDetailScreen` displays the node name, description, level/cost state,
requirements, prerequisites, excludes, and active effects.

- Player actions call `UpgradeService.purchaseSkillLevel` or `purchaseMajor`.
- Major purchases retain confirmation behavior.
- Typed purchase results become action-bar/status messages.
- Admin-only controls are omitted entirely unless the viewer has the edit
  permission.

Editor property screens use MapGUI `Field`, `Toggle`, `Button`, and pushed choice
screens. They support the existing editor capability set:

- add/delete/move nodes;
- add/remove prerequisite links;
- edit tree settings and path points;
- edit node metadata, kinds, icons, descriptions, costs, levels, requirements,
  excludes, and effects;
- undo/redo;
- save.

Structural edits are draft-only until Save. Player purchase actions never mutate the
editor draft.

## 5. Canonical v2 document

The editor uses a mutable JSON document with a parsed `SkillTree` view. Keeping the
JSON document avoids lossy domain-to-JSON conversion for nested requirement trees,
boost-source effects, and future fields.

The document shape is v2:

```json
{
  "version": 2,
  "job": "miner",
  "skill_points_per_level": 1,
  "root": "mining_basics",
  "nodes": {
    "mining_basics": {
      "kind": "root",
      "name": "Mining Basics",
      "position": { "x": 4, "y": 1 }
    }
  }
}
```

`SkillTreeConfigParser` remains the validator and parser. The new document/editor
model mutates JSON fields and reparses after mutations that affect graph validity.
Save always reparses the complete document before applying it.

## 6. Legacy migration

`UpgradeTreeLoader` continues loading legacy and Wynncraft files and registering both
the legacy tree and the converted v2 tree for compatibility. The existing
`convertLegacy` semantics remain the migration source of truth:

- numeric legacy node IDs aggregate into perk IDs;
- prerequisites map to v2 `prerequisites`;
- exclusives map to v2 `excludes`;
- `maxedPrerequisites` map to `NodeLevelRequirement` at the converted target max;
- level costs/effects map to v2 `NodeLevel` entries;
- positions carry over; legacy state writes have no equivalent and are not synthesized.

A new editor-loading/migration path materializes a v2 JSON document from the
converted graph when the source file is not already v2. The first successful save
writes `upgrade_trees/<tree-id>.json` as v2. No database schema or player-state
migration is performed.

## 7. Immediate save and reload

Add a v2-specific save method to `UpgradeTreeLoader` rather than routing v2 data
through `saveTree(String, String)`:

1. Parse the candidate JSON and require version 2.
2. Validate job/root/node references, requirement/effect vocabulary, and all field
   constraints through `SkillTreeConfigParser`.
3. Write to a sibling temporary file in the same directory.
4. Atomically replace the target file, with a same-filesystem fallback if the
   platform rejects `ATOMIC_MOVE`.
5. Register the validated `SkillTree` only after replacement succeeds.
6. Return a typed success/error result to the editor.

A failed parse, validation, write, or registry operation leaves the previous file,
active tree, and editor draft available for retry. The current screen invalidates
against the new tree after success. Other players observe the replacement on their
next graph rebuild/open; their persisted node levels remain untouched.

## 8. Command and permission behavior

`/jobs upgrade <job>`:

1. Resolve the job and its v2 tree (including the v2 adapter produced for a legacy tree).
2. Try `SkillTreeMapScreenBridge.open(...)`.
3. If it returns `false`, open the existing Craftux viewer/purchase screen.
4. If the viewer has edit permission but MapGUI is absent, send the explicit
   install message and keep the fallback viewer usable.

Keep `TreeEditorCommand` registered only as a compatibility command that
delegates to the same unified MapGUI/Craftux route as `/jobs upgrade`. It does not
open a second UI. Retain `jobs.command.admin.treeeditor` in `plugin.yml` as the
editor capability and update its description. No new command or permission is
introduced.

## 9. Failure handling and safety

- Missing MapGUI or API version mismatch fails closed to Craftux.
- Invalid draft content stays open and reports the first actionable validation
  problem; no partial save is attempted.
- Save failures preserve both draft and active registry.
- Deleted node keys are not silently mapped to different nodes.
- Purchase failures preserve the screen and show the typed service result.
- Editor operations are permission-gated at screen-build and action time, so a
  permission change while open cannot leave an editing action available.
- Existing domain-level job/upgrade services do not depend on MapGUI.

## 10. Verification plan

Focused tests:

- v2 document parse/edit/round-trip coverage;
- legacy-to-v2 migration coverage, including maxed prerequisites;
- loader validation and atomic-save failure behavior;
- graph geometry, status colors, and prerequisite edge projection;
- admin-control omission/inclusion based on permission;
- no-MapGUI bridge behavior;
- existing `UpgradeService` purchase and state persistence tests.

Add an opt-in `visualTest` source set modeled on `../modular-territories` for
MapGUI-backed compilation/render checks without adding MapGUI to the normal unit
test runtime.

Manual end-to-end verification uses `:paper:runServer` with MapGUI 1.0.0:

- open `/jobs upgrade miner` as a normal player;
- inspect and purchase a node/major;
- open the same job with the edit permission;
- add/move/link/edit/delete a node, undo/redo, save, and observe immediate reload;
- restart and confirm v2 JSON reloads with player state intact;
- remove MapGUI and confirm viewer/purchase fallback plus admin install guidance.

Update the skill-tree living spec, permission/operator docs, README where command
usage is documented, and changelog after implementation is verified.

## 11. Non-goals

- No replacement of `SkillTree`, `UpgradeService`, or PostgreSQL persistence.
- No MapGUI shading or fork of the upstream project.
- No Azoth/gathering-gate changes.
- No new player respec or currency behavior.
- No second editor UI or parallel legacy/v2 authoring format.
