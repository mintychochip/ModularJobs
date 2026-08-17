# MySQL Cutover Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace PostgreSQL with MySQL 8 as ModularJobs' sole relational backend across Paper, the REST API, schema provisioning, tests, CI, and operator documentation.

**Architecture:** Keep the existing JDBC/Hikari and SQLx repository boundaries, but make each implementation MySQL-only. Preserve connect-only schema ownership and fail-fast table verification. Use one canonical MySQL schema with bounded `VARCHAR` key columns, InnoDB, `utf8mb4`, and MySQL-native upsert/query syntax.

**Tech Stack:** Java 21/25, Paper 26.2, HikariCP 5, MySQL Connector/J 9.x, JUnit 5, Rust 2021, SQLx 0.8 MySQL, Tokio, MySQL 8.

## Global Constraints

- PostgreSQL support is removed rather than retained as a second backend.
- Paper and the REST API never execute DDL at runtime.
- MySQL 8 is the supported server baseline; use `com.mysql.cj.jdbc.Driver` and `jdbc:mysql://...` URLs.
- Every indexed or key-bearing string column uses bounded `VARCHAR(...)`; unbounded `TEXT` is not used in primary/unique/foreign-key columns.
- Existing repository APIs, payload formats, expiry behavior, security behavior, and decimal precision remain unchanged.
- SQL stays parameterized; never interpolate user-controlled values.
- Do not leave PostgreSQL dependencies, schema files, scripts, CI services, config examples, or support claims in active repository paths.

---

### Task 1: Convert Paper database type and schema verification

**Files:**
- Modify: `paper/src/main/java/net/aincraft/repository/DatabaseType.java`
- Modify: `paper/src/main/java/net/aincraft/repository/ConnectionSourceFactory.java`
- Modify: `paper/src/main/java/net/aincraft/repository/SchemaPolicy.java`
- Modify: `paper/src/main/java/net/aincraft/repository/SchemaPresence.java`
- Modify: `paper/src/test/java/net/aincraft/repository/DatabaseTypeTest.java`
- Modify: `paper/src/test/java/net/aincraft/repository/DatabaseConfigSectionsTest.java`
- Modify: `paper/src/test/java/net/aincraft/repository/PluginResourcesLifecycleTest.java`

**Interfaces:**
- Produces `DatabaseType.MYSQL`, identifier `mysql`, driver `com.mysql.cj.jdbc.Driver`, and resource lookup `sql/mysql.sql`.
- `DatabaseType.fromIdentifier(null|blank)` returns `MYSQL`; only case-insensitive `mysql` is accepted.
- `SchemaPresence.requireTables(Connection, DatabaseType, List<String>)` remains the public verification entry point.

- [ ] **Step 1: Write the failing tests**

Update `DatabaseTypeTest` to assert `MYSQL`, `mysql`, `MYSQL`, blank/default behavior, MySQL driver/resource, and rejection text naming `mysql`. Update configuration fixtures from `postgres`/`jdbc:postgresql` to `mysql`/`jdbc:mysql`.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```bash
./gradlew :paper:test --tests 'net.aincraft.repository.DatabaseTypeTest' --tests 'net.aincraft.repository.DatabaseConfigSectionsTest' --console=plain
```

Expected: failures because production still exposes only `POSTGRES` and PostgreSQL validation.

- [ ] **Step 3: Implement the minimal MySQL type cutover**

Replace the enum value and parser, remove the PostgreSQL-only factory guard, update schema policy/messages, and make schema presence use MySQL-compatible `information_schema.tables` lookup without `current_schema()`.

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run the same Gradle command. Expected: all selected tests pass.

- [ ] **Step 5: Commit**

```bash
git add paper/src/main/java/net/aincraft/repository paper/src/test/java/net/aincraft/repository
git commit -m "feat: switch paper database type to mysql"
```

### Task 2: Add MySQL schema and convert Paper repository SQL

**Files:**
- Create: `paper/src/main/resources/sql/mysql.sql`
- Delete: `paper/src/main/resources/sql/postgres.sql`
- Modify: `paper/src/main/java/net/aincraft/domain/RelationalJobProgressionRepositoryImpl.java`
- Modify: `paper/src/main/java/net/aincraft/repository/RelationalTimedBoostRepositoryImpl.java`
- Modify: `paper/src/main/java/net/aincraft/upgrade/PlayerUpgradeRepository.java`
- Modify: `paper/src/main/java/net/aincraft/domain/RelationalJobTaskRepositoryImpl.java`
- Modify: `paper/src/main/java/net/aincraft/service/YamlJobTaskLoader.java`
- Modify: affected repository tests under `paper/src/test/java/net/aincraft/`

**Interfaces:**
- `DatabaseType.getSQLTables()` returns statements parsed from `sql/mysql.sql`.
- Existing repository constructors and methods do not change.
- MySQL upsert statements use `ON DUPLICATE KEY UPDATE` and `VALUES(...)` or row aliases supported by the selected MySQL 8 baseline.

- [ ] **Step 1: Write failing schema/repository assertions**

Update schema fidelity tests to load `mysql.sql`, assert no `SERIAL`, `BYTEA`, `JSONB`, `TIMESTAMPTZ`, or `ON CONFLICT`, and assert `AUTO_INCREMENT`, `DECIMAL`, `BLOB`, `JSON`, `DATETIME`, `ENGINE=InnoDB`, and bounded `VARCHAR` key columns. Add/adjust repository integration assertions for progression, timed boost, player upgrade, task generated keys, foreign-key cascade, and precise `BigDecimal` round trips.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```bash
./gradlew :paper:test --tests '*SchemaFidelityTest' --tests '*RelationalJobProgressionRepositoryTest' --console=plain
```

Expected: failures because `mysql.sql` does not exist and production SQL/schema remain PostgreSQL-specific.

- [ ] **Step 3: Implement canonical MySQL DDL**

Create all existing tables in `mysql.sql` with InnoDB and `utf8mb4`. Use consistent bounded `VARCHAR` sizes for all primary-key, unique, and foreign-key fields; `DECIMAL(38,10)` for numeric values; `BLOB` for serialized boost data; integer `AUTO_INCREMENT` task IDs; `JSON` payload; and `DATETIME(6)` session columns. Preserve foreign-key cascade and defaults.

- [ ] **Step 4: Convert repository statements**

Replace every PostgreSQL `ON CONFLICT`/`excluded` statement with MySQL duplicate-key syntax. Remove the PostgreSQL-only type rejection in `PlayerUpgradeRepository`. Keep generated-key retrieval through JDBC `Statement.RETURN_GENERATED_KEYS`, and preserve ordering/casting behavior using MySQL-compatible expressions.

- [ ] **Step 5: Run the focused tests and verify GREEN**

Run the same Gradle command against a reachable MySQL 8 instance configured by the test environment. Expected: schema fidelity and repository round-trip tests pass; unavailable integration tests must be explicitly skipped by existing availability assumptions, not silently treated as success.

- [ ] **Step 6: Commit**

```bash
git add paper/src/main/resources/sql paper/src/main/java paper/src/test/java
 git commit -m "feat: migrate paper persistence to mysql"
```

### Task 3: Switch the Rust REST API to SQLx MySQL

**Files:**
- Modify: `web/rest-api/Cargo.toml`
- Modify: `web/rest-api/src/db.rs`
- Modify: `web/rest-api/src/main.rs`
- Modify: `web/rest-api/tests/common/mod.rs`
- Modify: `web/rest-api/tests/rest_api_tests.rs`

**Interfaces:**
- `SessionStore::connect(database_url: &str, max_connections: u32)` keeps its signature.
- `SessionStore::connect_with_pool` accepts `MySqlPool`; `pool()` returns `&MySqlPool`.
- `SessionStoreError` retains public variants and security semantics.

- [ ] **Step 1: Write failing Rust tests**

Change integration fixtures to MySQL URLs and `mysql.sql`; add assertions that schema checks work through MySQL `information_schema`, session create/get/update preserve JSON and UTC timestamps, and update returns the existing unauthorized result when no row is updated.

- [ ] **Step 2: Run tests and verify RED**

Run:

```bash
cd web/rest-api && cargo test --all-targets
```

Expected: compile/query failures because the code still imports PostgreSQL SQLx types and uses PostgreSQL placeholders/`RETURNING`.

- [ ] **Step 3: Implement SQLx MySQL conversion**

Enable SQLx `mysql`, `chrono`, `json`, and `uuid` features; replace `PgPool`/`PgPoolOptions` with MySQL equivalents; replace `$1` placeholders with `?`; use MySQL schema lookup (`table_schema = DATABASE()`); bind JSON and chrono values to MySQL columns; and replace `UPDATE ... RETURNING` with an update followed by a select of `expires_at` under the existing authorization/expiry rules.

- [ ] **Step 4: Run Rust tests and verify GREEN**

Run the same command against MySQL 8 with `DATABASE_URL=mysql://...`. Expected: all REST API tests pass and no PostgreSQL feature remains in Cargo metadata.

- [ ] **Step 5: Commit**

```bash
git add web/rest-api/Cargo.toml web/rest-api/src web/rest-api/tests
 git commit -m "feat: switch rest api persistence to mysql"
```

### Task 4: Update configuration, provisioning, CI, and release assets

**Files:**
- Modify: `paper/src/main/resources/database.yml`
- Create: `scripts/apply-mysql-schema.sh`
- Delete: `scripts/apply-postgres-schema.sh`
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/publish-packages.yml`
- Modify: `scripts/package-release-assets.sh`
- Modify: `scripts/test-package-release-assets.sh`
- Modify: `paper/src/main/resources/config.yml`
- Modify: `paper/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Operator script accepts an optional MySQL URL, defaults to `mysql://test:test@127.0.0.1:3306/modularjobs`, and applies `paper/src/main/resources/sql/mysql.sql` without creating schema in application code.
- Release asset is named consistently as `modularjobs-mysql-<version>.sql`.

- [ ] **Step 1: Add config/script/CI assertions**

Update shell/package tests and CI checks to require MySQL URLs, `mysql.sql`, and the MySQL release asset; assert PostgreSQL strings are absent from active build and workflow configuration.

- [ ] **Step 2: Run targeted checks and verify RED**

Run:

```bash
bash scripts/test-package-release-assets.sh
```

Expected: failures while package scripts and workflow references still expect PostgreSQL assets.

- [ ] **Step 3: Implement MySQL operational cutover**

Replace the JDBC dependency with MySQL Connector/J, update version catalog aliases, convert generated config, add the MySQL CLI/Docker schema installer with strict error handling, switch CI services/health checks/env variables/schema application to MySQL 8, and update release packaging inputs and output names.

- [ ] **Step 4: Run operational checks and verify GREEN**

Run:

```bash
bash scripts/test-package-release-assets.sh
./gradlew :paper:processResources --console=plain
```

Expected: packaging tests pass and generated resources contain MySQL configuration.

- [ ] **Step 5: Commit**

```bash
git add paper/build.gradle.kts gradle/libs.versions.toml paper/src/main/resources scripts .github/workflows
 git commit -m "build: provision and package mysql support"
```

### Task 5: Update documentation and remove PostgreSQL support claims

**Files:**
- Modify: `README.md`
- Modify: `docs/database-schema.md`
- Modify: `AGENTS.md`
- Modify: `.claude/CLAUDE.md`
- Modify: relevant `docs/living-specs/*.md`
- Modify: `web/rest-api/README.md`
- Modify: `web/rest-api/Cargo.toml`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Operator docs consistently show MySQL URLs, `type: mysql`, `mysql.sql`, and `apply-mysql-schema.sh`.
- Documentation explicitly preserves connect-only ownership and states PostgreSQL is no longer supported.

- [ ] **Step 1: Add documentation consistency check**

Run a repository search scoped to active source/docs/config files and define the expected result: no PostgreSQL-only support claim, `postgres.sql`, `apply-postgres-schema.sh`, PostgreSQL JDBC driver, or `PgPool` references outside historical changelog entries if retained.

- [ ] **Step 2: Implement documentation/config updates**

Update setup commands, examples, CI descriptions, REST API description, schema ownership text, living specs, and changelog entry to describe MySQL 8 only. Remove contradictory statements that prohibit MySQL.

- [ ] **Step 3: Verify consistency**

Run:

```bash
./gradlew :api:test :common:test :paper:test --console=plain
cd web/rest-api && cargo test --all-targets
```

Then rerun the scoped repository search. Expected: Java and Rust tests pass, and only deliberate historical migration notes may mention PostgreSQL.

- [ ] **Step 4: Commit**

```bash
git add README.md docs AGENTS.md .claude/CLAUDE.md web/rest-api CHANGELOG.md
 git commit -m "docs: document mysql-only persistence"
```

### Task 6: End-to-end MySQL verification and cleanup

**Files:**
- Modify: any affected files discovered by final verification only.
- Test: MySQL 8 service used by Java and Rust integration tests.

- [ ] **Step 1: Start or use a MySQL 8 test instance**

Create `modularjobs` with test credentials, apply `scripts/apply-mysql-schema.sh`, and export the Java and Rust MySQL test URLs.

- [ ] **Step 2: Exercise end-to-end persistence**

Run Java schema fidelity/repository tests and Rust REST API tests against the same MySQL instance. Confirm generated task IDs, decimal precision, binary boost persistence, player upgrade upserts, session JSON/timestamps, expiry handling, and schema-presence failure behavior.

- [ ] **Step 3: Run complete project verification**

Run:

```bash
./gradlew check :paper:shadowJar --console=plain
(cd web/rest-api && cargo test --all-targets)
(cd web/session-editor && npm test && npm run build)
```

Expected: all commands pass with MySQL-only dependencies and no PostgreSQL runtime path.

- [ ] **Step 4: Final repository search and cleanup**

Search active files for `postgres`, `postgresql`, `POSTGRES`, `PgPool`, `ON CONFLICT`, `JSONB`, `BYTEA`, `TIMESTAMPTZ`, and `SERIAL`. Remove stale comments, dead compatibility branches, obsolete files, and contradictory docs, except intentional historical changelog context.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: replace postgres persistence with mysql"
```
