use crate::models::EditorPayload;
use crate::security::tokens_equal;
use chrono::{DateTime, Duration, Utc};
use sqlx::mysql::MySqlPoolOptions;
use sqlx::{MySqlPool, Row};
use thiserror::Error;

#[derive(Debug, Error)]
pub enum SessionStoreError {
    #[error("session not found")]
    NotFound,
    #[error("invalid session token")]
    Unauthorized,
    #[error("session expired")]
    Expired,
    #[error("database error: {0}")]
    Database(#[from] sqlx::Error),
    #[error("payload serialization: {0}")]
    Serde(#[from] serde_json::Error),
    #[error("schema not provisioned: missing table(s) — apply paper sql/mysql.sql out-of-band (scripts/apply-mysql-schema.sh)")]
    SchemaMissing,
}

#[derive(Clone)]
pub struct SessionStore {
    pool: MySqlPool,
    session_ttl: Duration,
}

impl SessionStore {
    /// Connect only. Tables must already exist (provision via sql/mysql.sql).
    /// Does not run DDL.
    pub async fn connect(database_url: &str, max_connections: u32) -> Result<Self, sqlx::Error> {
        let pool = MySqlPoolOptions::new()
            .max_connections(max_connections)
            .connect(database_url)
            .await?;
        Ok(Self::from_pool(pool))
    }

    pub async fn connect_with_pool(pool: MySqlPool) -> Result<Self, sqlx::Error> {
        Ok(Self::from_pool(pool))
    }

    fn from_pool(pool: MySqlPool) -> Self {
        Self {
            pool,
            session_ttl: Duration::hours(24),
        }
    }

    pub fn pool(&self) -> &MySqlPool {
        &self.pool
    }
    /// Fail-fast: editor_sessions must exist. Never creates it.
    pub async fn require_schema(&self) -> Result<(), SessionStoreError> {
        self.require_table("editor_sessions").await
    }

    /// Fail-fast table presence check. Never creates tables.
    pub async fn require_table(&self, table_name: &str) -> Result<(), SessionStoreError> {
        let exists: bool = sqlx::query_scalar(
            r#"
            SELECT EXISTS (
              SELECT 1 FROM information_schema.tables
              WHERE table_schema = DATABASE()
                AND table_name = ?
            )
            "#,
        )
        .bind(table_name)
        .fetch_one(&self.pool)
        .await?;
        if !exists {
            return Err(SessionStoreError::SchemaMissing);
        }
        Ok(())
    }

    pub async fn create(
        &self,
        code: &str,
        token: &str,
        payload: &EditorPayload,
    ) -> Result<DateTime<Utc>, SessionStoreError> {
        let now = Utc::now();
        let expires_at = now + self.session_ttl;
        let json = serde_json::to_value(payload)?;
        sqlx::query(
            r#"
            INSERT INTO editor_sessions (session_code, session_token, payload, created_at, updated_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?)
            "#,
        )
        .bind(code)
        .bind(token)
        .bind(json)
        .bind(now)
        .bind(now)
        .bind(expires_at)
        .execute(&self.pool)
        .await?;
        Ok(expires_at)
    }

    pub async fn get(
        &self,
        code: &str,
        token: &str,
    ) -> Result<(EditorPayload, DateTime<Utc>), SessionStoreError> {
        let row = sqlx::query(
            r#"
            SELECT session_token, payload, expires_at
            FROM editor_sessions
            WHERE session_code = ?
            "#,
        )
        .bind(code)
        .fetch_optional(&self.pool)
        .await?;

        // Unknown code → Unauthorized (not NotFound) so clients cannot probe code existence.
        let row = row.ok_or(SessionStoreError::Unauthorized)?;
        let stored_token: String = row.try_get("session_token")?;
        if !tokens_equal(&stored_token, token) {
            return Err(SessionStoreError::Unauthorized);
        }
        let expires_at: DateTime<Utc> = row.try_get("expires_at")?;
        if expires_at < Utc::now() {
            return Err(SessionStoreError::Expired);
        }
        let payload_json: serde_json::Value = row.try_get("payload")?;
        let payload: EditorPayload = serde_json::from_value(payload_json)?;
        Ok((payload, expires_at))
    }

    pub async fn update(
        &self,
        code: &str,
        token: &str,
        payload: &EditorPayload,
    ) -> Result<DateTime<Utc>, SessionStoreError> {
        let (_, _) = self.get(code, token).await?;
        let now = Utc::now();
        let json = serde_json::to_value(payload)?;
        let result = sqlx::query(
            r#"
            UPDATE editor_sessions
            SET payload = ?, updated_at = ?
            WHERE session_code = ? AND session_token = ? AND expires_at > ?
            "#,
        )
        .bind(json)
        .bind(now)
        .bind(code)
        .bind(token)
        .bind(now)
        .execute(&self.pool)
        .await?;

        if result.rows_affected() != 1 {
            return Err(SessionStoreError::Unauthorized);
        }

        let row = sqlx::query(
            "SELECT expires_at FROM editor_sessions WHERE session_code = ? AND session_token = ?",
        )
        .bind(code)
        .bind(token)
        .fetch_optional(&self.pool)
        .await?;

        match row {
            Some(row) => {
                let expires_at: DateTime<Utc> = row.try_get("expires_at")?;
                Ok(expires_at)
            }
            None => Err(SessionStoreError::Unauthorized),
        }
    }
}
