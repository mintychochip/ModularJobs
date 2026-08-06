# Database schema ownership

## Rule

| Store | Who creates tables? | Who only connects? |
|-------|---------------------|--------------------|
| **SQLite** (local file) | Plugin on connect (`sql/sqlite.sql`) | — |
| **Postgres / MySQL / MariaDB** | Ops / CI / script **once** | Plugin + `jobs-session-api` |

The **game process and REST process never run DDL** against remote databases. That is intentional: multi-instance servers, least privilege, and reviewable migrations do not belong in `onEnable` / `main`.

## Source of truth

- Postgres: `jobs-core/src/main/resources/sql/postgres.sql`
- SQLite: `jobs-core/src/main/resources/sql/sqlite.sql`

## Provision Postgres

```bash
# Local / CI
export DATABASE_URL=postgres://user:pass@host:5432/modularjobs
./scripts/apply-postgres-schema.sh

# Or
psql "$DATABASE_URL" -f jobs-core/src/main/resources/sql/postgres.sql
```

Then point the plugin and API at that database:

```yaml
# database.yml
payable:
  type: postgres
  jdbc-url: jdbc:postgresql://host:5432/modularjobs
  username: modularjobs
  password: secret
```

```bash
# jobs-session-api — connect only, no AUTO_MIGRATE
export DATABASE_URL=postgres://user:pass@host:5432/modularjobs
cargo run --release
```

## Fail-fast

On remote connect the plugin checks that core tables exist (`SchemaPresence`). If missing, startup fails with a message pointing at `scripts/apply-postgres-schema.sh` — it does **not** create the tables.

## Lab escape hatches removed

`auto-schema: true` on postgres is **ignored** (warning logged). Use the script.

Rust `AUTO_MIGRATE` is removed; tests apply the shared SQL file explicitly.
