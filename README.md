# ModularJobs

Extensible job progression plugin for PaperMC (**26.2** / Java **25**).

## Modules

| Path | Role |
|------|------|
| `api` | Pure public contracts (no Paper) |
| `common` | Shared DTOs (editor payload, …) |
| `paper` | Paper plugin implementation (shadow jar) |
| `web` | Docs + session-editor + rest-api |

## Build

```bash
./gradlew :paper:build
# artifact: paper/build/libs/paper-all.jar
```

Unit tests:

```bash
./gradlew :api:test :common:test :paper:test
```

Session stack:

```bash
cd web/rest-api && cargo test
cd web/session-editor && npm test && npm run build
```

Git hooks (once per clone):

```bash
./scripts/install-git-hooks.sh
# SKIP_PRECOMMIT=1 git commit ...  # emergency bypass
```

CI: `.github/workflows/ci.yml` — Java 25 + Postgres (`check` + shadow jar), Rust rest-api, React session-editor.

## Operator quick start

1. Drop `paper-all.jar` into `plugins/`.
2. Start once to generate configs under `plugins/ModularJobs/`.
3. Configure database, economy, and permissions (below).
4. Restart or reload after config changes.

### Database (PostgreSQL only)

ModularJobs uses **PostgreSQL only** (no SQLite/MySQL/MariaDB).

1. Provision schema out-of-band (plugin never runs DDL):

```bash
export DATABASE_URL=postgres://user:pass@host:5432/modularjobs
./scripts/apply-postgres-schema.sh
# or: psql "$DATABASE_URL" -f paper/src/main/resources/sql/postgres.sql
```

2. Configure `database.yml` (sections with the same jdbc-url + username share one pool):

```yaml
payable:
  type: postgres
  jdbc-url: jdbc:postgresql://host:5432/modularjobs
  username: modularjobs
  password: secret
  maximum-pool-size: 10

timed-boost:
  type: postgres
  jdbc-url: jdbc:postgresql://host:5432/modularjobs
  username: modularjobs
  password: secret

upgrades:
  type: postgres
  jdbc-url: jdbc:postgresql://host:5432/modularjobs
  username: modularjobs
  password: secret
```

Missing tables → plugin **fails at startup**. See `docs/database-schema.md`.

### Economy

Money payables use the **Mint** ledger plugin (aincraft-org/mint).

```yaml
# config.yml
economy:
  required: true   # fail enable if the Mint economy plugin is missing
```

- `required: true` (default): hard-fail enable without a Mint plugin.
- `required: false`: experience-only servers; economy deposits throw if a task still pays money.

### Permissions

| Node | Default | Purpose |
|------|---------|---------|
| `modularjobs.admin` | op | Level/exp admin, boost, web editor, applyedits |
| `jobs.command.browse` | true | Browse GUI |
| `jobs.command.list` | true | List jobs |
| `jobs.command.stats` | true | Own stats |
| `jobs.command.admin.stats` | op | Others' stats |
| `jobs.command.archive` | true | Own archive |
| `jobs.command.admin.archive` | op | Others' archive |
| `jobs.command.leaveall` | true | Leave all jobs |
| `jobs.command.admin.treeeditor` | op | Upgrade tree editor |
| `modularjobs.bypassblockbreak` | op | Bypass profession-gated block breaking |

Admin commands (`/jobs boost`, `/jobs editor`, `/jobs applyedits`) require `modularjobs.admin`.

### Payment rules

```yaml
pay-in-creative: true
pay-while-riding: false
disabled-worlds: []
kill-contribution-cutoff: 0.5
```

### Profession APIs (stubs)

Azoth-style profession Bukkit services are **off by default**:

```yaml
profession-apis:
  register-bukkit-services: false
```

Set `true` only when integrating consumers that expect `ProfessionService` / related APIs. Station/NodeHarvest may still be stubs.

### Block breaking gates

Restrict breaking a material to a minimum profession level (`block-break-gates` in `config.yml`):

```yaml
block-break-gates:
  diamond_ore: { profession: mining, level: 30 }
```

Players below the level cannot break the block; the event is cancelled with a message. Staff bypass via `modularjobs.bypassblockbreak`.

### Fish catching gates

Restrict a fish item to a minimum profession level (`fish-catch-gates` in `config.yml`):

```yaml
fish-catch-gates:
  cod: { profession: fisherman, level: 1 }
  salmon: { profession: fisherman, level: 10 }
  tropical_fish: { profession: fisherman, level: 20 }
  pufferfish: { profession: fisherman, level: 30 }
```

Below-level or unjoined players do not collect configured fish and receive no fish job payment. Staff bypass via `modularjobs.bypassfishcatch`. Junk and treasure are unaffected.

### Soft depends

Mint, mcMMO, Bolt, LWC, Choco, Preferences — optional (Mint replaces the former Vault soft-depend).

## Version

Plugin and project version: **1.1.0** (see `plugin.yml`, root `build.gradle.kts`, `CHANGELOG.md`).
