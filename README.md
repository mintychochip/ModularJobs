# ModularJobs

Extensible job progression plugin for PaperMC (**26.2** / Java **25**).

## Modules

| Path | Role |
|------|------|
| `api` | Pure public contracts (no Paper) |
| `common` | Shared DTOs (editor payload, …) |
| `paper` | Paper plugin implementation (shadow jar) |
| `web` | Docs + session-editor + session-api |

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
cd web/session-api && cargo test
cd web/session-editor && npm test && npm run build
```

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

Money payables use **Vault** + an economy plugin.

```yaml
# config.yml
economy:
  required: true   # fail enable if Vault economy missing
```

- `required: true` (default): hard-fail enable without Vault economy.
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
| `modularjobs.specialization.bypass` | op | Re-pick pet specialization |

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

### Soft depends

PlaceholderAPI, Vault, mcMMO, Bolt, LWC, JobPets-Core, Choco — optional.

## Version

Plugin and project version: **1.1.0** (see `plugin.yml`, root `build.gradle.kts`, `CHANGELOG.md`).
