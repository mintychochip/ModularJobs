use anyhow::{Context, Result, bail};
use rest_api::app;
use rest_api::db::SessionStore;
use rest_api::handlers::AppState;
use std::env;
use std::net::SocketAddr;
use tracing_subscriber::EnvFilter;
use url::Url;

#[tokio::main]
async fn main() -> Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .init();

    let bind = env::var("BIND_ADDR").unwrap_or_else(|_| "127.0.0.1:18787".to_string());
    let loopback = bind.starts_with("127.0.0.1")
        || bind.starts_with("localhost:")
        || bind == "localhost"
        || bind.starts_with("[::1]");
    let production = env::var("MODULARJOBS_PRODUCTION").as_deref() == Ok("1") || !loopback;

    let database_url = if production {
        env::var("DATABASE_URL").with_context(|| {
            "DATABASE_URL must be set when MODULARJOBS_PRODUCTION=1 or when binding on a non-loopback address"
        })?
    } else {
        env::var("DATABASE_URL").unwrap_or_else(|_| {
            "mysql://test:test@127.0.0.1:3306/modularjobs".to_string()
        })
    };

    if bind.starts_with("0.0.0.0") || bind.starts_with("[::]") {
        tracing::warn!(
            %bind,
            "binding on all interfaces; ensure firewall/create-secret/CORS are configured"
        );
    }

    let create_secret = env::var("SESSION_CREATE_SECRET").ok().filter(|s| !s.is_empty());
    if create_secret.is_none() && production {
        bail!(
            "SESSION_CREATE_SECRET must be set when MODULARJOBS_PRODUCTION=1 or when binding on a non-loopback address"
        );
    } else if create_secret.is_none() {
        tracing::warn!(
            "SESSION_CREATE_SECRET unset: anyone who can reach this process may create sessions"
        );
    }

    if env::var("CORS_ALLOW_ORIGINS").as_deref() == Ok("*") {
        tracing::warn!("CORS_ALLOW_ORIGINS=* allows any browser origin");
    }

    let database_url_redacted = redact_database_url(&database_url);
    tracing::info!(%database_url_redacted, %bind, "starting rest-api (connect-only, no DDL)");

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

fn redact_database_url(url: &str) -> String {
    match Url::parse(url) {
        Ok(u) => {
            let mut redacted = format!("{}://", u.scheme());
            if let Some(host) = u.host_str() {
                redacted.push_str(host);
            }
            if let Some(port) = u.port() {
                redacted.push_str(&format!(":{}", port));
            }
            redacted.push_str(u.path());
            if let Some(query) = u.query() {
                redacted.push('?');
                redacted.push_str(query);
            }
            redacted
        }
        Err(_) => "(redacted)".to_string(),
    }
}