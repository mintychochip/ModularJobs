# jobs-session-api

Rust REST service for ModularJobs secure editor sessions stored in PostgreSQL.

## Schema ownership

This process **never creates tables**. Provision once:

```bash
./scripts/apply-postgres-schema.sh
# or
psql "$DATABASE_URL" -f jobs-core/src/main/resources/sql/postgres.sql
```

See `docs/database-schema.md`.

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/healthz` | none | Liveness |
| POST | `/api/v1/sessions` | none | Create session from `EditorPayload` JSON |
| GET | `/api/v1/sessions/{code}` | Bearer / `X-Session-Token` | Session envelope |
| GET | `/api/v1/sessions/{code}/payload` | Bearer / `X-Session-Token` | Raw `EditorPayload` |
| PUT | `/api/v1/sessions/{code}` | Bearer / `X-Session-Token` | Replace payload |

## Run

```bash
export DATABASE_URL=postgres://test:test@127.0.0.1:55432/modularjobs
export BIND_ADDR=127.0.0.1:18787
# schema must already exist
cargo run --release
```

On boot the API only **checks** that `editor_sessions` exists; missing schema → hard error pointing at the apply script.

## Tests

```bash
# tests apply shared sql/postgres.sql themselves (not production boot)
DATABASE_URL=postgres://test:test@127.0.0.1:55432/modularjobs cargo test
```
