# Design: Module layout (`api`, `common`, `paper`, `web`)

**Date:** 2026-08-06  
**Status:** Approved for implementation planning  
**Historical note (2026-08-10):** This approved design is retained for history; use current module documentation for distribution guidance.
**Branch / isolation:** Separate git worktree; include current master WIP on the restructure branch

## Problem

The monorepo layout is inconsistent and boundaries are weak:

| Current path | Issues |
|--------------|--------|
| `jobs-api` | Name is long; **~27 files import Bukkit/Paper** despite being the “public API” |
| `jobs-core` | Name does not say “Paper plugin” |
| `jobs-web` + `jobs-session-api` | Session stack is split; session API sits outside `web` |
| *(missing)* | No pure shared-DTO module for editor/export contracts |

Goals:

1. Rename Gradle/plugin modules to short names: **`api`**, **`common`**, **`paper`**, and unify frontend + session backend under **`web`**.
2. **`paper` is the only module that may depend on Paper/Bukkit.**
3. **`api` remains the third-party / public contract surface** and must contain **no Bukkit/Paper types**.
4. Fold **`jobs-session-api` into `web/session-api`** (Rust crate co-located with the editor and docs).
5. Do the work in a **separate worktree**, carrying **current uncommitted/WIP changes** onto that branch.

Non-goals (unless later expanded):

- Changing Postgres schema ownership or DDL policy
- Redesigning session auth (code + token)
- Renaming Java packages (`net.aincraft` stays)
- Cross-language code generation (OpenAPI/json-schema) for TS/Rust in this pass
- Soft-depend third-party plugin hooks (Vault, McMMO, etc.) beyond what paper already does

## Target layout

```
modularjobs/
  api/                 # pure public API (Java library)
  common/              # pure DTOs / shared value types (Java library)
  paper/               # Paper plugin (shadow jar, runServer, resources)
  web/
    …                  # Astro docs site (current jobs-web root)
    session-editor/    # React secure session editor
    session-api/       # Rust session REST (from jobs-session-api)
  scripts/
  docs/
  settings.gradle.kts  # include("api", "common", "paper")
  AGENTS.md, README.md # updated paths
```

### Gradle project graph

```
common  →  no Paper; no plugin lifecycle
api     →  depends on common only (platform-free)
paper   →  depends on api + common + Paper API (compileOnly) + plugin deps
web     →  not a Gradle project (npm + cargo)
```

| Module | Artifact role | Allowed platform deps |
|--------|---------------|------------------------|
| `common` | Shared DTOs / pure value types | None (JDK + maybe minimal serialization annotations if already used) |
| `api` | Public ModularJobs contract for integrators | None Paper/Bukkit; depends on `common` |
| `paper` | Plugin implementation + optional Bukkit convenience types | Paper, MockBukkit (tests), soft-depends |
| `web` | Docs + editor UI + session API process | Node, Rust, Postgres client in session-api |

### Path renames (git `mv` preferred)

| From | To |
|------|-----|
| `jobs-api/` | `api/` |
| `jobs-core/` | `paper/` |
| `jobs-web/` | `web/` |
| `jobs-session-api/` | `web/session-api/` |

Update all references: `settings.gradle.kts`, `build.gradle.kts`, scripts, CI (`.github/workflows`), README, AGENTS.md, CLAUDE.md notes, docs, cargo paths, session-editor README, schema script paths (`paper/src/main/resources/sql/postgres.sql`).

Gradle project names and preferred coordinates: match folder names (`:api`, `:common`, `:paper`). Shadow jar output should remain discoverable (e.g. `paper/build/libs/paper-all.jar` or keep a stable filename via `archiveFileName` if ops depend on `jobs-core-all.jar` — **prefer explicit `archiveFileName` only if something external hard-codes the old name; otherwise accept new names and document**).

## Responsibility split

### `common`

Pure types shared by more than one layer, especially:

- Editor / session **payload DTOs** used by the plugin export/import path (Java mirror of the TS + Rust contract: `EditorPayload`, jobs/tasks/payables metadata fields)
- Small shared value objects (e.g. stringly job keys, action type keys, decimal amount wrappers) when they are not inherently “service API”

Rules:

- No `org.bukkit.*`, `io.papermc.*`
- No plugin bootstrap, repositories, or command code
- Prefer immutable records / simple POJOs

Session **wire** types in TS (`web/session-editor`) and Rust (`web/session-api`) stay language-local for this pass. `common` owns the **plugin-side** JSON shape so paper export does not invent a fourth schema. Align field names with existing camelCase payload (`version`, `metadata.sessionToken`, etc.).

### `api`

Third-party and in-repo **public contracts** without Bukkit:

- Job / progression / payable / boost **interfaces** rewritten to pure types
- Service facades (`JobService`, profession APIs, etc.) using `UUID` (and pure DTOs) instead of `OfflinePlayer` / `Player`
- Pure **domain events** and/or listener SPI (not `org.bukkit.event.Event`)
- `Bridge` (or equivalent) without `Plugin` / `Bukkit.getServicesManager()` — paper registers the implementation at enable; consumers use a pure static accessor set by paper or ServiceLoader-style registration owned by the library

**Bukkit type substitution guide**

| Remove from api | Replacement |
|-----------------|-------------|
| `OfflinePlayer` / `Player` | `java.util.UUID` (+ optional display name only if required by contract) |
| `Plugin` | omit, or opaque host interface without Bukkit |
| `Material`, `ItemStack`, `Block`, `World`, `Entity`, … | namespaced string keys and/or `common` DTOs |
| `NamespacedKey` | `String` (or a tiny pure key type in `common`) |
| `extends Event` / `HandlerList` / `Cancellable` (Bukkit) | pure event types + cancel flag on the pure type; dispatch SPI in api |

Breaking change: external plugins that compiled against Bukkit types in `jobs-api` must migrate to pure types. Document in CHANGELOG.

### `paper`

Only Gradle module allowed to import Paper/Bukkit:

- Current `jobs-core` implementation (domain wiring, payment, repos, commands, upgrades, listeners)
- Mapping layer: UUID ↔ `OfflinePlayer` / `Player`, string keys ↔ `Material` / etc.
- **Optional** Bukkit event wrappers under e.g. `net.aincraft.paper.event.*` for servers that only know the Bukkit bus — **not** part of the pure `api` contract; depend on `:paper` (or a future thin paper-api jar) to use them
- Resources: `plugin.yml`, SQL, YAML configs
- Tests that need MockBukkit stay here

### `web`

Unified web + session stack:

```
web/
  package.json, astro, src/     # docs site (from jobs-web)
  session-editor/               # React app
  session-api/                  # Rust axum service (from jobs-session-api)
```

- Crate name: prefer `session-api` or `modularjobs-session-api` (update `Cargo.toml` + docs)
- Runtime contract unchanged: Postgres `editor_sessions`, code + token auth, payload versioning
- README / AGENTS paths: `cargo run --manifest-path web/session-api/Cargo.toml`, `cd web/session-editor && npm test`

## Paper isolation enforcement

1. **`api` and `common` build files must not declare `libs.paper.api`** (or MockBukkit).
2. **CI or unit check (recommended in implementation):** fail if sources under `api/src` or `common/src` match `import org.bukkit` / `import io.papermc`.
3. Tests that need a server stay in `paper`.

## Migration strategy (Approach B — ordered commits)

Work happens in a **git worktree** on a branch such as `refactor/module-layout`. **Include current master WIP** on that branch (commit or carry uncommitted files into the worktree before structural moves).

Suggested commit sequence (atomic, green when practical):

1. **Branch + WIP capture** — ensure WIP is committed or applied on the restructure branch so nothing is lost.
2. **Rename modules (mechanical)** — `git mv` jobs-api→api, jobs-core→paper, jobs-web→web; update Gradle includes and `project(":…")` deps; leave Paper deps on `api` temporarily only if required for a green build between commits *or* combine with step 4 if a green intermediate is impossible.
3. **Nest session-api** — `git mv jobs-session-api → web/session-api`; update docs, CI, READMEs, cargo package name if desired.
4. **Add `common`** — empty pure module; move pure DTOs (editor payload Java types first when they exist or are introduced).
5. **Peel Bukkit from `api`** — systematically replace Bukkit types; move Bukkit event classes to paper wrappers or delete in favor of pure events + paper adapters; update paper call sites.
6. **Docs + CI + scripts** — AGENTS.md, README, database docs, apply-schema script paths, workflow paths.
7. **Verification** — `./gradlew :api:test :common:test :paper:test` (and build shadow jar); `cargo test` in `web/session-api`; `npm test` in `web/session-editor` as applicable.

If a fully green tree is impossible mid-peel, prefer fewer larger commits that each compile, over many red commits.

### WIP note

Master may contain in-progress refactors (e.g. interface collapse, exploit protection). Those changes **must ride along** into the worktree branch so the restructure is not based on a stale tree. Resolve conflicts between WIP and renames carefully (`git mv` then re-apply path-sensitive WIP).

## Data flow (session stack — unchanged behavior)

```
Plugin export (payload JSON from paper, DTOs aligned with common)
    → POST web/session-api /api/v1/sessions
    → Postgres editor_sessions
    → web/session-editor ?code=&token=
    → PUT save with Bearer / X-Session-Token
```

## Risk register

| Risk | Mitigation |
|------|------------|
| Large API break for third parties | CHANGELOG; pure types are intentional; optional Bukkit wrappers only in paper |
| WIP + renames conflict | Capture WIP on branch first; prefer `git mv`; fix imports in dedicated commits |
| Missed path references | Grep for `jobs-api`, `jobs-core`, `jobs-web`, `jobs-session-api` after moves |
| api still transitively pulls Paper | Remove paper from api build; CI import grep |
| Shadow jar / plugin name assumptions | Check `plugin.yml`, runServer, any deploy scripts |

## Success criteria

- [ ] Gradle includes only `:api`, `:common`, `:paper`
- [ ] `web/session-api` builds and tests; no top-level `jobs-session-api`
- [ ] `api` and `common` source trees have **zero** `org.bukkit` / `io.papermc` imports
- [ ] `paper` is the only module with Paper API dependency
- [ ] Plugin still builds shadow jar and unit tests pass (MockBukkit tests in paper)
- [ ] Docs (README, AGENTS.md, database-schema paths) match new layout
- [ ] Work done on isolated worktree branch including prior WIP

## Open implementation details (decide during plan/PR, not blockers)

- Exact pure event bus shape (simple listener registry vs records + paper-only Bukkit bridge)
- Whether paper publishes Bukkit wrapper events in v1 of this refactor or defers wrappers
- Crate rename string (`session-api` vs `modularjobs-session-api`)
- Shadow jar final filename
- Whether `common` uses only JDK or allows a small JSON library for DTOs (prefer keep serialization in paper if today Kryo/Gson lives there)

## Approval record

- Module names: `api`, `common`, `paper`, `web` — **approved**
- Session API under `web/session-api` — **approved**
- Full Paper isolation now; `api` is public surface with **no Bukkit objects** — **approved**
- Approach B (ordered migration in worktree) — **approved**
- Include current master WIP on restructure branch — **approved**
