# ModularJobs — agent notes

PaperMC job progression plugin (Java 21 / 25 toolchain, Gradle multi-module) plus
Postgres-backed REST API (Rust) and React secure session editor.

## Modules

| Path | Role |
|------|------|
| `api` | Pure public contracts (no Paper) |
| `common` | Shared DTOs (editor payload, …) |
| `paper` | Paper plugin implementation (shadow jar) |
| `web` | Docs + session-editor + rest-api |
| `scripts/` | Out-of-band ops helpers (schema apply) |

Build plugin: `./gradlew :paper:build` (shadowJar → `paper/build/libs/paper-2.0.0-all.jar`).
Unit tests: `./gradlew :api:test :common:test :paper:test`.  
Static analysis (Error Prone on compile; Checkstyle/PMD/SpotBugs on `check`):  
`./gradlew check` — reports under `*/build/reports/{checkstyle,pmd,spotbugs}/`.  
Enforce fail-on-findings: `./gradlew check -Pquality.fail=true`.  
Configs: `config/checkstyle/`, `config/pmd/`, `config/spotbugs/`.  
Rust API: `cd web/rest-api && cargo test` / `cargo run --release`.  
React editor: `cd web/session-editor && npm test && npm run build`.

### Git hooks + CI

- **Install hooks** (once per clone): `./scripts/install-git-hooks.sh`  
  Sets `core.hooksPath=.githooks`. Pre-commit runs compile/tests for staged Java,
  `cargo check` for `web/rest-api`, `npm test` for `web/session-editor`.  
  Skip once: `SKIP_PRECOMMIT=1 git commit ...`
- **GitHub Actions**: `.github/workflows/ci.yml`  
  - `java` — JDK 25, Postgres service, `./gradlew check`, shadow jar + report artifacts  
  - `rest-api` — Rust stable + Postgres, `cargo test`  
  - `session-editor` — Node 22, `npm ci && npm test && npm run build`

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
    → POST web/rest-api /api/v1/sessions  (or create via API)
    → Postgres table editor_sessions
    → React session-editor loads by ?code=&token=
    → PUT save with Bearer / X-Session-Token
```

- Payload contract: version, metadata.sessionToken, jobs map, registered action/payable types  
  (`paper` editor JSON, `web/rest-api` models, `web/session-editor` types; shared DTOs in `common`)
- Auth: session **code** (public) + **token** (secret). Wrong/missing token → 401; no cross-session overwrite
- React client base URL: `VITE_SESSION_API_URL` (default `http://127.0.0.1:18787`)
- **Do not** use `bytebin.lucko.me` for the production secure editor path  
  (`web/session-editor` + Rust API). Legacy Vue `bytebin.ts` is deprecated for that path.
- Plugin in-game export may still mention bytebin historically; full plugin cutover is separate work

### API env

```bash
export DATABASE_URL=postgres://user:pass@host:5432/modularjobs
export BIND_ADDR=127.0.0.1:18787
# optional hardening (see web/rest-api/README.md)
# export SESSION_CREATE_SECRET=...
# export CORS_ALLOW_ORIGINS=http://127.0.0.1:5173
# schema must already exist
cargo run --release --manifest-path web/rest-api/Cargo.toml
```

Session ownership: code (public) + token (secret headers). Create is open unless
`SESSION_CREATE_SECRET` is set; GET/PUT require Bearer / `X-Session-Token`.

### Plugin remote DB (`database.yml`)

```yaml
payable:
  type: postgres
  jdbc-url: jdbc:postgresql://host:5432/modularjobs
  username: modularjobs
  password: secret
```

## Living specs (domain catalogs)

Durable design intent + feature horizons live under `docs/living-specs/`.
Read the domain catalog before implementing; flip checkboxes when work ships.
Index: `docs/living-specs/README.md`. One-shot design dumps under
`docs/superpowers/` are historical — horizons in living specs are authoritative.

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
- `web/rest-api` (Rust) + React `web/session-editor` for secure sessions
