# REST-backed web editor session cutover

- **Date:** 2026-08-08
- **Status:** Approved design
- **Scope:** Replace Paper's Bytebin editor-session path with the PostgreSQL-backed `web/rest-api` path.

## Decision

The Paper plugin MUST NOT launch, provision, or own a PostgreSQL server. PostgreSQL is an operator-managed dependency shared by Paper and `web/rest-api`.

Operators/CI apply `paper/src/main/resources/sql/postgres.sql` out of band. The plugin and REST API connect to the provisioned database; neither process runs DDL. Local development uses an external Docker/Podman/PostgreSQL service.

## Current problem

The React editor and Rust API already use `editor_sessions` in PostgreSQL, but Paper still wires `BytebinClient` and validates sessions through an in-process Caffeine cache. Consequently, `/jobs editor` creates Bytebin sessions and `/jobs applyedits` reads Bytebin sessions, while browser saves go to the REST API.

## Architecture

### Paper REST client

Add a focused Paper REST client around Java `HttpClient` with operations for:

- create session (`POST /api/v1/sessions`, including optional `X-Create-Secret`);
- fetch payload (`GET /api/v1/sessions/{code}/payload` with the session token).

The API-generated `code`, `token`, and `expiresAt` are authoritative. Paper MUST use the token returned by the create response rather than relying on the payload's client-supplied `metadata.sessionToken`.

### Local session handoff

Keep a short-lived Paper-local Caffeine mapping keyed by session code:

```text
session code -> session token, player UUID, creation/expiry metadata
```

This mapping is only an authorization/token handoff for `/jobs applyedits`; PostgreSQL remains the durable session-payload store. It is not a second payload cache.

- Export stores the mapping after REST creation succeeds.
- Apply requires a mapping owned by the executing player.
- Successful apply removes the mapping.
- Missing mappings produce a clear re-export/restart message.
- Plugin restart invalidates the handoff, matching the existing in-memory editor-session behavior.

The token MUST NOT be required as a Minecraft command argument because command/chat/console logs can expose it.

### Editor URL

Generate a URL with the public code in the query and the secret token in the fragment:

```text
<web-editor>/session?code=<code>#token=<token>
```

The Astro session bridge MUST preserve the fragment when it opens the React editor. This keeps the token out of the initial HTTP request and normal server access logs.

### Applying edits and repository cache

`/jobs applyedits <code>` fetches the payload from REST, then uses the existing `RelationalJobTaskRepositoryImpl`:

- `save()` commits and updates its read cache;
- `delete()` commits and invalidates its read cache;
- export reads task records from PostgreSQL.

No cross-process cache invalidation is required because REST only modifies `editor_sessions`; it does not write `job_tasks`.

The apply operation retains the existing per-task save/delete behavior. The local session mapping is removed only after a successful import.

## Configuration

Replace Bytebin-specific editor configuration with REST settings:

```yaml
editor:
  enabled: true
  session-api-url: http://127.0.0.1:18787
  web-editor-url: https://modular-jobs.vercel.app/editor
  session-create-secret: change-me
```

The create secret is optional for loopback development and recommended for a remotely reachable REST API. Paper's local handoff expiry MUST not outlive the REST session expiry. The default values/documentation will match the REST API's configured session lifetime.

## Error handling

- REST create/fetch failures become actionable editor command errors.
- Missing local code mapping reports that the session was created before a plugin restart or has expired and should be re-exported.
- Wrong-owner apply is rejected before fetching/applying data.
- REST expiry is reported distinctly from invalid credentials where the API status permits.
- A failed/partial import retains the local mapping for retry; successful import removes it.

## Verification

1. Paper tests cover REST request/response handling, code/token ownership mapping, missing mappings, and apply behavior through the repository.
2. Rust REST tests continue to cover session creation, token authorization, expiry, and payload replacement.
3. React tests/build cover code-plus-fragment credential loading and REST save behavior.
4. Smoke test the complete path against provisioned PostgreSQL: export, open URL, edit/save, apply, then read the changed task through Paper's repository/service path.
