use crate::db::{SessionStore, SessionStoreError};
use crate::models::{
    CreateSessionBody, CreateSessionResponse, EditorPayload, SessionEnvelope, UpdateSessionBody,
};
use crate::security::{tokens_equal, SlidingWindowLimiter};
use axum::extract::{Path, State};
use axum::http::{header, HeaderMap, StatusCode};
use axum::response::{IntoResponse, Response};
use axum::Json;
use rand::Rng;
use std::sync::Arc;
use uuid::Uuid;

#[derive(Clone)]
pub struct AppState {
    pub store: SessionStore,
    /// When set, POST /sessions requires matching `X-Create-Secret` (constant-time).
    pub create_secret: Option<String>,
    pub create_limiter: Arc<SlidingWindowLimiter>,
}

impl AppState {
    pub fn new(store: SessionStore) -> Self {
        Self {
            store,
            create_secret: std::env::var("SESSION_CREATE_SECRET")
                .ok()
                .filter(|s| !s.is_empty()),
            create_limiter: Arc::new(SlidingWindowLimiter::for_session_create()),
        }
    }

    /// Test / explicit construction without reading env for secret (secret still optional).
    pub fn with_create_policy(
        store: SessionStore,
        create_secret: Option<String>,
        create_limiter: SlidingWindowLimiter,
    ) -> Self {
        Self {
            store,
            create_secret,
            create_limiter: Arc::new(create_limiter),
        }
    }
}

pub type ApiResult<T> = Result<T, ApiError>;

pub struct ApiError {
    status: StatusCode,
    message: String,
}

impl ApiError {
    fn new(status: StatusCode, message: impl Into<String>) -> Self {
        Self {
            status,
            message: message.into(),
        }
    }
}

impl From<SessionStoreError> for ApiError {
    fn from(value: SessionStoreError) -> Self {
        match value {
            // NotFound is unused on auth paths after oracle fix; keep mapping defensive.
            SessionStoreError::NotFound | SessionStoreError::Unauthorized => {
                ApiError::new(StatusCode::UNAUTHORIZED, "invalid session token")
            }
            SessionStoreError::Expired => {
                ApiError::new(StatusCode::GONE, "session expired")
            }
            SessionStoreError::Database(e) => {
                tracing::error!(error = %e, "database error");
                ApiError::new(StatusCode::INTERNAL_SERVER_ERROR, "database error")
            }
            SessionStoreError::Serde(e) => {
                ApiError::new(StatusCode::BAD_REQUEST, format!("invalid payload: {e}"))
            }
            SessionStoreError::SchemaMissing => ApiError::new(
                StatusCode::SERVICE_UNAVAILABLE,
                "schema not provisioned; apply sql/mysql.sql out-of-band",
            ),
        }
    }
}

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        let body = serde_json::json!({ "error": self.message });
        (self.status, Json(body)).into_response()
    }
}

fn extract_token(headers: &HeaderMap) -> Result<String, ApiError> {
    if let Some(value) = headers.get("x-session-token") {
        let s = value
            .to_str()
            .map_err(|_| ApiError::new(StatusCode::BAD_REQUEST, "invalid x-session-token header"))?;
        if !s.is_empty() {
            return Ok(s.to_string());
        }
    }
    if let Some(value) = headers.get(header::AUTHORIZATION) {
        let s = value
            .to_str()
            .map_err(|_| ApiError::new(StatusCode::BAD_REQUEST, "invalid authorization header"))?;
        if let Some(token) = s.strip_prefix("Bearer ") {
            if !token.is_empty() {
                return Ok(token.to_string());
            }
        }
    }
    Err(ApiError::new(
        StatusCode::UNAUTHORIZED,
        "missing session token (Authorization: Bearer <token> or X-Session-Token)",
    ))
}

fn generate_code() -> String {
    // Short URL-safe code similar to bytebin keys
    const CHARSET: &[u8] = b"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    let mut rng = rand::thread_rng();
    (0..10)
        .map(|_| {
            let idx = rng.gen_range(0..CHARSET.len());
            CHARSET[idx] as char
        })
        .collect()
}

fn check_create_secret(state: &AppState, headers: &HeaderMap) -> Result<(), ApiError> {
    let Some(expected) = state.create_secret.as_deref() else {
        return Ok(());
    };
    let provided = headers
        .get("x-create-secret")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");
    if !tokens_equal(provided, expected) {
        return Err(ApiError::new(
            StatusCode::UNAUTHORIZED,
            "invalid or missing create secret",
        ));
    }
    Ok(())
}

/// POST /api/v1/sessions — create a new editor session.
pub async fn create_session(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<CreateSessionBody>,
) -> ApiResult<(StatusCode, Json<CreateSessionResponse>)> {
    check_create_secret(&state, &headers)?;
    if !state.create_limiter.check_and_record() {
        return Err(ApiError::new(
            StatusCode::TOO_MANY_REQUESTS,
            "create rate limit exceeded; try again later",
        ));
    }

    let mut payload = body.into_payload();
    let code = generate_code();
    // Always mint a high-entropy server token (ignore client-supplied values).
    let token = Uuid::new_v4().to_string();
    payload.metadata.session_token = token.clone();

    let expires_at = state.store.create(&code, &token, &payload).await?;
    Ok((
        StatusCode::CREATED,
        Json(CreateSessionResponse {
            code,
            token,
            expires_at: expires_at.to_rfc3339(),
        }),
    ))
}

/// GET /api/v1/sessions/:code — load session payload (token required).
pub async fn get_session(
    State(state): State<AppState>,
    Path(code): Path<String>,
    headers: HeaderMap,
) -> ApiResult<Json<SessionEnvelope>> {
    let token = extract_token(&headers)?;
    let (payload, expires_at) = state.store.get(&code, &token).await?;
    Ok(Json(SessionEnvelope {
        code,
        payload,
        expires_at: expires_at.to_rfc3339(),
    }))
}

/// GET /api/v1/sessions/:code/payload — raw EditorPayload (token required).
pub async fn get_session_payload(
    State(state): State<AppState>,
    Path(code): Path<String>,
    headers: HeaderMap,
) -> ApiResult<Json<EditorPayload>> {
    let token = extract_token(&headers)?;
    let (payload, _) = state.store.get(&code, &token).await?;
    Ok(Json(payload))
}

/// PUT /api/v1/sessions/:code — replace session payload (token required).
pub async fn update_session(
    State(state): State<AppState>,
    Path(code): Path<String>,
    headers: HeaderMap,
    Json(body): Json<serde_json::Value>,
) -> ApiResult<Json<SessionEnvelope>> {
    let token = extract_token(&headers)?;
    // Accept either wrapped {payload: ...} or bare EditorPayload
    let payload: EditorPayload = if body.get("payload").is_some() && body.get("version").is_none() {
        let wrapped: UpdateSessionBody = serde_json::from_value(body).map_err(|e| {
            ApiError::new(StatusCode::BAD_REQUEST, format!("invalid body: {e}"))
        })?;
        wrapped.into_payload().map_err(|e| {
            ApiError::new(StatusCode::BAD_REQUEST, format!("invalid payload: {e}"))
        })?
    } else {
        serde_json::from_value(body).map_err(|e| {
            ApiError::new(StatusCode::BAD_REQUEST, format!("invalid payload: {e}"))
        })?
    };

    // Enforce that payload sessionToken matches auth token (cannot steal sessions)
    if !tokens_equal(&payload.metadata.session_token, &token) {
        return Err(ApiError::new(
            StatusCode::FORBIDDEN,
            "payload.metadata.sessionToken must match session token",
        ));
    }

    let expires_at = state.store.update(&code, &token, &payload).await?;
    Ok(Json(SessionEnvelope {
        code,
        payload,
        expires_at: expires_at.to_rfc3339(),
    }))
}

/// GET /healthz
pub async fn healthz() -> impl IntoResponse {
    Json(serde_json::json!({ "status": "ok" }))
}
