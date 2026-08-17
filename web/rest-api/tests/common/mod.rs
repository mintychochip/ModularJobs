//! Test-only helpers: apply shared MySQL DDL from paper (not the runtime path).

use sqlx::MySqlPool;

/// Source of truth: paper shipped MySQL schema (include at compile time).
const MYSQL_SCHEMA: &str =
    include_str!("../../../../paper/src/main/resources/sql/mysql.sql");

/// Split on `;` the same way DatabaseType.getSQLTables does, excluding SQL comments.
pub fn schema_statements() -> Vec<String> {
    MYSQL_SCHEMA
        .lines()
        .filter(|line| !line.trim_start().starts_with("--"))
        .collect::<Vec<_>>()
        .join("\n")
        .split(';')
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .map(|s| format!("{s};"))
        .collect()
}
/// Provision schema out-of-band for tests (mirrors scripts/apply-mysql-schema.sh).
pub async fn apply_shipped_mysql_schema(pool: &MySqlPool) -> Result<(), sqlx::Error> {
    for stmt in schema_statements() {
        sqlx::query(&stmt).execute(pool).await?;
    }
    Ok(())
}
