Codebase:

This codebase is for ModularJobs - an extensible job progression system plugin for Minecraft PaperMC servers.

## Project Overview
- **Type**: PaperMC/Spigot plugin for Minecraft 1.21.11 / Paper 26.2
- **Language**: Java 21 / 25 toolchain
- **Build System**: Gradle (Kotlin DSL)
- **Structure**: Multi-module project (`api`, `common`, `paper`, `web`)

## Modules

| Path | Role |
|------|------|
| `api` | Pure public contracts (no Paper) |
| `common` | Shared DTOs (editor payload, …) |
| `paper` | Paper plugin implementation (shadow jar) |
| `web` | Docs + session-editor + rest-api |

## Core Features
- Job progression system with leveling
- 40+ action types (block placement/breaking, crafting, killing entities, etc.)
- Configurable reward system (Payables)
- Timed & item-based boost system
  - MySQL 8 only (connect-only schema ownership)
- PlaceholderAPI integration
- Job upgrade system
- Third-party plugin hooks (McMMO, Vault, LWC, Bolt)

## Technology Stack
- **Framework**: PaperMC API, Adventure/Kyori text components
- **Wiring**: Manual composition root (`PluginContext`) — no DI framework
- **Serialization**: Kryo 5.6.2
 - **Database**: HikariCP (connection pooling), MySQL 8 only
- **Caching**: Caffeine
- **Math**: exp4j
- **Documentation**: Astro + Starlight (`web/`)
- **Session stack**: Rust `web/rest-api` + React `web/session-editor`

## Key Modules & Components

### api
- Pure public contracts (no Paper dependency)
- `ActionTypes.java` - Predefined action types
- `Job.java`, `JobTask.java` - Core abstractions
- `Payable.java` - Reward abstraction
- `Boost.java` - Boost system abstraction
- `Bridge.java` - Plugin interface

### common
- Shared DTOs (editor payload, session contract types, …)

### paper
- **Domain Layer**: Job/JobProgression/JobTask/Payable services with mappers
- **Payment**: `BoostEngineImpl`, `TimedBoostDataService` - boost calculation
- **Repository**: Data persistence with repository pattern
  - `JobRepository`, `JobProgressionRepository`, `TimedBoostRepository`
  - `ConnectionSourceFactory`, `HikariConfigProvider` - DB config
- **Service Layer**: `JobService`, `ProgressionService`
- **Config**: Yaml configuration under plugin resources
- **Serialization**: `BinaryInImpl`, `BinaryOutImpl`, Kryo codecs
- **Commands**: Command framework with Paper/Brigadier
- **Upgrades**: `JobUpgradeNode`, `UserUpgradeRepository`
- **Placeholders**: PlaceholderAPI expansion

### web
- Astro + Starlight docs site
- `web/session-editor` — React secure session editor
 - **web/rest-api** — Rust REST API for editor sessions on MySQL

## Design Patterns
- **Composition root**: `PluginContext` + package `*Wiring` classes construct the object graph
 - **Repository Pattern**: Abstraction over MySQL
- **Domain Mapping**: DomainMapper<Domain, Record> for model conversion
- **Sealed Types**: Type-safe variants (e.g., `Target` for boost targets)
- **Service Layer**: Business logic separation

## Database Configuration
- Configured in `database.yml` (plugin data folder; template under `paper/src/main/resources/`)
 - **MySQL 8 only** — connection pooling with HikariCP
 - **Schema ownership:** MySQL is **connect-only** — apply
   `paper/src/main/resources/sql/mysql.sql` via `scripts/apply-mysql-schema.sh`
   (never CREATE from the game/API process). See `docs/database-schema.md` and root `AGENTS.md`.
- Also: `web/rest-api` (Rust) + `web/session-editor` (React) for secure editor sessions

## Development
- Build: `./gradlew :paper:build` → shadow jar at `paper/build/libs/paper-all.jar`
- Tests: `./gradlew :api:test :common:test :paper:test`
- Session API: `cd web/rest-api && cargo test`
- Session editor: `cd web/session-editor && npm test && npm run build`
- Test server: `./gradlew :paper:runServer` (when configured)
- Server data in `paper/run/` (when runServer used)

Rules:

You are a senior Java software engineer and always follow SOLID, DRY, and SRP principles.

Provide concise feedback, sacrifice grammar for the sake of concision.

You are allowed to implement using multiple papermc-plugin-developer subagents.

Assume the code base is indexed and whenever searching you can only use claude-context search_code to search code base.

Always use claude-context when you need to locate anything in the codebase.
This means you should automatically use claude-context to search the code base using semantic search without having me ask.

Always use context7 when I need code generation, setup or configuration steps, or
library/API documentation. This means you should automatically use the Context7 MCP
tools to resolve library id and get library docs without me having to explicitly ask.

## Working Log

### Guice Binding Fixes (2026-01-04)
- `BoostModule.java`: Added `BoostFactory`, `ConditionFactory`, `PolicyFactory` bindings → `BoostFactoryImpl.INSTANCE`
- `DomainModule.java`: `JobResolver` → `JobResolverImpl` (requires import due to package-private)

### Guice Removal (2026-08-05)
- Removed Google Guice entirely; deleted all `*Module` Guice binders
- Manual composition: `PluginContext`, `DomainWiring`, `PayableWiring`, `PaymentWiring`, `KeyResolvers`
- Replaced Guice transitive Guava with explicit `libs.guava`
- Bootstrap: `PluginContext.create(this)` instead of `Guice.createInjector`

### MockBukkit 26.2 unit tests (2026-08-05)
- `testImplementation` MockBukkit `mockbukkit-v26.2:26.2.0-mj` (vendored from branch `upgrade/v26.2` in `libs/mockbukkit-maven` until Central publishes)
- Paper API catalog: `26.2.build.65-beta`; Java toolchain 25; Gradle wrapper 9.6.1
- Bukkit-touching tests use `MockBukkitSupport` (`MockBukkit.mock`/`unmock`); removed OfflinePlayer Proxy stubs

### Module layout (2026-08)
- Renamed Gradle modules: `jobs-api`→`api`, `jobs-core`→`paper`; added `common`
- Web stack under `web/` (`session-editor`, `rest-api`); pure `api` has no Paper dependency
