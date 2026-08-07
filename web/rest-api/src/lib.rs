pub mod db;
pub mod handlers;
pub mod models;
pub mod security;

use axum::routing::{get, post, put};
use axum::Router;
use handlers::AppState;
use tower_http::cors::CorsLayer;
use tower_http::trace::TraceLayer;

/// Build the HTTP router used by the binary and integration tests.
/// CORS comes from env via [`security::cors_layer_from_env`] unless overridden.
pub fn app(state: AppState) -> Router {
    app_with_cors(state, security::cors_layer_from_env())
}

/// Router with an explicit CORS layer (tests / custom deploys).
pub fn app_with_cors(state: AppState, cors: CorsLayer) -> Router {
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
