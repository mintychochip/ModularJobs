# rest-api (`web/rest-api`)

Rust REST service for ModularJobs secure editor sessions stored in PostgreSQL.

## Schema ownership

This process **never creates tables**. Provision the shared database once:

```bash
./scripts/apply-postgres-schema.sh
# or
psql "$DATABASE_URL" -f paper/src/main/resources/sql/postgres.sql
```

See `docs/database-schema.md`. Paper and this REST process must use the same
PostgreSQL database. The Paper plugin only connects through its existing
`database.yml` Hikari configuration; it never launches PostgreSQL, owns its
data directory, or runs schema DDL.

## Paper editor configuration

Set the plugin's `config.yml` editor endpoint to this process:

```yaml
editor:
  enabled: true
  session-api-url: http://127.0.0.1:18787
  web-editor-url: http://localhost:4321/editor
  session-create-secret: long-random-value
  session-ttl-minutes: 1440
```

When `SESSION_CREATE_SECRET` is set for this API, `editor.session-create-secret`
must contain the same value. The browser receives the public session code in the
URL query and the secret token in the URL fragment.

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
# CORS allow-list (comma-separated). Default = localhost Vite/Astro ports.
# Patterns:
#   exact:  https://editor.example.com
#   glob:   https://*.example.com   |  http://localhost:*
#   host:   *.example.com           (any scheme/port under that host suffix)
#   regex:  re:^https://dev-.+\.example\.com$
#   any:    *   (alone; explicit opt-in — not for production)
export CORS_ALLOW_ORIGINS=https://*.example.com,http://localhost:*,https://app.example.com
```

Hardening built in:

- Server always mints UUID session tokens (client-supplied tokens ignored)
- Constant-time token comparison
- Sliding-window create rate limit
- CORS allowlist with exact / glob / host-suffix / regex patterns (not `*` by default)
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
