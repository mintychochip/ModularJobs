# Skill tree (job upgrades) — Living Spec

> Status: active  
> Last updated: 2026-08-08  
> Owners: modularjobs maintainers

## Intent

Each job can own a **JSON-driven skill-node graph**. Java owns the system
(loading, unlock rules, persistence, effect application); JSON owns content
(topology, costs, requirements, effects, path conflicts). “Blacksmith →
Weaponsmith” is content, not special-cased Java.

## Boundaries

### In scope

- Node kinds: root, skill, major (advancement)
- Per-level costs, cumulative/replace effect modes
- Requirement trees (`all` / `any` / `not`)
- Explicit `excludes` path locking
- Skill points from job levels; spend on nodes
- Player state: total points, node levels, derived effects
- Leave job → clear that job’s tree state
- In-game tree editor GUI (admin) + JSON loaders (`SkillTreeConfigParser`, …)

### Out of scope / non-goals

- Pet-specialization permanent `/upgrade` flow (legacy, separate)
- New spend currencies (money/items) for nodes — skill points only for now;
  cost schema may stay forward-compatible
- Player respec of major choices in normal play (majors permanent)
- Recipe/action content for specific jobs

## Invariants

- Content is data-driven; no hardcoded job specialization classes.
- Authoritative purchased state is **node levels map** when present (`node_levels`);
  legacy unlocked set may still exist during migration.
- Spent points = sum of actual level costs, not `unlockedNodes.size()`.
- Effects are **derived** from persisted levels (recalc on reset/leave/reload), not one-shot unreproducible mutations only.
- Leaving a job clears upgrade data for that job.

## Implementation guidance

- Packages: `api/.../upgrade`, `paper/.../upgrade` (+ config, editor).
- Prefer `SkillTreeConfigParser` path for new trees; keep Wynncraft deserializers only as compatibility if still needed.
- Persist via `player_upgrades` (MySQL); connect-only schema.
- Tests: `SkillTreeTest` and service tests for excludes/requirements/points.

### Explicit do-nots

- Do not hardcode Blacksmith/Weaponsmith (or any job) topology in Java.
- Do not treat major unlocks as freely respeccable without an explicit product decision.
- Do not invent money costs without promoting from Future and updating this catalog.

## Current

- [x] Upgrade tree graph model, loader, service unlock/reset
- [x] Skill tree config parsers (requirements, effects, excludes)
- [x] `node_levels` column on `player_upgrades`
- [x] Skill points per job level + retroactive calculation hooks
- [x] In-game tree editor GUIs
- [ ] Close remaining gaps from design spec vs runtime (verify spent-points, effect derive mode, leave cleanup) against `docs/superpowers/specs/2026-08-05-job-skill-tree-design.md`

### Current notes

Design: `docs/superpowers/specs/2026-08-05-job-skill-tree-design.md` and plan sibling.
Treat unchecked design gaps as **Current** verification work, not greenfield.

## Next

- [ ] Audit and fix any remaining design gaps (spent points, effect re-derive, leave clear)
- [ ] Content: ship/refine at least one production job tree JSON as reference

## Future

- [ ] Money/item node costs
- [ ] Player-facing respec (paid or free) for majors
- [ ] Shared cross-job trees / global talent layers

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-05 | Single node model, JSON content | Generality; no special-case jobs |
| 2026-08-05 | Skill points only (for now) | Simpler economy; schema forward-compatible |
| 2026-08-05 | Majors permanent in normal play | Meaningful path choices |

## Open questions

- [ ] Is Wynncraft tree format still a supported input or legacy-only?
