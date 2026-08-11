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
# artifact: paper/build/libs/paper-2.0.0-all.jar
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

1. Drop `modularjobs-paper-2.0.0.jar` into `plugins/`.
2. Start once to generate configs under `plugins/ModularJobs/`.
3. Configure database, economy, and permissions (below).
4. Restart or reload after config changes.

### Starter content

The bundled `jobs.yml`, `job_tasks.yml`, `job_tasks.csv`, `fisherman.yml`,
`boost_sources_default.json`, and `upgrade_trees/*.json` files form a generic
starter pack. Replace or extend these examples for each server; their values are
not a fixed progression contract.

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

Money payables are optional. When the Mint plugin and service are available,
ModularJobs uses its reflective ledger bridge. Without a provider, the default
blackhole policy accepts positive economy payables but discards the currency.
Install Mint or select the fail policy when real currency rewards are mandatory.

```yaml
# config.yml
economy:
  required: false
  missing-provider: blackhole # blackhole | fail
```

- `missing-provider: blackhole` (default): keep the server running and discard
  positive money payables when no provider is available.
- `missing-provider: fail`: fail enable when Mint is unavailable.
- `required: true` remains a compatibility shorthand for `fail` when no explicit
  `missing-provider` is set.

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

Admin commands (`/jobs boost`, `/jobs editor`, `/jobs applyedits`) require `modularjobs.admin`.

### Payment rules

```yaml
pay-in-creative: true
pay-while-riding: false
disabled-worlds: []
kill-contribution-cutoff: 0.5
```

### Profession API

ModularJobs always registers its core `ProfessionService` Bukkit service for
dependent plugins. Optional Recipe/Buff/Station/NodeHarvest services remain behind:

```yaml
profession-apis:
  register-bukkit-services: false
```

The public API exposes profession catalog, progression, recipes, buffs, stations,
and resource-node hooks without requiring a separate server-specific progression
pack.

### Gathering interaction gates

ModularJobs owns profession progression, task data, and payment. Server operators
may use their own protection or interaction-gating plugins; cancelled events
receive no payment.

### Integrations and deferred dependency

Mint, mcMMO, Bolt, LWC, Choco, and PlaceholderAPI are optional integrations.
The Craftux-backed UI is the current mandatory UI dependency and remains an
explicit deferred distribution task; the release is not yet a fully standalone
Paper artifact. The external Preferences plugin is not required.


## Version

Plugin and project version: **2.0.0** (see `plugin.yml`, root `build.gradle.kts`, `CHANGELOG.md`).
