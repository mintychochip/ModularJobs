# ModularJobs — Living Spec (product / platform)

> Status: active  
> Last updated: 2026-08-21
> Owners: modularjobs maintainers

## Intent

Extensible job progression for PaperMC servers: players join jobs, perform
actions, earn experience and payables, unlock skill trees, and (optionally)
face profession-gated world interactions. Success looks like a maintainable
monorepo where `api` stays pure, `paper` owns Bukkit, and the secure web editor
stack shares one operator-provisioned MySQL 8 database without either
process running DDL.

## Boundaries

### In scope

- Paper plugin (`paper`) for Minecraft 1.21.x / Paper 26.2
- Pure public contracts (`api`) and shared editor DTOs (`common`)
- Secure session editor (`web/session-editor` + `web/rest-api`)
- Operator docs / Astro wiki under `web/`
- Out-of-band schema apply (`scripts/apply-mysql-schema.sh`, `paper/.../sql/mysql.sql`)

### Out of scope / non-goals

- Plugin-owned database process or boot-time `CREATE TABLE`
- SQLite / PostgreSQL / MariaDB support
- Guice or other DI frameworks (manual composition root only)
- Using `bytebin.lucko.me` for the production secure editor path
- Cross-language OpenAPI codegen for TS/Rust (unless promoted later)

## Invariants

- **MySQL 8 only.** Runtime processes connect and verify; ops apply DDL.
- **`api` has zero Paper/Bukkit dependencies.** Paper-only types stay in `paper`.
- **Composition root:** `PluginContext` + package `*Wiring` — no Guice.
- **Atomic commits:** one logical change per commit when landing work.
- **Admin-sensitive commands** (`boost`, `editor`, `applyedits`, level/exp) require
  `modularjobs.admin` (or more specific nodes where defined).
- **Version alignment:** plugin.yml, root Gradle version, CHANGELOG stay in sync.

## Implementation guidance

### Module seams

| Module | Role | Allowed deps |
|--------|------|----------------|
| `api` | Public contracts for integrators | JDK + `conditions-api`; no Paper |
| `common` | Shared DTOs + JSON codecs (editor, conditions) | JDK + Gson; may depend on `api` |
| `paper` | Plugin impl + shadow jar | api, common, Paper, Craftux, databag, optional soft-depends |
| `web/*` | Docs, React editor, Rust REST | Node / Rust / MySQL client |

### How to build here

- Prefer existing patterns (repository + domain mapper, wiring classes, Messages).
- Commands: Paper Brigadier; themed text via `dev.mintychochip.util.Messages` (not Mint).
- Tests: JUnit 5; MockBukkit for Bukkit-touching tests; live MySQL for SQL repo tests
  (`MODULARJOBS_TEST_MYSQL_*` or `localhost:13306`; skip when unavailable).
- Static analysis: `./gradlew check`; fail-on-findings with `-Pquality.fail=true`.
- Do not invent parallel TODO markdown that diverges from living-spec horizons.

### Explicit do-nots

- Do not reintroduce Guice modules or DDL-in-process.
- Do not put Bukkit types in `api`.
- Do not treat `docs/superpowers/plans/*` checkboxes as the only progress surface —
  mirror durable state into living specs.

## Current

Shipped platform surface still “active capability” for agents:

- [x] Module layout: `api` / `common` / `paper` / `web`
- [x] Guice removed; `PluginContext` + `*Wiring` composition
- [x] Java toolchain 25, Paper/MockBukkit 26.2, Gradle multi-module
- [x] Git hooks + CI (Java check + shadow jar, rest-api, session-editor)
- [x] Root README / AGENTS.md document operator + agent paths
- [x] Java domain package `dev.mintychochip` (`api`, `common`, `paper`; was `net.aincraft`)
- [ ] Living specs adopted and kept honest across domains (this catalog)

### Current notes

The general Paper distribution hardening is landed: Mint is an optional
reflective economy adapter with a blackhole default, Preferences is local-only,
the editor is opt-in, and Craftux remains the explicitly deferred UI dependency.

Related one-shot docs: `docs/superpowers/specs/2026-08-06-module-layout-design.md`.

## Next

- [ ] Keep living-spec horizons in sync when domain work ships
- [ ] Align any remaining operator docs with MySQL-only persistence, optional

## Future

- [ ] Cross-language payload schema single source (OpenAPI / json-schema) if drift hurts
- [ ] Central config reload path (today: load at enable; no full hot-reload story)
- [ ] Profession Bukkit services beyond stubs when consumers need them

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08 | Manual wiring, no Guice | Simpler graph, fewer transitive deps |
| 2026-08 | MySQL 8 only, connect-only schema | Multi-instance, least privilege, reviewable migrations |
| 2026-08 | Secure editor via REST + MySQL, not Bytebin | Token ownership, shared durable store |
| 2026-08-10 | Optional reflective Mint adapter + blackhole default | Base Paper builds stay independent of Mint while currency-required servers can fail fast |
| 2026-08-21 | Rename Java packages to `dev.mintychochip` | Align plugin bootstrap, shadow jar, and published API coordinates |

## Open questions

- [ ] How aggressively to split product-level horizons vs only domain files once mint + sessions stabilize?
- [ ] Public release cadence / versioning for integrators of `api`?
