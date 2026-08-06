# Job Skill Tree Refinement — Design Spec

Date: 2026-08-05
Status: Draft for review

## 1. Goal

Refine ModularJobs' job upgrade system into a generic, JSON-driven skill-node graph.
Every job's tree is defined entirely in JSON: topology, node kinds, costs, requirements,
effects, and path conflicts. Java knows the *system*; JSON defines the *content*.

"Blacksmith → Weaponsmith or Toolsmith" is an example, not a special case. There is no
hardcoded notion of a Blacksmith, Weaponsmith, or specialization anywhere in the code.

## 2. Scope

In scope:

- One graph per base job, loaded from JSON.
- One node model with three kinds: root, skill, advancement (major).
- Node levels with per-level costs and cumulative/replace effect modes.
- Configurable requirement tree with `all` / `any` / `not`.
- Explicit node `excludes` for path locking (skills lock paths too).
- Derived effect evaluation from persisted player state.
- Skill-point economy: fixed points per job level, per-node/per-level costs.
- Player state: total points, node levels, state map.
- Leaving a job clears that job's tree state.

Out of scope (explicitly):

- The existing pet-specialization `/upgrade` flow (permanent pet selection).
- New cost currencies (money, items). Skill points only, but cost schema stays
  forward-compatible.
- Recipe/action/content implementation details for any specific job.
- Player-facing respec of major choices (major choices are permanent in normal play).

## 3. Current State and Gaps

`UpgradeTree` + `UpgradeNode` already model a graph with prerequisites, exclusive nodes,
effects, positions, and perk levels. Loader (`UpgradeTreeConfigParser`, `UpgradeTreeLoader`)
reads JSON. `UpgradeServiceImpl` handles unlock/reset. `PlayerUpgradeDataImpl` persists
per player+job.

Gaps that block the target model:

| Gap | Current behavior | Target |
|---|---|---|
| Node levels | Each level is a separate node (`efficiency_1`) | One node with internal levels |
| Cost | Single scalar `cost` per node | Per-level costs |
| Spent points | `spentSkillPoints() == unlockedNodes.size()` (placeholder) | Sum of actual level costs |
| Node state | `Set<String> unlockedNodes` | `Map<nodeKey, level>` |
| State writes | `perkLevels` auxiliary map, not persisted authoritatively | Namespaced state map derived from purchased majors |
| Effect application | One-time mutation on unlock | Derived from persisted levels; recalculated on reset/leave/reload |
| Path conflicts | One-sided `exclusive` set | Symmetric explicit `excludes`, any node kind |
| Requirements | `prerequisites` + `maxedPrerequisites` | Registered condition tree with all/any/not |
| Leave cleanup | Upgrade data not cleared on leave | Cleared on leave |

## 4. Node Model

One node model. No separate advancement class.

```
Node
├── kind: root | skill | major
├── name, description, icon (locked/unlocked), item model
├── cost: skill points, plain int (root: 0)
├── levels[]: per-level cost + effects (skill only)
├── level_effect_mode: cumulative | replace (skill only)
├── requirements: registered condition tree (all/any/not)
├── excludes: explicit symmetric path locks (any kind)
├── effects: registered effect list (per-level for skills)
├── state[]: keyed state writes (major only)
├── position, path points (UI)
```

### 4.1 Node kinds

- **root** — starting point, cost 0, no requirements.
- **skill** — repeatable, one purchase per level. Owns `levels[]` and
  `level_effect_mode`.
- **major** — one-time permanent choice. Confirmation required before purchase.
  Owns optional `state[]` writes. Requires `max_level: 1` semantics (single purchase).

### 4.2 Levels

Ordered array, not map, so level order is unambiguous:

```json
"levels": [
  { "cost": 1, "effects": [ ... ] },
  { "cost": 2, "effects": [ ... ] },
  { "cost": 4, "effects": [ ... ] }
]
```

`level_effect_mode`:

- `cumulative` — active effects = effects of levels 1..current.
- `replace` — active effects = effects of current level only.

Per-node setting, chosen in JSON.

### 4.3 Requirements

Registered condition tree. Bounded vocabulary; unknown types fail tree loading.

```
Requirement
├── AllOf(requirements)
├── AnyOf(requirements)
├── Not(requirement)
└── leaves:
    ├── job_level      (job, minimum)
    ├── node_level     (node, minimum)
    ├── node_unlocked  (node)
    ├── state_equals   (key, value)
    └── permission     (key)
```

Example:

```json
"requirements": {
  "any": [
    { "type": "job_level", "minimum": 30 },
    { "type": "permission", "key": "jobs.special_access" }
  ]
}
```

### 4.4 Excludes (path locks)

Any node kind may lock out other nodes. Declared explicitly, normalized symmetric:

```json
"excludes": ["toolsmith"]
```

Weaponsmith excludes Toolsmith; loader normalizes to a symmetric conflict set.
Exclusivity is never inferred from state writes or graph reachability.

Validation:

- If two nodes can coexist, their state writes must not conflict (same key,
  different value → load error).
- If two nodes are mutually exclusive, conflicting state writes are allowed.

### 4.5 State writes (major-only)

```json
"state": [
  { "set": { "tree.vocation": "weaponsmith" } }
]
```

Keys are namespaced and registered. Unknown keys fail loading. Writes are
`set` semantics by default; `remove` reserved for future use.

Two majors writing the same key to different values require explicit mutual
exclusion (see 4.4). No purchase-order/journal persistence in v1.

### 4.6 Effects

Registered list; unknown types fail tree loading.

```
boost
ruled_boost
permission
recipe_unlock
state_set
```

## 5. Economy

- Skill points are the only node currency.
- Awarded at a fixed configured rate per job level (`skill_points_per_level`).
- Cost is a plain integer skill-point amount:

```json
"cost": 5
```

- Spent points = sum of purchased level costs (cumulative along a skill's levels)
  plus major node costs.
- `available = total - spent`.

## 6. Player State

Persisted per player + job:

```
PlayerJobTreeState
├── total_skill_points
└── node_levels: Map<nodeKey, level>
```

Derived views (never persisted):

```
unlocked nodes   = { node | node_levels[node] > 0 }
state map        = recomputed from purchased majors
perk levels      = node_levels grouped by perk
```

Authoritative rules:

- `node_levels` is the single source of truth for what a player owns.
- No separate unlocked-nodes set; unlock state is `level > 0`.
- No separate perk-level map; that is derivable from `node_levels`.
- `state` is derived from purchased majors, recomputed on reset/leave/reload.

## 7. Effect Evaluation

Active effects are **derived**, never one-time mutations:

```
active = derive(node_levels, purchased_majors, tree)
```

- `cumulative` skill: sum effects 1..level.
- `replace` skill: effects(level) only.
- majors: effects + state writes of purchased majors.
- Reset, leave, reload all recompute `active` and sync the player to it.
  No stale boosts/permissions survive.

## 8. Lifecycle

- **Level up**: award `skill_points_per_level` points.
- **Purchase skill level**: validate requirements, cost, excludes; persist level;
  recompute+sync effects.
- **Purchase major**: confirmation step; revalidate everything server-side;
  persist; recompute+sync.
- **In-job skill reset**: refund purchased ordinary skill levels (remove levels),
  recompute+sync. Major nodes are not refunded by a normal reset.
- **Leave job**: clear that job's tree state (levels, majors, state, points),
  revoke effects, delete persisted data. Rejoin starts fresh.

## 9. JSON Schema Sketch

```json
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
        { "cost": 2, "effects": [ { "type": "boost", "target": "xp", "amount": 1.2 } ] },
        { "cost": 4, "effects": [ { "type": "boost", "target": "xp", "amount": 1.35 } ] }
      ]
    },
    "blasting": {
      "kind": "skill",
      "name": "Blasting",
      "prerequisites": ["mining_basics"],
      "excludes": ["deep_mine"],
      "levels": [
        { "cost": 3, "effects": [ { "type": "permission", "key": "jobs.miner.blasting" } ] }
      ]
    },
    "deep_mine": {
      "kind": "skill",
      "name": "Deep Mining",
      "prerequisites": ["mining_basics"],
      "excludes": ["blasting"],
      "levels": [
        { "cost": 3, "effects": [ { "type": "permission", "key": "jobs.miner.deep_mine" } ] }
      ]
    },
    "weaponsmith": {
      "kind": "major",
      "name": "Weaponsmith",
      "prerequisites": ["efficiency"],
      "cost": 5,
      "excludes": ["toolsmith"],
      "requirements": {
        "all": [
          { "type": "node_level", "node": "efficiency", "minimum": 3 }
        ]
      },
      "state": [
        { "set": { "tree.vocation": "weaponsmith" } }
      ]
    },
    "toolsmith": {
      "kind": "major",
      "name": "Toolsmith",
      "prerequisites": ["efficiency"],
      "cost": 5,
      "excludes": ["weaponsmith"],
      "requirements": {
        "all": [
          { "type": "node_level", "node": "efficiency", "minimum": 3 }
        ]
      },
      "state": [
        { "set": { "tree.vocation": "toolsmith" } }
      ]
    }
  }
}
```

## 10. Migration

- Existing `upgrade_trees.json` (legacy format) continues to load.
- Legacy nodes map to the new model: each legacy node becomes a single-level skill
  (or a major if it writes state); `prerequisites`/`exclusive`/`children` map to the
  new fields; `children` becomes derived.
- `player_upgrades` table gains a `node_levels` (JSON) column;
  legacy `unlocked_nodes` rows migrate to `node_levels: {key: 1}`.
- Retroactive skill-point calculation preserved (level × points per level).

## 11. Open Questions

- Exact UI for confirmation dialog on major purchases.
- Whether `recipe_unlock` targets a namespaced recipe or a recipe group.