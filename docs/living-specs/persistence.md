# Persistence

## Intent

Own player/job state and editor sessions in a single operator-managed MySQL 8
database. The game plugin and REST API **never create tables**. Success: missing
schema fails fast with an actionable message; multi-instance servers share one
reviewed schema; backups and upgrades stay outside `onEnable`.

## Boundaries

- DDL source of truth: `paper/src/main/resources/sql/mysql.sql`
- Apply path: `./scripts/apply-mysql-schema.sh`
- HikariCP pools keyed by `jdbc-url` + `username` (`database.yml`)
- Repository pattern + domain mappers for progression, tasks, boosts, upgrades
- Schema presence checks on connect (`SchemaPresence` / REST `require_table`)
- Rust REST session authority in `editor_sessions`

## Non-goals

- SQLite / PostgreSQL / MariaDB / Mongo providers
- In-process DDL, Flyway/Liquibase inside the plugin or API
- Plugin launching or provisioning a MySQL process
- Embedding DB files under the plugin data folder for production

## Invariants

- **Fail-fast** if required tables are missing (plugin enable / API boot).
- **One shared MySQL** for Paper progression data and editor sessions when using the
  secure editor stack.
- Lab/integration tests may apply the SQL file explicitly; production paths stay
  connect-only.
- DECIMAL for money/XP amounts; no float for durable currency/experience storage.
- Indexed and key-bearing identifiers use bounded VARCHAR columns in the MySQL schema.

## Explicit do-nots

- Do not reintroduce multi-dialect `ConnectionSource` factories.
- Do not CREATE TABLE from Java or Rust production code paths.
- Do not store session **payload** authority in Paper-local caches (handoff only).

## Current

- [x] MySQL-only remote stack; other dialects removed
- [x] Shared SQL file + apply script
- [x] Schema presence fail-fast on plugin and REST
- [x] Tables: progression, archive, tasks/payables, timed boosts, player_upgrades, editor_sessions

## Next

- [ ] Formal migration versioning beyond monolithic `mysql.sql` if multi-env drift becomes painful
- [ ] Read replicas / reporting off primary (not needed yet)

## Decisions log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-08 | MySQL 8 only, connect-only schema | One dialect, broad operator availability, least privilege |
| 2026-08 | Ops-owned DDL only | Multi-instance, least privilege |
| 2026-08 | Shared DB for editor sessions + game data | Single ops surface; apply path reuses job_tasks repos |
