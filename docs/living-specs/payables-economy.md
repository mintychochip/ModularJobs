# Payables & economy — Living Spec

> Status: active  
> Last updated: 2026-08-10
> Owners: ModularJobs maintainers

## Intent

Job tasks award typed **payables** (experience, money, …). Money deposits go
through an **EconomyProvider** bridge so the payment pipeline stays independent
of a specific ledger implementation. Servers without an economy provider remain
usable by default; operators who require real currency can select fail-fast
startup behavior.

## Boundaries

### In scope

- `EconomyProvider` contract and factory selection
- Optional Mint ledger bridge (`MintEconomyProvider`) loaded reflectively
- `BlackholeEconomyProvider` fallback for missing providers
- Payable wiring and experience bar UX helpers
- `economy.required` compatibility and `economy.missing-provider` policy
- Optional Bukkit soft-depend on Mint

### Out of scope / non-goals

- Implementing a full economy/ledger inside ModularJobs
- Treating a missing provider as a payment-time exception by default
- Durable payout identifiers in the current payment pipeline
- Coupling messages or chat theming to an economy provider

## Invariants

- Mint has zero compile-time dependency surface; all Mint types are resolved by
  the optional adapter at runtime.
- Mint is resolved lazily per deposit because its Bukkit service may register
  asynchronously.
- When `economy.missing-provider: blackhole` is selected, positive currency
  payables return success without changing a balance; invalid amounts return
  false.
- When `economy.missing-provider: fail` is selected, missing Mint fails plugin
  wiring with an actionable configuration message.
- `economy.required: true` maps to `fail` only when no explicit
  `economy.missing-provider` value is present.
- Deposit uses fresh idempotency keys per call today; a timeout has unknown
  outcome and must not trigger an automatic retry.
- Currency/account namespaces for ModularJobs are explicit
  (`modularjobs:…`); do not invent ad-hoc strings in random call sites.

## Implementation guidance

- Factory: `EconomyProviderFactory` — prefer the optional Mint bridge, then apply
  the configured missing-provider policy.
- Package: `net.aincraft.payable`; wire via `PayableWiring`.
- Tests: unit-test factory policy and fallback behavior; use MockBukkit/service
  registration where a live Bukkit service is needed.
- Keep the payment pipeline calling `EconomyProvider.deposit` only — no direct
  ledger calls outside the provider.
- Keep blackhole logging at provider selection, never once per reward.

### Explicit do-nots

- Do not capture Mint eagerly at enable if that races service registration.
- Do not treat deposit failures as XP rollback unless atomic multi-currency
  payouts become an explicit product decision.
- Do not add a provider-specific dependency to the pure `api` module.

## Current

- [x] `EconomyProvider` abstraction + factory
- [x] Reflective Mint adapter with no Mint compile dependency
- [x] Default blackhole fallback and explicit fail policy
- [x] Local preferences service has no external Preferences dependency
- [x] Factory, fallback, and Mint-absence tests

### Current notes

Mint failures return false and are logged by the adapter. The payment pipeline
does not retry after an unknown outcome.

## Next

- [ ] Add a durable payout identifier if true at-most-once payment semantics
  become a requirement.
- [ ] Document additional provider adapters only when an operator need exists.

## Future

- [ ] Durable payout / idempotency keys from the payment pipeline
- [ ] Multi-currency payables if content needs more than `modularjobs:coin`

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-10 | Mint is an optional reflective adapter | Base Paper builds must not resolve Mint |
| 2026-08-10 | Blackhole is the default missing-provider policy | Experience-only/development servers start safely |
| 2026-08-10 | Explicit `fail` policy remains available | Currency-required servers need startup safety |

## Open questions

- [ ] Should the currency id become config-driven if multiple ledgers are supported?
