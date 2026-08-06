# Changelog

## Unreleased

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
