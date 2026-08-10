# Changelog

## Unreleased

## 1.2.0 — PostgreSQL and gathering gates

### Added

- **Profession-gated block breaking**: `block-break-gates` in `config.yml` restricts breaking a material to players at/above a configured profession level (bypass: `modularjobs.bypassblockbreak`).
- **Profession-gated fish catching**: `fish-catch-gates` restricts configured vanilla fish by profession level (bypass: `modularjobs.bypassfishcatch`).

### Breaking

- **Module layout rename**: Gradle modules and tree paths are now `api` / `common` / `paper` / `web` (was `jobs-api`, `jobs-core`, `jobs-web`, `jobs-session-api`).
  - Build: `./gradlew :paper:build` → artifact `paper/build/libs/paper-all.jar` (was `:jobs-core:build` / `jobs-core-all.jar`).
  - Tests: `./gradlew :api:test :common:test :paper:test`.
  - Schema SQL: `paper/src/main/resources/sql/postgres.sql`.
  - Session stack: `web/rest-api` (Rust), `web/session-editor` (React).
- **`api` is pure**: public contracts have **no Paper/Bukkit dependency**. Paper-only types and wiring live in `paper`; shared editor/session DTOs live in `common`.
- Downstream consumers of the old Maven/module coordinates (`jobs-api` / `jobs-core`) must switch to the new module paths and jar name.

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
