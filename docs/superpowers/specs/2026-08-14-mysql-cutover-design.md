# MySQL Cutover Design

## Goal

Replace PostgreSQL with MySQL as ModularJobs' sole relational database for the Paper plugin and secure editor REST API, while preserving behavior and the existing connect-only schema ownership model.

## Scope

The cutover covers:

- Paper JDBC dependency, database type parsing, schema presence checks, connection startup diagnostics, and PostgreSQL-specific repository SQL.
- MySQL 8-compatible operator-provisioned schema and schema application script.
- Rust REST API SQLx pool, query placeholders, schema checks, JSON/timestamp bindings, and update semantics.
- Bundled configuration, build metadata, CI database services/environment, release asset names, tests, README, database documentation, and living specifications that explicitly describe PostgreSQL support.

PostgreSQL support is removed rather than retained as a second backend. SQLite and MariaDB remain unsupported unless the MySQL driver/API explicitly guarantees compatibility; the supported store identifier is `mysql`.

## Invariants

1. Paper and the REST API only connect to an already-provisioned database. They never execute DDL at runtime.
2. Startup still fails fast when required tables are missing.
3. Existing repository behavior, keys, payloads, expiry checks, and data precision remain unchanged.
4. Identical configured JDBC URL and username sections continue sharing one Hikari pool.
5. SQL remains parameterized; no user-controlled value is interpolated into statements.
6. MySQL schema uses InnoDB and UTF-8 (`utf8mb4`) for relational consistency and player/job key support.

## Architecture

`DatabaseType` becomes a single-value enum containing `MYSQL("mysql", "com.mysql.cj.jdbc.Driver")`. Identifier parsing accepts `mysql` case-insensitively and defaults to MySQL when blank. PostgreSQL aliases and error text are removed. `ConnectionSourceFactory` creates the same Hikari-backed `ConnectionSource`, verifies required tables through MySQL-compatible `information_schema` queries, and reports MySQL-specific provisioning guidance.

Paper repository statements that currently use PostgreSQL `ON CONFLICT` are converted to MySQL `ON DUPLICATE KEY UPDATE`. JDBC positional `?` parameters remain unchanged. All PostgreSQL-only type checks and comments are migrated to MySQL terminology.

The canonical schema becomes `paper/src/main/resources/sql/mysql.sql`. It uses MySQL 8-compatible constructs: bounded `VARCHAR(...)` columns for every key-bearing field used in primary keys, unique constraints, or foreign-key relationships (including player, job, action, context, payable, and session identifiers), `BIGINT AUTO_INCREMENT` or `INT AUTO_INCREMENT` for generated task IDs, `DECIMAL(38,10)` for amounts and experience, `BLOB` for serialized binary values, `JSON` for editor payloads, and `DATETIME(6)` for session timestamps. Tables use `ENGINE=InnoDB`, `utf8mb4`, and explicit foreign-key behavior. Unbounded `TEXT` is reserved for non-indexed content. Schema is still applied by operators through `scripts/apply-mysql-schema.sh`.

The Rust API switches SQLx from PostgreSQL to MySQL (`MySqlPoolOptions`, `MySqlPool`, and MySQL feature flags). SQL placeholders become `?`. The `UPDATE ... RETURNING` query is replaced with a parameterized update followed by a parameterized select of `expires_at`, preserving the current unauthorized result when the update affects no row or expiry races. JSON and chrono bindings use SQLx MySQL-compatible types.

## Configuration and operations

Generated `database.yml` sections use `type: mysql` and `jdbc:mysql://host:3306/modularjobs`. Documentation and release packaging refer to `mysql.sql`, `DATABASE_URL=mysql://...`, and `apply-mysql-schema.sh`. CI provisions MySQL 8 for both Java and Rust jobs, applies the schema before integration/fidelity tests, and uses MySQL-specific environment variable names. PostgreSQL service, client, driver, schema, and asset references are removed.

## Testing

Tests are updated to assert MySQL defaults, parsing, driver class, JDBC configuration, schema resource fidelity, MySQL upsert syntax, and MySQL REST API behavior. Live integration tests use a MySQL 8 service and are skipped only when the configured MySQL endpoint is unavailable, matching current test conventions. Java module tests, Rust tests, and CI configuration must compile without PostgreSQL dependencies or identifiers. Verification includes targeted red/green tests for changed behavior, the relevant Java test suite, Rust `cargo test`, and a MySQL-backed schema/repository smoke test.

## Error handling

- Missing or blank database type resolves to MySQL.
- Any non-MySQL identifier fails with an error that names `mysql` and the MySQL schema script.
- Missing schema tables fail startup/API schema checks with MySQL provisioning instructions.
- Database connectivity and SQL errors retain their existing exception propagation and user-visible failure boundaries.
- The REST API continues returning unauthorized behavior for unknown, invalid-token, and update-race cases; it does not expose session existence.

## Non-goals

- Runtime schema migrations or automatic table creation.
- Supporting PostgreSQL and MySQL simultaneously.
- Adding MariaDB, SQLite, or another dialect.
- Changing domain models, repository APIs, session security, or editor payload shape.
