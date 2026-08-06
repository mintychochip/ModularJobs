Codebase:

This codebase is for ModularJobs - an extensible job progression system plugin for Minecraft PaperMC servers.

## Project Overview
- **Type**: PaperMC/Spigot plugin for Minecraft 1.21.11
- **Language**: Java 21
- **Build System**: Gradle (Kotlin DSL)
- **Structure**: Multi-module project (jobs-api, jobs-core, jobs-web)

## Core Features
- Job progression system with leveling
- 40+ action types (block placement/breaking, crafting, killing entities, etc.)
- Configurable reward system (Payables)
- Timed & item-based boost system
- Multiple database support (SQLite, MySQL, MariaDB, PostgreSQL)
- PlaceholderAPI integration
- Job upgrade system
- Third-party plugin hooks (McMMO, Vault, LWC, Bolt)

## Technology Stack
- **Framework**: PaperMC API, Adventure/Kyori text components
- **Wiring**: Manual composition root (`PluginContext`) — no DI framework
- **Serialization**: Kryo 5.6.2
- **Database**: HikariCP (connection pooling)
- **Caching**: Caffeine
- **Math**: exp4j
- **Documentation**: Astro + Starlight

## Key Modules & Components

### jobs-api
- `ActionTypes.java` - Predefined action types
- `Job.java`, `JobTask.java` - Core abstractions
- `Payable.java` - Reward abstraction
- `Boost.java` - Boost system abstraction
- `Bridge.java` - Plugin interface

### jobs-core
- **Domain Layer**: Job/JobProgression/JobTask/Payable services with mappers
- **Payment**: `BoostEngineImpl`, `TimedBoostDataService` - boost calculation
- **Repository**: Data persistence with repository pattern
  - `JobRepository`, `JobProgressionRepository`, `TimedBoostRepository`
  - `ConnectionSourceFactory`, `HikariConfigProvider` - DB config
- **Service Layer**: `JobService`, `ProgressionService`
- **Config**: `ConfigurationModule`, `YamlConfiguration`
- **Serialization**: `BinaryInImpl`, `BinaryOutImpl`, Kryo codecs
- **Commands**: Command framework with Paper/Brigadier
- **Upgrades**: `JobUpgradeNode`, `UserUpgradeRepository`
- **Placeholders**: PlaceholderAPI expansion

## Design Patterns
- **Composition root**: `PluginContext` + package `*Wiring` classes construct the object graph
- **Repository Pattern**: Abstraction over relational databases
- **Domain Mapping**: DomainMapper<Domain, Record> for model conversion
- **Sealed Types**: Type-safe variants (e.g., `Target` for boost targets)
- **Service Layer**: Business logic separation

## Database Configuration
- Configured in `database.yml`
- Supports SQLite (file-based) and relational DBs (MySQL/MariaDB/PostgreSQL)
- Connection pooling with HikariCP
- **Schema ownership:** SQLite may bootstrap `sql/sqlite.sql` in-process; remote Postgres is
  **connect-only** — apply `sql/postgres.sql` via `scripts/apply-postgres-schema.sh` (never
  CREATE from the game/API process). See `docs/database-schema.md` and root `AGENTS.md`.
- Also: `jobs-session-api` (Rust) + `jobs-web/session-editor` (React) for secure editor sessions

## Development
- Build: `gradle build` → builds shadowJar
- Test server: `gradle runServer` (Minecraft 1.21.11)
- Test plugins auto-downloaded: Mint, Bolt
- Server data in `jobs-core/run/`

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
