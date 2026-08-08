# Persistence — Living Spec

> Status: active  
> Last updated: 2026-08-08  
> Owners: modularjobs maintainers

## Intent

Own player/job state and editor sessions in a single operator-managed PostgreSQL
database. The game plugin and REST API **never create tables**. Success: missing
schema fails fast with an actionable message; multi-instance servers share one
reviewed schema; backups and upgrades stay outside `onEnable`.

## Boundaries

### In scope

- DDL source of truth: `paper/src/main/resources/sql/postgres.sql`
- Apply path: `./scripts/apply-postgres-schema.sh` or `psql -f …`
- HikariCP pools keyed by `jdbc-url` + `username` (`database.yml`)
- Repository pattern + domain mappers for progression, tasks, boosts, upgrades
- Schema presence checks on connect (`SchemaPresence` / REST `require_table`)
- Shared `editor_sessions` table for Paper + `web/rest-api`

### Out of scope / non-goals

- SQLite / MySQL / MariaDB / Mongo providers
- In-process DDL, Flyway/Liquibase inside the plugin or API
- Plugin launching or provisioning a Postgres process
- Embedding DB files under the plugin data folder for production

## Invariants

- **Connect-only at runtime.** `SchemaPolicy` ignores `auto-schema`; no CREATE.
- **Fail-fast** if required tables are missing (plugin enable / API boot).
- **One shared Postgres** for Paper progression data and editor sessions when
  using the secure editor stack.
- Lab/integration tests may apply the SQL file explicitly; production paths stay connect-only.
- NUMERIC for money/XP amounts; no float for durable currency/experience storage.

## Implementation guidance

- Config sections: `payable`, `timed-boost`, `upgrades` in `database.yml` — identical
  credentials share one pool.
- Repositories own SQL; services stay free of JDBC.
- Write-back / flush paths must not clobber newer XP (max-experience merge on re-queue).
- Cache invalidation for job tasks lives on repository save/delete; REST does not write `job_tasks`.
- Docs: `docs/database-schema.md` must stay consistent with this catalog.

### Explicit do-nots

- Do not reintroduce multi-dialect `ConnectionSource` factories for SQLite/MySQL.
- Do not CREATE TABLE from Java or Rust production code paths.
- Do not store session **payload** authority in Paper-local caches (handoff only).

## Current

- [x] Postgres-only remote stack; other dialects removed
- [x] Shared SQL file + apply script
- [x] Schema presence fail-fast on plugin and REST
- [x] Tables: progression, archive, tasks/payables, timed boosts, player_upgrades, editor_sessions
- [x] `player_upgrades.node_levels` column for skill-tree v2 state
- [x] Repository pattern + Hikari for Paper stores

### Current notes

See `docs/database-schema.md`. REST and Paper must point at the same DB for editor apply flow.

## Next

- [ ] Any schema changes required by Mint economy (currency id / ledger correlation) — only if payout durability needs a durable payout id column
- [ ] Keep SQL file, apply script, and SchemaPresence table lists in lockstep when tables change

## Future

- [ ] Formal migration versioning beyond monolithic `postgres.sql` if multi-env drift becomes painful
- [ ] Read replicas / reporting off primary (not needed yet)

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08 | Drop SQLite/MySQL/MariaDB | Ops simplicity; one dialect |
| 2026-08 | Ops-owned DDL only | Multi-instance, least privilege |
| 2026-08 | Shared DB for editor sessions + game data | Single ops surface; apply path reuses job_tasks repos |

## Open questions

- [ ] Introduce ordered migration files vs keep single SQL snapshot?
