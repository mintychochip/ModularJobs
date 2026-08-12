# MapGUI Upgrade Tree Design

**Goal:** Replace the player-facing job upgrade-tree inventory GUI with a required MapGUI popup while preserving legacy/v2 upgrade behavior.

## Scope

- `/jobs upgrade <job>` opens a MapGUI popup.
- Legacy `UpgradeTree` and v2 `SkillTree` are rendered.
- Node state, prerequisites, exclusions, costs, effects, level purchases, and major-node confirmation remain service-authoritative.
- Existing `Messages` text and Bukkit sound semantics remain unchanged.
- Existing `/jobs upgrade <job> reset` command behavior remains unchanged.
- MapGUI is required at runtime; no Craftux fallback is retained for the player upgrade-tree GUI.
- The separate admin tree-editor UI is out of scope for this migration and remains on its existing stack.

## Architecture

`UpgradeTreeGui` becomes a MapGUI-backed opener/screen boundary. The command resolves the job and invokes `MapGui.get().open(player, new UpgradeTreeScreen(...))`. The screen owns only transient presentation state and delegates every mutation to `UpgradeService`.

The graph screen uses a MapGUI `Draw` node for edges, node cells, hover state, and pixel hit testing. Its coordinate system is derived from `Screen.width()`/`height()` and node `Position`; null-position nodes remain non-interactive and are not coalesced at `(0,0)`. Graph panning is bounded and invalidates the screen.

Clicking a node opens a detail screen through the MapGUI session stack. Detail content is inside a keyed `Scroll` node. Skill nodes call `purchaseSkillLevel`; major nodes push a confirmation screen and call `purchaseMajor` exactly once after confirmation. Legacy nodes call `unlock`. Success and failure feedback uses the existing message/sound matrix.

## Dependency and Runtime Metadata

- Add `compileOnly("io.github.flog99:mapgui-api:1.0.0")`.
- Add a complete `paper-plugin.yml` preserving plugin identity and permissions.
- Declare server dependency `MapGUI` with `load: BEFORE`, `required: true`, and `join-classpath: true`.
- Keep `plugin.yml` for compatibility with existing tests/legacy metadata.
- Do not shade MapGUI classes into the plugin jar.
- Add MapGUI 1.0.0 to the Paper run-server download set for runtime smoke testing.

## Lifecycle and Input

- MapGUI popup closes through MapGUI/session lifecycle; screen `onClose` clears transient pending confirmation state.
- `clickSound()` is disabled so the existing domain sounds are not doubled by MapGUI's default click sound.
- The screen accepts both mouse buttons only where needed; graph node activation must not bypass the existing service checks.
- Wheel input pans the graph when not consumed by a detail `Scroll`.
- Detail back/close returns through the MapGUI session stack.

## Verification

- Unit tests cover graph layout/hit boundaries, null positions, status precedence, legacy/v2 node actions, major confirmation/cancellation, feedback mapping, panning bounds, and lifecycle cleanup.
- Descriptor tests verify MapGUI dependency metadata and permission preservation.
- Focused tests run before the full `:paper:test` suite.
- Runtime smoke starts Paper with MapGUI, confirms plugin enablement, and exercises `/jobs upgrade miner`.
