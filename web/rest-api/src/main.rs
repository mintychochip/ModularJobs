use rest_api::app;
use rest_api::db::SessionStore;
use rest_api::handlers::AppState;
use std::env;
use std::net::SocketAddr;
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .init();

    let database_url = env::var("DATABASE_URL").unwrap_or_else(|_| {
        "mysql://test:test@127.0.0.1:3306/modularjobs".to_string()
    });
    let bind = env::var("BIND_ADDR").unwrap_or_else(|_| "127.0.0.1:18787".to_string());

    if bind.starts_with("0.0.0.0") || bind.starts_with("[::]") {
        tracing::warn!(
            %bind,
            "binding on all interfaces; ensure firewall/create-secret/CORS are configured"
        );
    }
    if env::var("SESSION_CREATE_SECRET").ok().filter(|s| !s.is_empty()).is_none() {
        tracing::warn!(
            "SESSION_CREATE_SECRET unset: anyone who can reach this process may create sessions"
        );
    }
    if env::var("CORS_ALLOW_ORIGINS").as_deref() == Ok("*") {
        tracing::warn!("CORS_ALLOW_ORIGINS=* allows any browser origin");
    }

    tracing::info!(%database_url, %bind, "starting rest-api (connect-only, no DDL)");

    // Connect only — schema must already exist (scripts/apply-mysql-schema.sh).
    let store = SessionStore::connect(&database_url, 5).await?;
    store.require_schema().await.map_err(|e| anyhow::anyhow!("{e}"))?;

    let state = AppState::new(store);
    let router = app(state);

    let addr: SocketAddr = bind.parse()?;
    let listener = tokio::net::TcpListener::bind(addr).await?;
    tracing::info!(%addr, "listening");
    axum::serve(listener, router).await?;
    Ok(())
}
