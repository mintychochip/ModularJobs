# Database schema ownership

ModularJobs supports **PostgreSQL only**.

## Rule

| Store | Who creates tables? | Who only connects? |
|-------|---------------------|--------------------|
| **PostgreSQL** | Ops / CI / script **once** | Plugin |

The **game process never runs DDL**. That is intentional: multi-instance servers, least privilege, and reviewable migrations do not belong in `onEnable`.

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

## Startup behavior

1. Open Hikari pool to PostgreSQL.
2. Verify required tables exist (`job_progression`, `job_tasks`, …).
3. If any table is missing → **fail enable** with a message pointing at `scripts/apply-postgres-schema.sh`.

SQLite, MySQL, and MariaDB are **not supported**.
