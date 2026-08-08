# Professions — Living Spec

> Status: active  
> Last updated: 2026-08-08  
> Owners: modularjobs maintainers

## Intent

Expose profession identity and levels (catalog-backed) for progression display,
integrations, and **world interaction gates**. Success: operators can require
minimum profession levels to break materials; bypass is explicit permission;
profession Bukkit services stay off unless an integrator enables them.

## Boundaries

### In scope

- Profession catalog / level resolution from job progression (§8.1 style ids)
- `BlockBreakGate` model (`api`) + YAML loader/store/listener (`paper`)
- Config: `block-break-gates`, `profession-apis.register-bukkit-services`
- Permission: `modularjobs.bypassblockbreak` (default op)

### Out of scope / non-goals

- Tool/enchant-tier gating for block break (not in gates feature)
- Category/`*` wildcards for materials (explicit materials only)
- Spam rate-limiting gate denial messages (direct message per attempt)
- Full Azoth station/harvest implementations (stubs only unless promoted)

## Invariants

- Gate = hard cancel of `BlockBreakEvent` below required level; block stays.
- Profession id on a gate is **explicit**, not inferred from the paying job.
- Unknown material / profession / non-positive level → load warning, entry skipped.
- Empty/absent `block-break-gates` → feature disabled (no break overhead).
- Bukkit profession services register **only** when config flag is true.

## Implementation guidance

- Load gates at enable via `YamlBlockBreakGateLoader` → immutable `BlockBreakGateStore`.
- Listener: NORMAL, ignoreCancelled; check bypass → gate → profession level.
- Keep contracts in `api` so non-paper consumers can read gate tables later.
- Tests: loader validation + listener behavior with stub ProfessionService.

### Explicit do-nots

- Do not soft-fail break (damage block partially) — hard gate only.
- Do not register profession Bukkit services by default.
- Do not invent category wildcards without updating this catalog.

## Current

- [x] Block break gate API + paper loader/store/listener
- [x] Config section + bypass permission + README/changelog
- [x] Profession APIs feature-flagged off by default
- [x] Tests for loader and listener

### Current notes

Shipped via PR #22 / feat/block-break-gates. Default config gates commented out
(operators opt in per material).

## Next

- [ ] Optional: example gate pack in docs/wiki for common ores
- [ ] No further gate types committed until operators request (place, craft, …)

## Future

- [ ] Place/interact gates mirroring break
- [ ] Message spam rate-limit
- [ ] Real ProfessionService implementations beyond stubs / station hooks
- [ ] Tool-tier or region conditions on gates

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-07 | Hard gate on break event | Clear progression gating |
| 2026-08-07 | Explicit profession per material | Decouple from paying job |
| 2026-08-07 | No material wildcards | Predictable config |
| 2026-08 | Profession Bukkit services off by default | Avoid stub footguns |

## Open questions

- [ ] Should gate denials use Messages theme tokens consistently everywhere?
