# Professions — Living Spec

> Status: active  
> Last updated: 2026-08-10  
> Owners: ModularJobs and Azoth maintainers

## Intent

Expose catalog-backed profession identity, levels, progression tasks, and
integration services. Azoth consumes the authoritative `ProfessionService` and
owns all hard world-interaction gates. Success: operators can configure
profession requirements without duplicating level state or mixing profession
levels with Azoth combat levels.

## Boundaries

### ModularJobs in scope

- Profession catalog and level/experience resolution from job progression.
- Java 21-compatible `modularjobs-api` and `modularjobs-common` artifacts.
- Progression task data, including Herbalism, and payment listeners.
- Always-available Bukkit `ProfessionService`; optional auxiliary services remain
  behind `profession-apis.register-bukkit-services`.

### Azoth in scope

- `block-break-gates` for mining, woodcutting, farming, and herbalism.
- `fish-catch-gates` for cod, salmon, tropical fish, and pufferfish.
- `interaction-gates` for log stripping and mature sweet-berry, cocoa, and
  berry-bearing cave-vine harvesting.
- Gate predicates, denial messages, bypass permission, and listener enforcement.

### Out of scope

- Azoth combat-level migration or combat-level changes.
- Tool/enchant-tier, region, wildcard, GUI, database-schema, or pre-roll loot
  filtering.
- Breeding, shearing, milking, fishing junk, and fishing treasure gates.

## Invariants

- A configured gate is explicit: material/action, profession id or alias, and a
  positive minimum level.
- Unknown material/profession or invalid level is warned and skipped.
- Missing profession level is insufficient; exact and higher levels pass.
- Azoth gate listeners run at NORMAL with `ignoreCancelled=true`; denied events
  are cancelled before ModularJobs MONITOR payment listeners.
- Cancelled gathering events receive no progression/payment.
- `azoth.bypassgathering` bypasses all Azoth gathering gates.
- ModularJobs does not enforce gathering gates and does not depend on Azoth.

## Implementation guidance

- Resolve gate profession aliases through `ProfessionService.resolve` at Azoth
  startup and query `ProfessionService.level` only at event time.
- Keep gate configuration and Paper event predicates in Azoth; keep the public
  ModularJobs API free of Paper types.
- Keep mature-plant and log-strip payment predicates aligned with the documented
  Azoth predicates.

## Current

- [x] Profession catalog, level, and experience API
- [x] Always-registered core `ProfessionService`
- [x] Java 21 API/common publication at version 2.0.0
- [x] Herbalism task data and operator-run migration
- [x] Azoth-owned block, fish, log-strip, and plant-harvest gate design

## Next

- [ ] Add further gate families only after an explicit cross-plugin design.
- [ ] Add reload/editor support only with a defined lifecycle and validation contract.

## Future

- [ ] Optional region/tool conditions after a stable predicate contract exists.
- [ ] Pre-roll fishing pool filtering if a stable Paper API becomes available.

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-07 | Hard gate on the stable Paper event boundary | Avoid NMS/datapack coupling |
| 2026-08-07 | Explicit profession per material/action | Decouple enforcement from paying job |
| 2026-08-08 | No material wildcards | Predictable configuration |
| 2026-08-10 | Azoth owns gathering enforcement | Keep world policy with the interaction host |
| 2026-08-10 | Core ProfessionService always registered | Required dependency has a stable integration point |

## Open questions

- [ ] Should future gate families use separate bypass permissions or retain the single Azoth node?
