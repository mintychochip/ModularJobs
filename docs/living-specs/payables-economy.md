# Payables & economy — Living Spec

> Status: active  
> Last updated: 2026-08-08  
> Owners: modularjobs maintainers

## Intent

Job tasks award typed **payables** (experience, money, …). Money deposits go
through an **EconomyProvider** bridge so the payment pipeline stays free of a
specific ledger implementation. Success: experience-only servers can disable
required economy; money servers hard-fail enable without a provider when
`economy.required: true`; deposits are best-effort and never block XP integrity.

## Boundaries

### In scope

- `EconomyProvider` contract and factory selection
- **Mint** ledger bridge (`MintEconomyProvider`) — current production path
- Payable wiring, experience bar UX helpers
- Config: `economy.required`
- plugin soft-depend: `Mint`

### Out of scope / non-goals

- Implementing a full economy/ledger inside ModularJobs
- Vault as the primary money bridge (removed in favor of Mint)
- Skill-point currency (skill tree domain)
- Using Mint for chat theming / messages (`Messages` stays local)

## Invariants

- When `economy.required: true`, missing Mint → **fail enable**.
- When `required: false`, experience-only OK; money deposits fail if a task still pays money and no provider.
- Mint is resolved **lazily per deposit** (Mint registers asynchronously; avoid wiring-time race).
- Deposit uses fresh idempotency keys per call today — **not** durable payout IDs; do not build silent retries that double-credit.
- Currency/account namespaces for ModularJobs are explicit (`modularjobs:…`); do not invent ad-hoc strings in random call sites.

## Implementation guidance

- Factory: `EconomyProviderFactory` — prefer Mint when present.
- Package: `net.aincraft.payable`; wire via `PayableWiring`.
- Tests: unit-test factory selection; MockBukkit/service registration where needed.
- Keep payment pipeline calling `EconomyProvider.deposit` only — no direct LedgerService outside the provider.
- Document operator setup in README (Mint plugin required for money).

### Explicit do-nots

- Do not reintroduce Vault as default without a deliberate living-spec decision.
- Do not capture `Mint` eagerly at enable if that races Mint boot.
- Do not treat deposit failures as XP rollback unless product decides atomic multi-currency payouts (not current behavior).

## Current

- [x] `EconomyProvider` abstraction + factory
- [x] Remove Vault soft-depend / `VaultEconomyProvider` (in-flight on working tree)
- [x] `MintEconomyProvider` (issue credits via LedgerService; lazy Mint resolve)
- [ ] Land uncommitted Mint cutover (gradle Mint dep, plugin.yml, config, tests, README) with green `:paper:test`
- [ ] Verify `PluginYmlProductionReadinessTest` / factory tests match Mint-only story

### Current notes

Working tree (not necessarily committed): Vault deleted; Mint provider + soft-depend.
Comments in `MintEconomyProvider` document at-most-once limits (no durable payout id yet).

## Next

- [ ] Commit Mint economy cutover as atomic change set
- [ ] Operator docs: install Mint, currency namespace expectations
- [ ] Decide whether payment pipeline should pass a durable payout key into deposit

## Future

- [ ] Durable payout / idempotency keys from payment pipeline (true at-most-once)
- [ ] Multi-currency payables if content needs more than `modularjobs:coin`
- [ ] Optional Vault adapter as secondary soft-depend (only if operators demand)

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 1.1.0 | Vault economy + hard-fail when required | Production money path |
| 2026-08-08 | Replace Vault with Mint ledger | Align with ain craft Mint; richer ledger |

## Open questions

- [ ] Should `economy.required` default stay `true` for all distributions?
- [ ] Currency id fixed to `modularjobs:coin` or config-driven?
