# Jobs progression — Living Spec

> Status: active  
> Last updated: 2026-08-08  
> Owners: modularjobs maintainers

## Intent

Players join jobs, perform registered actions (break, place, craft, kill, …),
earn experience and payables under configurable eligibility rules, and progress
along leveling curves. Success: fair, configurable rewards; anti-farm controls;
reliable multi-payable accumulation under concurrency.

## Boundaries

### In scope

- Job / JobTask / JobProgression models (`api` + `paper` domain)
- Action type registry (40+ action types)
- Payment pipeline: listeners → eligibility → payables + XP → write-back
- Payment settings: creative, riding, disabled worlds, kill contribution, furnace range
- Place→break anti-farm (`exploit-config.yml`)
- Commands: browse, list, stats, archive, leaveall, admin level/exp
- PlaceholderAPI expansion for progression display

### Out of scope / non-goals

- Economy provider internals (see [payables-economy.md](./payables-economy.md))
- Skill tree topology (see [skill-tree.md](./skill-tree.md))
- Web editor session transport (see [secure-sessions.md](./secure-sessions.md))
- Profession world gates (see [professions.md](./professions.md)) — consume levels only

## Invariants

- Multi-XP awards in one payment must **reload progression per payable** so awards accumulate.
- Write-back flush failure **re-queues** with max-experience merge (no older-XP clobber).
- Kill multi-damage pays each qualifying **contributor** above cutoff.
- Payment rules honor `PaymentSettings` from config.
- Job task definitions may be edited via secure editor + apply; runtime authority for tasks is Paper repositories after apply.

## Implementation guidance

- Domain layer + mappers under `paper/.../domain`; services under `service` / payment packages.
- Action handlers stay focused; eligibility in payment settings / exploit config.
- Prefer existing action registration patterns when adding action types.
- MockBukkit for listener tests; pure unit tests for curves/mappers when possible.

### Explicit do-nots

- Do not skip progression reload when applying multiple XP payables in one flow.
- Do not pay in disabled worlds / forbidden creative-riding combos when settings forbid it.
- Do not bypass repository cache rules when applying editor task changes.

## Current

- [x] Job join/leave, progression persistence, archive
- [x] Action type system + task payables
- [x] PaymentSettings wired (creative, riding, worlds, kill cutoff)
- [x] Place→break anti-farm beyond stone (`exploit-config.yml`)
- [x] Multi-payable XP accumulation + write-back re-queue merge
- [x] Admin level/exp gated; production permission tree
- [x] PlaceholderAPI soft-depend expansion
- [x] PlaceholderAPI expansion: full `modular` placeholder set (level/experience/jobs/totallevels/maxjobs/name/description/isin/canjoin/…)
- [x] Join limits: max-jobs, per-job `jobs.join.<job>` permission, world join restriction, auto-join on login
- [x] Config-driven level-up commands (`level-up-commands` in config.yml)

### Current notes

Core progression is production-readiness grade (1.1.0 cut). New work usually
touches adjacent domains (economy, gates, editor) rather than rewriting the pipeline.

## Next

- [ ] Smoke-test payment path after Mint provider lands (money deposits + XP still correct)
- [ ] Document operator-facing payment/exploit knobs if wiki lags README

## Future

- [ ] Additional action types as content needs arise
- [ ] Richer anti-exploit policies beyond place→break
- [ ] Per-world job enable lists (if operators demand finer control than disabled-worlds)

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 1.1.0 | PaymentSettings + expand anti-farm | Production fairness / security |
| 1.1.0 | Reload progression per payable | Multi-XP correctness |
| 1.1.0 | Kill contribution pays all qualifiers | Fair multi-fighter rewards |

## Open questions

- [ ] Durable payout IDs for money awards (enables true at-most-once with Mint) — promote when economy domain prioritizes
