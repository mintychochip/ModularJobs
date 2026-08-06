# ModularJobs — agent notes

PaperMC job progression plugin (Java 21 / 25 toolchain, Gradle multi-module) plus
Postgres-backed session API (Rust) and React secure session editor.

## Modules

| Path | Role |
|------|------|
| `api` | Pure public contracts (no Paper) |
| `common` | Shared DTOs (editor payload, …) |
| `paper` | Paper plugin implementation (shadow jar) |
| `web` | Docs + session-editor + session-api |
| `scripts/` | Out-of-band ops helpers (schema apply) |

Build plugin: `./gradlew :paper:build` (shadowJar → `paper/build/libs/paper-all.jar`).  
Unit tests: `./gradlew :api:test :common:test :paper:test`.  
Rust API: `cd web/session-api && cargo test` / `cargo run --release`.  
React editor: `cd web/session-editor && npm test && npm run build`.

## Database schema ownership (important)

**PostgreSQL only. The Java plugin and Rust API never create tables.**

| Store | Who applies DDL | Runtime process |
|-------|-----------------|-----------------|
| Postgres | Ops / CI / script **once** | Connect + verify only |

- Source of truth: `paper/src/main/resources/sql/postgres.sql`
- Apply: `./scripts/apply-postgres-schema.sh` or  
  `psql "$DATABASE_URL" -f paper/src/main/resources/sql/postgres.sql`
- Policy: `SchemaPolicy` — never runs DDL in-process (`auto-schema` is ignored)
- Fail-fast: `SchemaPresence` on connect; missing tables → hard error, no CREATE
- Details: `docs/database-schema.md`

Do **not** reintroduce boot-time `CREATE TABLE` or SQLite/MySQL/MariaDB support.
Lab tests may apply the shared SQL file explicitly; production paths must stay connect-only.

## Secure session stack

```
Plugin export (payload JSON)
    → POST web/session-api /api/v1/sessions  (or create via API)
    → Postgres table editor_sessions
    → React session-editor loads by ?code=&token=
    → PUT save with Bearer / X-Session-Token
```

- Payload contract: version, metadata.sessionToken, jobs map, registered action/payable types  
  (`paper` editor JSON, `web/session-api` models, `web/session-editor` types; shared DTOs in `common`)
- Auth: session **code** (public) + **token** (secret). Wrong/missing token → 401; no cross-session overwrite
- React client base URL: `VITE_SESSION_API_URL` (default `http://127.0.0.1:18787`)
- **Do not** use `bytebin.lucko.me` for the production secure editor path  
  (`web/session-editor` + Rust API). Legacy Vue `bytebin.ts` is deprecated for that path.
- Plugin in-game export may still mention bytebin historically; full plugin cutover is separate work

### API env

```bash
export DATABASE_URL=postgres://user:pass@host:5432/modularjobs
export BIND_ADDR=127.0.0.1:18787
# schema must already exist
cargo run --release --manifest-path web/session-api/Cargo.toml
```

### Plugin remote DB (`database.yml`)

```yaml
payable:
  type: postgres
  jdbc-url: jdbc:postgresql://host:5432/modularjobs
  username: modularjobs
  password: secret
```

## Architecture notes (core)

- Composition root: `PluginContext` + package `*Wiring` (no Guice)
- Repository pattern + HikariCP against PostgreSQL only
- Commands: Paper Brigadier; themed text via `net.aincraft.util.Messages` (not Mint)
- Tests: JUnit 5; MockBukkit for Bukkit-touching tests; repository SQL tests need a live PG  
  (`MODULARJOBS_TEST_PG_*` or default `localhost:55432`; skipped when unavailable)

## Agent rules

- Senior Java: SOLID, DRY, SRP; concise feedback
- Prefer existing patterns over new frameworks
- Do not invent DDL-in-process for remote DBs
- Prefer `claude-context` / codebase search when locating code; Context7 for library docs when generating setup/config against external APIs
- Atomic commits: one logical change per commit when landing work

## Working log (high level)

- Guice removed (2026-08): manual wiring
- MockBukkit 26.2 / Paper 26.2 / Java toolchain 25
- Postgres DDL + fidelity tests; connect-only remote schema ownership
- Module layout rename: `jobs-api`→`api`, `jobs-core`→`paper`, `jobs-web`/`jobs-session-api`→`web/*`
- `web/session-api` (Rust) + React `web/session-editor` for secure sessions
