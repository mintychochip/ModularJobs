# Design: Player conditions API

**Date:** 2026-08-19  
**Status:** Approved for implementation  
**Living spec:** `docs/living-specs/conditions.md`

## Problem

Boost conditions live under `api`/`paper` boost packages, evaluate against
`BoostContext` by looking up Bukkit players, and persist through Kryo codecs
registered in `KryoCodecRegistry`. That couples a generic player-predicate
library to job boosts, blocks vanilla-shaped JSON, and makes item PDC a binary
graph of condition records.

## Goals

1. Extract a Paper-free **conditions API** in `api` (`dev.conditions`).
2. Adventure-shaped: immutable graph, `test(ConditionContext)`, no Bukkit.
3. Gson **reader/writer** in `common` that emits/consumes vanilla loot-condition
   JSON as **bytes**.
4. Boost rules persist as `{ priority, conditions (byte[]), boost }`.
5. `SerializableBoostData` is the only boost persistence document (JSON bytes
   in item PDC and `time_boosts.boost_source`).
6. Remove Kryo from this path. The serializer registry *is* the conditions API.

## Non-goals

- Full vanilla loot-condition grammar (block entity, random_chance, match_tool, …)
- Published standalone Adventure-style artifacts (`conditions-api` / `conditions-gson`)
- NMS `LootItemCondition` codecs
- Changing MySQL schema (BLOBs stay; contents become UTF-8 JSON)

## Module seams

| Module | Owns |
|--------|------|
| `api` | `Condition`, `ConditionContext`, condition records, `ConditionSerializer` |
| `common` | `GsonConditionSerializer` (vanilla JSON bytes) |
| `paper` | Player → `ConditionContext`; item/timed JSON of `SerializableBoostData`; `modularjobs:job` data on the snapshot |

`common` may depend on `api`. `api` stays Paper-free and does not depend on `common`.

## Evaluation

`Condition.test(ConditionContext)` is a pure function of a snapshot. Paper builds
the snapshot once per boost evaluation (flags, location, weather, fluid, effects,
resources, job keys). Missing player → `ConditionContext.absent()`; conditions
fail closed (`false`). Unknown condition id fails on **read**, not at test time.

## JSON dialect

Vanilla loot-condition objects:

```json
{
  "condition": "minecraft:entity_properties",
  "entity": "this",
  "predicate": { "flags": { "is_sneaking": true } }
}
```

Supported `minecraft:*` in this slice: `all_of`, `any_of`, `inverted`,
`entity_properties` (flags, location.biomes, effects), `weather_check`,
`location_check` (fluid). Custom: `modularjobs:world`, `modularjobs:job`,
`modularjobs:player_resource`. Unknown `minecraft:*` → throw on read.

`ConditionSerializer.write(Condition) → byte[]` / `read(byte[])` — UTF-8 JSON.
That replaces Kryo condition codecs.

## Boost persistence

Runtime `Rule` still holds a decoded `Condition`. On the wire:

```json
{
  "kind": "passive",
  "slots": "all",
  "source": {
    "key": "modularjobs:mining_helmet",
    "rules": [
      {
        "priority": 100,
        "conditions": "<utf-8 json bytes, Gson byte[] / base64>",
        "boost": { "type": "multiplicative", "amount": 1.25 }
      }
    ]
  }
}
```

Item PDC key `modular_jobs:item_boost_data` stays `BYTE_ARRAY` of this JSON.
`time_boosts.boost_source` stores the same `SerializableBoostData` JSON bytes
(`kind: consumable` + duration). `time_boosts.duration` is ISO-8601 UTF-8 or
null. No Kryo fallback.

Human config (`boost_sources.json`) may still use inlined condition **objects**;
the parser runs them through `ConditionSerializer`. Item/DB always stores
condition **bytes**.

## Testing

- `api`: snapshot evaluation (sneak, compose, job keys) with no Bukkit
- `common`: vanilla JSON round-trip + unknown id rejection
- `paper`: item PDC JSON round-trip; timed row JSON; `RuledBoostSource` priority
  still highest-match-wins
