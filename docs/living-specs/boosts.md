# Boosts — Living Spec

> Status: active  
> Last updated: 2026-08-19  
> Owners: modularjobs maintainers

## Intent

Multiply job rewards via timed and item/context boosts with a clear evaluation
engine (targets, conditions, policies). Success: admin-granted timed boosts
persist in MySQL; evaluation is deterministic; boost admin commands stay
permission-gated.

## Boundaries

### In scope

- Boost model in `api` (`container.boost`, factories)
- `BoostEngine` evaluation in `paper` payment path
- Timed boost persistence (`time_boosts` / identity tables)
- Conditions and policies for when a boost applies (player predicates owned by
  [conditions.md](./conditions.md); boosts persist them as serializer bytes)
- Admin `/jobs boost` (requires `modularjobs.admin`)

### Out of scope / non-goals

- Skill-tree effects that are not boosts (see skill-tree effect appliers)
- Global server economy inflation tools outside job payables

## Invariants

- Timed boost store is MySQL connect-only like all remote stores.
- Boost admin is not available to normal players.
- Evaluation must not double-apply the same source incorrectly (identity keyed by target+source).

## Implementation guidance

- Prefer sealed/typed targets and registered condition/policy factories.
- Config loaders under `boost/config`; keep engine free of Bukkit where possible,
  adapters at the edges.
- Tests: pure engine cases + repository tests with live PG when available.

### Explicit do-nots

- Do not expose unrestricted boost grant without admin permission.
- Do not store boosts only in memory for production timed boosts.

## Current

- [x] Boost engine + condition/policy factories
- [x] Timed boost repository (MySQL)
- [x] Admin boost command permission-gated
- [x] Integration into payment calculation path

### Current notes

Player predicates live in `dev.conditions`. Boost rules persist
`priority` + condition serializer bytes inside `SerializableBoostData` JSON.
Item PDC stores that JSON inside a `dev.databag.DataBag` `BYTE_ARRAY`.
Touch payment pipeline order carefully.

## Next

- [ ] Broader vanilla condition subset as content needs (see conditions catalog)

## Future

- [ ] Metrics: boost application rate / latency histograms
- [ ] Player-visible active boost list UX improvements
- [ ] More condition types (world, biome, tool, party, …) as content needs

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 1.1.0 | Gate `/jobs boost` behind admin | Security |

## Open questions

- [ ] None active
