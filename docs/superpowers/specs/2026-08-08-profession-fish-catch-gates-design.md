# Profession-Gated Fish Catching

**Date:** 2026-08-08
**Status:** Approved

## Problem

The plugin already pays for `PlayerFishEvent` catches, but profession levels do not
control which fish a player can obtain. The fishing profession is cataloged as
`fishing` with the `fisherman` alias. Operators need explicit minimum levels for
vanilla fish species, analogous to `block-break-gates`.

## Decisions

- **Scope:** Gate the four vanilla fish items only: `cod`, `salmon`,
  `tropical_fish`, and `pufferfish`. Junk and treasure are not gated.
- **Config:** Opt-in, per-fish entries in a new `fish-catch-gates` section. No
  wildcard or category fallback.
- **Profession:** Each entry names an explicit profession or alias. `fisherman`
  resolves to the canonical `fishing` id through `ProfessionCatalog`.
- **Eligibility:** A player must have a joined profession level at least equal
  to the configured level. Missing level is treated as level 0.
- **Catch behavior:** Use the stable Paper `PlayerFishEvent` API at
  `State.CAUGHT_FISH`. An ineligible generated item is not collected and does
  not reach the job payment listener. There is no custom reroll and no custom
  loot-table implementation.
- **Pool boundary:** This is the stable event-level equivalent from the
  player's perspective, not a modification of vanilla's weighted loot pool.
  The vanilla roll remains unchanged internally; the configured item is simply
  rejected before collection. Exact pre-roll pool filtering would require
  version-coupled server internals or a datapack/scoreboard system and is out
  of scope.
- **Feedback:** Send one themed denial message per rejected catch. No
  rate-limiting.
- **Bypass:** `modularjobs.bypassfishcatch`, default `op`, bypasses all fish
  gates.
- **Compatibility:** An absent or empty section leaves all fishing behavior
  unchanged. No database or schema changes.

## Configuration

Add to `paper/src/main/resources/config.yml`:

```yaml
# Fish catching gates: minimum profession level required to catch a fish item.
# Fish keys are cod, salmon, tropical_fish, or pufferfish.
# Professions are catalog ids or aliases (for example fisherman -> fishing).
fish-catch-gates:
  # cod: { profession: fisherman, level: 1 }
  # salmon: { profession: fisherman, level: 10 }
  # tropical_fish: { profession: fisherman, level: 20 }
  # pufferfish: { profession: fisherman, level: 30 }
```

The loader parses keys case-insensitively. Unknown materials, non-fish
materials, unknown professions, non-integer levels, and non-positive levels
produce a warning and are skipped. A valid entry stores the lower-case item key
and canonical profession id.

## Components

### `api` module

- `FishCatchGate` — immutable record `(String itemKey, String professionId,
  int minLevel)`, normalizing item and profession ids to lower case.
- `FishCatchGateService` — read-only `gates()` and case-insensitive
  `gateFor(String itemKey)` methods.

The contracts remain Paper-free, matching the existing block-gate boundary.

### `paper` module

- `YamlFishCatchGateLoader` — reads `fish-catch-gates`, validates the key is one
  of the four fish materials, resolves the profession, validates the level, and
  logs warnings for skipped entries.
- `FishCatchGateStore` — immutable in-memory map keyed by lower-case item key;
  mirrors `BlockBreakGateStore`.
- `FishCatchGateListener` — handles
  `@EventHandler(priority = NORMAL, ignoreCancelled = true)` for
  `PlayerFishEvent`:
  1. return for bypass permission;
  2. return unless state is `CAUGHT_FISH`;
  3. return unless `getCaught()` is an `Item`;
  4. look up the caught stack's material key;
  5. return when no gate exists;
  6. check `ProfessionService.level`;
  7. when missing/below level, cancel and message the player.
- `PluginContext` loads the YAML gates and registers the listener alongside the
  block-break gate listener.
- `plugin.yml` declares the op-default bypass permission.

The listener runs at `NORMAL`; the existing fish payment handler runs at
`MONITOR` and already ignores cancelled events. Thus rejected catches produce
no `ActionTypes.FISH` payment or progression.

## Error handling

Invalid configuration never prevents startup: the loader warns and skips the
entry. A missing profession level is a blocked catch. A missing gate store entry,
non-catch fishing state, null/non-item caught entity, prior cancellation, or
bypass permission leaves the event unchanged.

## Testing

- `api`: record normalization and service case-insensitive lookup.
- `paper` loader: valid fish entries, `fisherman` canonicalization, unknown and
  non-fish keys, unknown profession, non-integer/zero/negative levels, absent
  section, and warning-safe skipping.
- `paper` listener with MockBukkit: below-level and unjoined players are
  cancelled; exact/above levels pass; unconfigured fish, non-catch states,
  prior cancellation, and bypass pass; only the caught-fish path is gated.
- Run module tests plus the normal Paper build after implementation.

## Non-goals

- Replacing or rerolling vanilla fishing loot.
- NMS patches, datapack generation, or scoreboard synchronization.
- Gating junk, treasure, fishing locations, rods, enchantments, or biomes.
- Per-player gate configuration or an editor/GUI surface.
- Database changes or a config reload command.
