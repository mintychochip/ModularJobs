# Database schema ownership

ModularJobs supports **PostgreSQL only**.

## Rule

| Store | Who creates tables? | Who only connects? |
|-------|---------------------|--------------------|
| **PostgreSQL** | Ops / CI / script **once** | Paper plugin and `web/rest-api` |

The **game process and REST API never run DDL**. That is intentional: multi-instance
servers, least privilege, reviewable migrations, backups, and upgrades do not belong
in `onEnable` or the API request path.

## Source of truth

- `paper/src/main/resources/sql/postgres.sql`

## Provision Postgres

```bash
# Local / CI
export DATABASE_URL=postgres://user:pass@host:5432/modularjobs
./scripts/apply-postgres-schema.sh

# Or
psql "$DATABASE_URL" -f paper/src/main/resources/sql/postgres.sql
```

Then point the plugin at that database:

```yaml
# database.yml
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

Identical `jdbc-url` + `username` sections share one Hikari pool.

## Shared editor session database

The Paper plugin and `web/rest-api` must point at the same PostgreSQL database. The
REST API stores editor payloads in `editor_sessions`; Paper fetches those payloads
through the REST API and applies task changes through its existing repositories.

The plugin does not launch PostgreSQL, manage its process, create a data directory,
or replace operator backups and upgrades. For local development, run PostgreSQL
externally (for example, Docker/Podman) and apply the schema before starting either
process.

## Startup behavior

1. Open Hikari pool to PostgreSQL.
2. Verify required tables exist (`job_progression`, `job_tasks`, …).
3. If any table is missing → **fail enable** with a message pointing at `scripts/apply-postgres-schema.sh`.

## Job and task configuration updates

`YamlJobTaskLoader` imports `job_tasks.csv` only when the `job_tasks` table is
empty. Once task rows exist, startup deliberately skips the import; changing
the packaged CSV does not overwrite live task data. Operators must back up and
apply task changes through the existing editor/repository path or an explicit
reviewed SQL operation. Do not clear a live task table without a backup and
reimport plan.

The `fisher` → `fisherman` job-key rename likewise requires an operator-managed
data update for existing progression, upgrade, task, and payable rows before
deploying the renamed catalog.

SQLite, MySQL, and MariaDB are **not supported**.
