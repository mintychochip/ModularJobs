//! Test-only helpers: apply shared Postgres DDL from paper (not the runtime path).

use sqlx::PgPool;

/// Source of truth: paper shipped Postgres schema (include at compile time).
const POSTGRES_SCHEMA: &str =
    include_str!("../../../paper/src/main/resources/sql/postgres.sql");

/// Split on `;` the same way DatabaseType.getSQLTables does.
pub fn schema_statements() -> Vec<String> {
    POSTGRES_SCHEMA
        .split(';')
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .map(|s| format!("{s};"))
        .collect()
}

/// Provision schema out-of-band for tests (mirrors scripts/apply-postgres-schema.sh).
pub async fn apply_shipped_postgres_schema(pool: &PgPool) -> Result<(), sqlx::Error> {
    for stmt in schema_statements() {
        let without_comments: String = stmt
            .lines()
            .filter(|l| !l.trim_start().starts_with("--"))
            .collect::<Vec<_>>()
            .join("\n");
        if without_comments.trim().is_empty() || without_comments.trim() == ";" {
            continue;
        }
        sqlx::query(&stmt).execute(pool).await?;
    }
    Ok(())
}
