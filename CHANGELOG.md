# Changelog

## Unreleased

## 2.0.0 — ModularJobs API and Azoth gathering gates

### Added

- Publish Java 21-compatible `modularjobs-api` and `modularjobs-common` artifacts.
- Always register the core `ProfessionService` Bukkit service for dependent plugins.
- Add Herbalism progression task data and the operator-run data migration.
- Move gathering gate configuration and enforcement to Azoth for block breaking,
  fishing, log stripping, and mature plant harvesting.

### Breaking

- Gathering gates are no longer enforced or configured by ModularJobs. Install and
  configure Azoth for world-interaction enforcement.
- Gate-only ModularJobs API contracts and bypass permissions were removed in the
  2.0.0 API cutover.
- Module layout rename: Gradle modules and tree paths are now `api` / `common` / `paper` / `web` (was `jobs-api`, `jobs-core`, `jobs-web`, `jobs-session-api`).
  - Build: `./gradlew :paper:build` → artifact `paper/build/libs/paper-2.0.0-all.jar` (release asset: `modularjobs-paper-2.0.0.jar`).
  - Tests: `./gradlew :api:test :common:test :paper:test`.
  - Schema SQL: `paper/src/main/resources/sql/postgres.sql`.
  - Session stack: `web/rest-api` (Rust), `web/session-editor` (React).
  - `api` is pure: public contracts have no Paper/Bukkit dependency.

### Database

- **PostgreSQL only**: removed SQLite, MySQL, MariaDB, and Mongo providers.
- Default `database.yml` is Postgres (Hikari). Schema still applied out-of-band via `scripts/apply-postgres-schema.sh`.
- Shared connection pools keyed by `jdbc-url` + `username` (not SQLite file path).

## 1.1.0 — Production-readiness cut

### Security / payments (P0)

- Gate `/jobs boost`, `/jobs editor`, `/jobs applyedits` behind `modularjobs.admin`.
- Remove unrestricted debug `/test` free-item command.
- Wire `PaymentSettings` (`pay-in-creative`, `pay-while-riding`, `disabled-worlds`).
- Expand place→break anti-farm beyond STONE (`exploit-config.yml`, `*` materials).
- Vault economy provider; hard-fail when `economy.required: true` and no provider.
- Package `sqlite-jdbc` in the plugin artifact.
- Modern `plugin.yml` (`api-version`, softdepends, permission tree).

### Data integrity / concurrency (P1)

- Reload progression per payable so multi-XP awards accumulate.
- Write-back flush failure re-queues with max-experience merge (no older-XP clobber).
- Log scheduled write-back flush failures.
- Fix `loadAllForJob` pending-delete keying (job key, not player id).
- Kill multi-damage pays each qualifying **contributor**.
- SQLite WAL + busy_timeout; share one `ConnectionSource` per file path.

### Lifecycle / honesty / ops (P2–P3)

- Disable unregisters Bukkit services + PlaceholderAPI expansion; non-spinning flush wait.
- Multi-DB honesty: SQLite auto-schema; Postgres/MySQL connect-only + ops scripts/docs.
- Profession Bukkit services feature-flagged (`profession-apis.register-bukkit-services`).
- Root README, CI workflow, version alignment to **1.1.0**.
