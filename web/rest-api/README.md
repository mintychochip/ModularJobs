# rest-api (`web/rest-api`)

Rust REST service for ModularJobs secure editor sessions stored in PostgreSQL.

## Schema ownership

This process **never creates tables**. Provision once:

```bash
./scripts/apply-postgres-schema.sh
# or
psql "$DATABASE_URL" -f paper/src/main/resources/sql/postgres.sql
```

See `docs/database-schema.md`.

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/healthz` | none | Liveness |
| POST | `/api/v1/sessions` | optional `X-Create-Secret` | Create session (server mints token) |
| GET | `/api/v1/sessions/{code}` | Bearer / `X-Session-Token` | Session envelope |
| GET | `/api/v1/sessions/{code}/payload` | Bearer / `X-Session-Token` | Raw `EditorPayload` |
| PUT | `/api/v1/sessions/{code}` | Bearer / `X-Session-Token` | Replace payload |

**Ownership model:** possession of the secret `session_token` for a `session_code` is ownership. There is no user account. Wrong/missing/unknown-code auth attempts return **401** (no existence oracle). Expired sessions with a valid token return **410**. Payload `sessionToken` rewrite on PUT returns **403**.

## Security / env

```bash
export DATABASE_URL=postgres://test:test@127.0.0.1:55432/modularjobs
export BIND_ADDR=127.0.0.1:18787          # default: loopback only
# Optional: require shared secret on create (recommended if not loopback-only)
export SESSION_CREATE_SECRET=long-random-value
# Optional: max session creates per 60s (default 60)
export SESSION_CREATE_RATE_LIMIT=60
# CORS: comma-separated origins. Default = localhost Vite/Astro ports.
# Use * only for local debugging (explicit opt-in; not for production).
export CORS_ALLOW_ORIGINS=http://127.0.0.1:5173,https://editor.example.com
```

Hardening built in:

- Server always mints UUID session tokens (client-supplied tokens ignored)
- Constant-time token comparison
- Sliding-window create rate limit
- CORS allowlist (not `*` by default)
- PUT cannot rewrite `sessionToken` or the stored token column

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
# tests apply shared paper sql/postgres.sql themselves (not production boot)
DATABASE_URL=postgres://test:test@127.0.0.1:55432/modularjobs cargo test
```
