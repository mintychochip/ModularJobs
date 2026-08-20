# Player conditions — Living Spec

> Status: active  
> Last updated: 2026-08-19  
> Owners: modularjobs maintainers

## Intent

Player predicates as their own API (not a boost implementation detail). Success:
conditions evaluate against a Paper-free snapshot; JSON is vanilla loot-condition
shaped; boosts persist a condition as serializer **bytes** next to rule priority.

## Boundaries

### In scope

- Standalone library [aincraft-org/conditions](https://github.com/aincraft-org/conditions)
  (`dev.conditions:api|gson|paper`, CalVer `YY.M.D.REVISION`)
- Snapshots for player, living entity, and block
- Paper adapters (`from`, `fromLiving`, `fromBlock`) + `modularjobs:*` types
- Boost rule wire format: `priority` + condition bytes + boost
- `SerializableBoostData` JSON on items and timed-boost blobs

### Out of scope / non-goals

- Full vanilla loot table grammar
- NMS predicate codecs

## Invariants

- `api` / `common` have zero Bukkit/Paper types.
- `Condition` never looks up a `Player`; Paper builds `ConditionContext`.
- Unknown condition ids fail on read.
- Missing player / missing snapshot fields fail closed (`false`).
- Kryo is not used for conditions or `SerializableBoostData`.

## Implementation guidance

- Library lives in [aincraft-org/conditions](https://github.com/aincraft-org/conditions).
  ModularJobs consumes `dev.conditions:{api,gson,paper}` (sibling
  `../conditions/build/maven-repo` or GitHub Packages).
- Primitive bag lives in [mintychochip/databag](https://github.com/mintychochip/databag)
  (`dev.databag:databag`, sibling `../databag/build/maven-repo`). Conditions
  `PersistentBags` embeds it as PDC `BYTE_ARRAY`.
- DataBag writes envelope v1 (`DBAG` + version + length-prefixed entries);
  reads unversioned v0. Payload encodings that may change use
  `setBytes(key, formatId, bytes)` / `getFormatted`. Unknown tags in v1+ skip;
  unknown envelope versions throw.
- CalVer `YY.M.D.REVISION` on that repo; local default `0.0.0-SNAPSHOT`.
- Adventure-shaped: immutable records, `ConditionSerializer.read/write(byte[])`.
- Spec against Paper/Minecraft JSON keys for built-in kinds. Third-party ids
  register a `ConditionHandler` (JSON → `DataBag` arguments +
  `ConditionContext.extras()`); do not fold party/region/profession/etc. into
  ModularJobs or the core factory.
- Boost config files may inline condition objects; item/DB stores bytes.

### Explicit do-nots

- Do not reintroduce Kryo condition codecs.
- Do not evaluate conditions via `Bukkit.getPlayer` inside `api`.
- Do not persist `BoostSource` with a separate codec from `SerializableBoostData`.

## Current

- [x] `dev.conditions` snapshot model in `api`
- [x] `GsonConditionSerializer` vanilla JSON bytes in `common`
- [x] Boost rule persistence: priority + condition bytes
- [x] `SerializableBoostData` JSON on items and `time_boosts`
- [x] Remove `KryoCodecRegistry` from this path
- [x] Paper `ConditionContext` from live player
- [x] Extracted to `github.com/aincraft-org/conditions` with CalVer
- [x] Living-entity and block snapshots + vanilla JSON kinds
- [x] Kryo primitive bag (`DataBag`) embeds on items as PDC `BYTE_ARRAY`
- [x] `DataBag` extracted to [mintychochip/databag](https://github.com/mintychochip/databag) (`dev.databag:databag`)
- [x] DataBag envelope versions + formatted payload ids for migrations
- [x] `ConditionHandler` / `DataHandler` SPI so extra predicates are not ModularJobs-owned

## Next

- [ ] Broader vanilla subset (time_check, equipment, random_chance) as content needs

## Future

- [ ] Predicate files / `minecraft:reference`
- [ ] Editor UI for vanilla-shaped conditions

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-19 | Adventure-shaped, specced against Paper, packages in `api`/`common`/`paper` | Reusable API without a fourth Gradle module |
| 2026-08-19 | JSON bytes, not NMS codecs | Unit-testable; item PDC is UTF-8 JSON |
| 2026-08-19 | Boost rules store condition as byte[] | Conditions API owns ser/de; boosts stay opaque |
| 2026-08-19 | Drop Kryo | Serializer registry moves to conditions API |
| 2026-08-19 | Standalone `aincraft-org/conditions`, CalVer `YY.M.D.REVISION` | Library is reusable; ModularJobs consumes published artifacts |
| 2026-08-19 | Maven group + Java package `dev.conditions` | Matches Craftux `dev.craftux` style |
| 2026-08-19 | Kryo `DataBag` primitives on item PDC `BYTE_ARRAY` | Light PDC-like store; conditions stay JSON bytes inside the bag |
| 2026-08-19 | `DataBag` lives in `mintychochip/databag` (`dev.databag`) | Primitive bag is reusable; conditions/paper keep `PersistentBags` |
| 2026-08-19 | DataBag v1 envelope + format ids on `byte[]` slots | Unversioned bags still read; new primitives skip; payload encodings migrate |
| 2026-08-19 | `ConditionHandler` + `DataHandler` registries; extras `DataBag` on the snapshot | Party/region/etc. stay out of ModularJobs; unknown ids still throw |

## Open questions

- [x] JSON dialect — vanilla loot-condition subset
- [x] Kryo — removed; conditions API is the reader/writer
