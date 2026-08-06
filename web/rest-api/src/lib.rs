pub mod db;
pub mod handlers;
pub mod models;

use axum::routing::{get, post, put};
use axum::Router;
use handlers::AppState;
use tower_http::cors::{Any, CorsLayer};
use tower_http::trace::TraceLayer;

/// Build the HTTP router used by the binary and integration tests.
pub fn app(state: AppState) -> Router {
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    Router::new()
        .route("/healthz", get(handlers::healthz))
        .route("/api/v1/sessions", post(handlers::create_session))
        .route(
            "/api/v1/sessions/{code}",
            get(handlers::get_session).put(handlers::update_session),
        )
        .route(
            "/api/v1/sessions/{code}/payload",
            get(handlers::get_session_payload),
        )
        // Also accept PUT as post alias path for clients that prefer /save
        .route("/api/v1/sessions/{code}/save", put(handlers::update_session))
        .layer(cors)
        .layer(TraceLayer::new_for_http())
        .with_state(state)
}
