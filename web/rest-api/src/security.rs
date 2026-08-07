//! Auth and deployment hardening helpers for the session API.

use std::collections::VecDeque;
use std::sync::Mutex;
use std::time::{Duration, Instant};
use subtle::ConstantTimeEq;
use tower_http::cors::{AllowOrigin, CorsLayer};
use axum::http::{header, HeaderName, HeaderValue, Method};

/// Constant-time equality for secret tokens (length mismatch → false, no early byte exit).
pub fn tokens_equal(a: &str, b: &str) -> bool {
    let a = a.as_bytes();
    let b = b.as_bytes();
    if a.len() != b.len() {
        // Dummy compare so unequal-length path still does work proportional to `a`.
        let _ = a.ct_eq(a);
        return false;
    }
    bool::from(a.ct_eq(b))
}

/// Simple global sliding-window rate limiter (create endpoint DoS mitigation).
pub struct SlidingWindowLimiter {
    hits: Mutex<VecDeque<Instant>>,
    max_hits: usize,
    window: Duration,
}

impl SlidingWindowLimiter {
    pub fn new(max_hits: usize, window: Duration) -> Self {
        Self {
            hits: Mutex::new(VecDeque::new()),
            max_hits: max_hits.max(1),
            window,
        }
    }

    /// Defaults: 60 creates / 60s (tunable via env in main).
    pub fn for_session_create() -> Self {
        let max = std::env::var("SESSION_CREATE_RATE_LIMIT")
            .ok()
            .and_then(|s| s.parse().ok())
            .unwrap_or(60);
        Self::new(max, Duration::from_secs(60))
    }

    /// Returns true if the call is allowed and records it; false if over limit.
    pub fn check_and_record(&self) -> bool {
        let now = Instant::now();
        let mut hits = self.hits.lock().unwrap_or_else(|e| e.into_inner());
        while let Some(front) = hits.front() {
            if now.duration_since(*front) > self.window {
                hits.pop_front();
            } else {
                break;
            }
        }
        if hits.len() >= self.max_hits {
            return false;
        }
        hits.push_back(now);
        true
    }
}

/// Build CORS layer from `CORS_ALLOW_ORIGINS`:
/// - unset / empty → localhost editor defaults (dev-safe, not `*`)
/// - `*` → any origin (explicit opt-in; insecure for public deploys)
/// - comma-separated list → exact origins only
pub fn cors_layer_from_env() -> CorsLayer {
    cors_layer_from_config(std::env::var("CORS_ALLOW_ORIGINS").ok().as_deref())
}

pub fn cors_layer_from_config(raw: Option<&str>) -> CorsLayer {
    let methods = [
        Method::GET,
        Method::POST,
        Method::PUT,
        Method::OPTIONS,
    ];
    let headers = [
        header::AUTHORIZATION,
        header::CONTENT_TYPE,
        HeaderName::from_static("x-session-token"),
        HeaderName::from_static("x-create-secret"),
    ];

    let layer = CorsLayer::new()
        .allow_methods(methods)
        .allow_headers(headers);

    match raw.map(str::trim).filter(|s| !s.is_empty()) {
        Some("*") => layer.allow_origin(tower_http::cors::Any),
        Some(list) => {
            let origins: Vec<HeaderValue> = list
                .split(',')
                .map(str::trim)
                .filter(|s| !s.is_empty())
                .filter_map(|s| HeaderValue::from_str(s).ok())
                .collect();
            if origins.is_empty() {
                layer.allow_origin(AllowOrigin::list(default_dev_origins()))
            } else {
                layer.allow_origin(AllowOrigin::list(origins))
            }
        }
        None => layer.allow_origin(AllowOrigin::list(default_dev_origins())),
    }
}

fn default_dev_origins() -> Vec<HeaderValue> {
    [
        "http://127.0.0.1:5173",
        "http://localhost:5173",
        "http://127.0.0.1:4173",
        "http://localhost:4173",
        "http://127.0.0.1:4321",
        "http://localhost:4321",
    ]
    .into_iter()
    .map(HeaderValue::from_static)
    .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn tokens_equal_matches_and_rejects() {
        assert!(tokens_equal("abc", "abc"));
        assert!(!tokens_equal("abc", "abd"));
        assert!(!tokens_equal("abc", "ab"));
        assert!(!tokens_equal("", "x"));
        assert!(tokens_equal("", ""));
    }
}
