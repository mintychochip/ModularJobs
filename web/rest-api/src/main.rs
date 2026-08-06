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
        "postgres://test:test@127.0.0.1:55432/modularjobs".to_string()
    });
    let bind = env::var("BIND_ADDR").unwrap_or_else(|_| "127.0.0.1:18787".to_string());

    tracing::info!(%database_url, %bind, "starting rest-api (connect-only, no DDL)");

    // Connect only — schema must already exist (scripts/apply-postgres-schema.sh).
    let store = SessionStore::connect(&database_url, 5).await?;
    store.require_schema().await.map_err(|e| anyhow::anyhow!("{e}"))?;

    let state = AppState { store };
    let router = app(state);

    let addr: SocketAddr = bind.parse()?;
    let listener = tokio::net::TcpListener::bind(addr).await?;
    tracing::info!(%addr, "listening");
    axum::serve(listener, router).await?;
    Ok(())
}
