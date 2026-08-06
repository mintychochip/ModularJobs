use crate::db::{SessionStore, SessionStoreError};
use crate::models::{
    CreateSessionBody, CreateSessionResponse, EditorPayload, SessionEnvelope, UpdateSessionBody,
};
use axum::extract::{Path, State};
use axum::http::{header, HeaderMap, StatusCode};
use axum::response::{IntoResponse, Response};
use axum::Json;
use rand::Rng;
use uuid::Uuid;

#[derive(Clone)]
pub struct AppState {
    pub store: SessionStore,
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
            SessionStoreError::NotFound => ApiError::new(StatusCode::NOT_FOUND, "session not found"),
            SessionStoreError::Unauthorized => {
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
                "schema not provisioned; apply sql/postgres.sql out-of-band",
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

/// POST /api/v1/sessions — create a new editor session.
pub async fn create_session(
    State(state): State<AppState>,
    Json(body): Json<CreateSessionBody>,
) -> ApiResult<(StatusCode, Json<CreateSessionResponse>)> {
    let mut payload = body.into_payload();
    let code = generate_code();
    // Prefer client-supplied sessionToken; otherwise mint one.
    let token = if payload.metadata.session_token.is_empty() {
        let t = Uuid::new_v4().to_string();
        payload.metadata.session_token = t.clone();
        t
    } else {
        payload.metadata.session_token.clone()
    };

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
    if payload.metadata.session_token != token {
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
