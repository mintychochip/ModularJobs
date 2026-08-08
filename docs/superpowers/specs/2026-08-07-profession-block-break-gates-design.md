# Profession-Gated Block Breaking

**Date:** 2026-08-07
**Status:** Draft for review

## Problem

Players can break any block regardless of their profession progression. The
plugin already tracks profession levels (via the §8.1 `ProfessionCatalog` +
job progression), but nothing restricts block breaking by level. This feature
lets the server tie breaking to profession level: e.g. mining level 30
required to break diamond ore.

## Decisions (from design session)

- **Hard gate**: cannot break at all below the required level. The
  `BlockBreakEvent` is cancelled; the block stays; player gets a message on
  each attempt.
- **Check timing**: on break attempt, inside `BlockBreakEvent`.
- **Config**: per-material entries. No category/`*` fallbacks.
- **Profession**: explicit profession id per block entry — decoupled from the
  paying job.
- **Feedback**: message with reason; spam-rate-limiting not included in scope
  (simple direct message per attempt).
- **Bypass**: permission `modularjobs.bypassblockbreak`, `default: op`
  (mirrors `modularjobs.admin`). Ops bypass by virtue of the default.
- **Scope**: block ALL breaking of that material outright for unqualified
  players. No tool/enchant-tier gating in this feature.

## Config

New section in `paper/src/main/resources/config.yml`:

```yaml
# Block breaking gates: minimum profession level required to break a material.
# Material names are Minecraft keys (diamond_ore, ancient_debris, ...).
# Professions are §8.1 catalog ids (mining, woodcutting, farming, ...).
block-break-gates:
  diamond_ore: { profession: mining, level: 30 }
  deepslate_diamond_ore: { profession: mining, level: 30 }
  ancient_debris: { profession: mining, level: 40 }
```

- Material key parsed via `Material.matchMaterial(...)`, case-insensitive;
  unknown material → load warning, entry skipped.
- Unknown/empty profession id → load warning, entry skipped.
- `level <= 0` → load warning, entry skipped.
- Section absent/empty → feature disabled entirely; zero overhead on break.

## Components

### api module (new)

- `BlockBreakGate` — immutable record:
  `(Material material, String professionId, int minLevel)`.
- `BlockBreakGateService` — `@NotNull List<BlockBreakGate> gates()`,
  `Optional<BlockBreakGate> gateFor(Material)`.
  Kept in `api` so consumers outside `paper` (none today, but the profession
  APIs are deliberately published) can read the gate table without Paper
  internals.

### paper module

- `YamlBlockBreakGateLoader` — parses the `block-break-gates` section into
  `List<BlockBreakGate>`; validates material/profession/level; logs warnings.
  Pattern follows the existing config loaders (`SkillTreeConfigParser`,
  `UpgradeTreeLoader`).
- `BlockBreakGateStore` — immutable in-memory cache of loaded gates, keyed by
  material (guava `ImmutableMap` or plain `Map`); mirrors
  `MemoryBuffService`/`MemoryRecipeService`. Loaded once at startup from
  `config.yml` (consistent with jobs.yml / job_tasks.yml — the plugin has no
  central config reload path today); loader kept separately so a future
  reload path can re-invoke it.
- `BlockBreakGateListener` — Bukkit `Listener`:
  - `@EventHandler(priority = NORMAL, ignoreCancelled = true)` on
    `BlockBreakEvent`.
  - Bypass: `event.getPlayer().hasPermission("modularjobs.bypassblockbreak")`
    → return.
  - Look up gate for `event.getBlock().getType()`; none → return.
  - `ProfessionService.level(player, gate.professionId())`; empty (not joined)
    or `< gate.minLevel()` → cancel + message.
  - Message: `<error>Mining level <primary>30</primary> required to break
    <secondary>diamond ore</secondary>` via existing `Messages` MiniMessage
    tags.
- Wire into `PluginContext`/`ProfessionWiring` composition root; register
  listener in the bootstrap.

### Priority ordering (critical)

Existing `JobPaymentListener.onBlockBreak` runs at `MONITOR`. The gate fires
at `NORMAL`, so a cancelled break never reaches pay logic, never populates
`breakCache`, and never re-arms exploit protection. Denied break → no
interaction with payment/exploit systems at all.

### plugin.yml

```yaml
permissions:
  modularjobs.bypassblockbreak:
    description: Bypass profession-gated block breaking
    default: op
```

## Error handling

- Bad config entries never crash startup: warn + skip.
- `ProfessionService.level` empty (player never joined that profession) is
  treated as level 0 → blocked, message shown.
- No gates configured → listener still registers but short-circuits on the
  empty map lookup (negligible cost).

## Testing

- `api`: `BlockBreakGate` + service model test.
- `paper` (MockBukkit):
  - Loader: valid entries parse; bad material/profession/level warn & skip;
    empty section → empty list.
  - Listener: below level → `BlockBreakEvent` cancelled + message; at/above
    level → not cancelled; no gate for material → not cancelled; bypass
    permission → not cancelled; unjoined profession → cancelled.
- Gate-level semantics reuse `ProfessionServiceTest`-style JSON fixtures if
  needed; keep tests deterministic and isolated.

## Non-goals

- No soft-gate (deny pay but allow break).
- No tool/enchantment tier gating.
- No per-player config; gates are global.
- No rate limiting of the denial message.
- No GUI/editor surface for gates (config file only).
