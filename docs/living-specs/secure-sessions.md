# Secure sessions (web editor) — Living Spec

> Status: active  
> Last updated: 2026-08-08  
> Owners: modularjobs maintainers

## Intent

Operators export job/task configuration from the Paper plugin, edit it in a
browser, and apply changes back. Sessions are **code (public) + token (secret)**
stored in PostgreSQL via `web/rest-api`; the React editor never uses Bytebin for
the production path. Success: no token in command args or normal access logs;
wrong token cannot overwrite another session; Paper applies through existing
task repositories (cache-correct).

## Boundaries

### In scope

- `web/rest-api` (Rust/Axum): create/get/put session, healthz
- `web/session-editor` (React/Vite): load/save by code + token
- Paper: `RestSessionClient`, local Caffeine handoff (`EditorSessionStore`),
  `/jobs editor`, `/jobs applyedits <code>`
- Shared DTOs in `common` (plugin-side payload shape)
- Config: `editor.session-api-url`, `web-editor-url`, `session-create-secret`, TTL
- Astro bridge preserving URL fragment (`#token=`)

### Out of scope / non-goals

- Bytebin (`bytebin.lucko.me`) for production secure editor
- Plugin launching Postgres or REST process
- REST writing `job_tasks` directly (Paper apply owns task mutations)
- Token as a Minecraft command argument

## Invariants

- **Server-minted token is authoritative** on create; ignore client-supplied payload token for auth.
- Session URL: `?code=` in query, `#token=` in fragment.
- Local Paper mapping is **handoff only** (code → token, owner, expiry) — not a second payload store.
- Apply requires mapping **owned by** the executing player; success removes mapping; failure retains for retry.
- GET/PUT require Bearer / `X-Session-Token`; wrong/missing → 401.
- Create may require `X-Create-Secret` when configured.
- Shared Postgres; schema out-of-band; REST never DDL.

## Implementation guidance

- Paper client: Java `HttpClient`; status-aware errors.
- Export → REST create → store local handoff → show URL.
- Apply → local ownership check → REST fetch → repository save/delete → drop handoff.
- Payload contract: version, metadata, jobs map, registered action/payable types — keep Java/TS/Rust field names aligned.
- React base URL: `VITE_SESSION_API_URL` (default `http://127.0.0.1:18787`).
- Tests: Paper store/client/service; Rust session auth/expiry; React credential loading.

### Explicit do-nots

- Do not log tokens in command feedback or standard access paths if avoidable.
- Do not reintroduce Bytebin for the secure path.
- Do not put durable session payload authority in Caffeine.

## Current

- [x] Rust REST API on Postgres `editor_sessions` (create/get/payload/save)
- [x] React session-editor + token auth headers
- [x] Paper REST client + EditorConfig REST settings
- [x] Local code→token handoff store with ownership/expiry
- [x] Wire export/apply through REST (Bytebin path removed from Paper)
- [x] Config/docs: external REST + Postgres dependency
- [ ] Final polish: any remaining operator docs / wiki pages still mentioning Bytebin for this path

### Current notes

Design + plan: `docs/superpowers/specs/2026-08-08-rest-editor-session-cutover-design.md`,
`docs/superpowers/plans/2026-08-08-rest-editor-session-cutover.md`.
Implementation largely landed on master (recent commits). Treat remaining open
plan steps as verification/docs unless tests fail.

## Next

- [ ] End-to-end smoke checklist documented for ops (export → edit → save → apply)
- [ ] Optional: require `SESSION_CREATE_SECRET` in non-loopback deploy docs as default recommendation

## Future

- [ ] Session list/admin revoke UI
- [ ] Multi-user collaborative editing locks
- [ ] Cross-language generated types from one schema

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08 | Postgres editor_sessions + Rust API | Durable, auditable, no Bytebin trust |
| 2026-08-08 | Token in URL fragment; code in query | Reduce token in server access logs |
| 2026-08-08 | Paper local handoff, not second payload cache | Restart-safe semantics without dual authority |
| 2026-08-08 | Apply via job task repository | Correct cache invalidation |

## Open questions

- [ ] Default hosted `web-editor-url` ownership/deploy pipeline long-term?
