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
/// - `*` alone → any origin (explicit opt-in; insecure for public deploys)
/// - comma-separated list of:
///   - exact origins: `https://editor.example.com`
///   - globs with `*`: `https://*.example.com`, `http://localhost:*`
///   - host-only globs: `*.example.com` (any scheme/port under that host suffix)
///   - regex: `re:^https://dev-[a-z]+\.example\.com$`
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
            let patterns = parse_cors_patterns(list);
            if patterns.is_empty() {
                layer.allow_origin(AllowOrigin::list(default_dev_origins()))
            } else {
                layer.allow_origin(AllowOrigin::predicate(move |origin, _parts| {
                    let origin = origin.to_str().unwrap_or("");
                    origin_matches_any(origin, &patterns)
                }))
            }
        }
        None => layer.allow_origin(AllowOrigin::list(default_dev_origins())),
    }
}

/// Split and normalize a comma-separated `CORS_ALLOW_ORIGINS` value.
pub fn parse_cors_patterns(list: &str) -> Vec<String> {
    list.split(',')
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .map(|s| s.to_string())
        .collect()
}

/// True if `origin` matches any configured pattern (exact, glob, host-suffix, or `re:`).
pub fn origin_matches_any(origin: &str, patterns: &[String]) -> bool {
    patterns.iter().any(|p| origin_matches_pattern(origin, p))
}

/// Match a browser `Origin` header value against one allow-list entry.
///
/// Supported patterns:
/// - Exact: `https://editor.example.com`
/// - Glob (`*` = any non-empty run of chars): `https://*.example.com`, `http://localhost:*`
/// - Host suffix (no scheme): `*.example.com` → any `scheme://…example.com[:port]`
/// - Regex: `re:^https://…$` (full-string; invalid regex never matches)
pub fn origin_matches_pattern(origin: &str, pattern: &str) -> bool {
    let origin = origin.trim();
    let pattern = pattern.trim();
    if origin.is_empty() || pattern.is_empty() {
        return false;
    }

    if let Some(re_src) = pattern.strip_prefix("re:") {
        return match regex::Regex::new(re_src) {
            Ok(re) => re.is_match(origin),
            Err(_) => false,
        };
    }

    // Host-only patterns (no "://") match any scheme/port for that host tree.
    if !pattern.contains("://") && pattern.contains('.') {
        return host_suffix_matches(origin, pattern);
    }

    // Exact (ASCII case-insensitive) or full-origin glob.
    if origin.eq_ignore_ascii_case(pattern) {
        return true;
    }
    if pattern.contains('*') {
        return glob_match_ignore_ascii_case(pattern, origin);
    }
    false
}

/// `*.example.com` / `example.com` against full origin `https://dev.example.com:443`.
fn host_suffix_matches(origin: &str, host_pattern: &str) -> bool {
    let Some(parts) = parse_origin_host(origin) else {
        return false;
    };
    let host = parts.to_ascii_lowercase();
    let pat = host_pattern.trim().to_ascii_lowercase();

    if pat.starts_with("*.") {
        let suffix = &pat[1..]; // ".example.com"
        // sub.example.com or deeper.a.example.com; not apex alone unless also listed
        host.ends_with(suffix) && host.len() > suffix.len()
    } else if pat.contains('*') {
        glob_match(&pat, &host)
    } else {
        host == pat
    }
}

fn parse_origin_host(origin: &str) -> Option<&str> {
    // scheme://host[:port]
    let rest = origin.split_once("://")?.1;
    let hostport = rest.split('/').next().unwrap_or(rest);
    // strip IPv6 brackets carefully: [::1]:5173
    if let Some(inner) = hostport.strip_prefix('[') {
        let end = inner.find(']')?;
        return Some(&inner[..end]);
    }
    Some(hostport.rsplit_once(':').map(|(h, _)| h).unwrap_or(hostport))
}

/// Glob match where `*` matches any sequence (including empty). Case-sensitive.
fn glob_match(pattern: &str, value: &str) -> bool {
    glob_match_bytes(pattern.as_bytes(), value.as_bytes())
}

fn glob_match_ignore_ascii_case(pattern: &str, value: &str) -> bool {
    glob_match(
        &pattern.to_ascii_lowercase(),
        &value.to_ascii_lowercase(),
    )
}

fn glob_match_bytes(pattern: &[u8], value: &[u8]) -> bool {
    let mut pi = 0;
    let mut vi = 0;
    let mut star_pi: Option<usize> = None;
    let mut star_vi: usize = 0;

    while vi < value.len() {
        if pi < pattern.len() && pattern[pi] == b'*' {
            star_pi = Some(pi);
            star_vi = vi;
            pi += 1;
        } else if pi < pattern.len() && pattern[pi] == value[vi] {
            pi += 1;
            vi += 1;
        } else if let Some(sp) = star_pi {
            pi = sp + 1;
            star_vi += 1;
            vi = star_vi;
        } else {
            return false;
        }
    }
    while pi < pattern.len() && pattern[pi] == b'*' {
        pi += 1;
    }
    pi == pattern.len()
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

    #[test]
    fn cors_exact_origin() {
        assert!(origin_matches_pattern(
            "https://editor.example.com",
            "https://editor.example.com"
        ));
        assert!(origin_matches_pattern(
            "https://Editor.Example.com",
            "https://editor.example.com"
        ));
        assert!(!origin_matches_pattern(
            "https://evil.example.com",
            "https://editor.example.com"
        ));
    }

    #[test]
    fn cors_subdomain_glob() {
        assert!(origin_matches_pattern(
            "https://dev.example.com",
            "https://*.example.com"
        ));
        assert!(origin_matches_pattern(
            "https://staging.example.com",
            "https://*.example.com"
        ));
        assert!(origin_matches_pattern(
            "https://a.b.example.com",
            "https://*.example.com"
        ));
        assert!(!origin_matches_pattern(
            "https://example.com",
            "https://*.example.com"
        ));
        assert!(!origin_matches_pattern(
            "http://dev.example.com",
            "https://*.example.com"
        ));
        assert!(!origin_matches_pattern(
            "https://evil.com",
            "https://*.example.com"
        ));
    }

    #[test]
    fn cors_localhost_any_port() {
        assert!(origin_matches_pattern(
            "http://localhost:5173",
            "http://localhost:*"
        ));
        assert!(origin_matches_pattern(
            "http://localhost:4173",
            "http://localhost:*"
        ));
        assert!(!origin_matches_pattern(
            "http://127.0.0.1:5173",
            "http://localhost:*"
        ));
    }

    #[test]
    fn cors_host_only_suffix() {
        assert!(origin_matches_pattern(
            "https://dev.example.com",
            "*.example.com"
        ));
        assert!(origin_matches_pattern(
            "http://staging.example.com:8080",
            "*.example.com"
        ));
        assert!(!origin_matches_pattern(
            "https://example.com",
            "*.example.com"
        ));
        assert!(origin_matches_pattern(
            "https://example.com",
            "example.com"
        ));
        assert!(!origin_matches_pattern(
            "https://notexample.com",
            "*.example.com"
        ));
    }

    #[test]
    fn cors_regex_pattern() {
        assert!(origin_matches_pattern(
            "https://dev-abc.example.com",
            r"re:^https://dev-[a-z]+\.example\.com$"
        ));
        assert!(!origin_matches_pattern(
            "https://prod-abc.example.com",
            r"re:^https://dev-[a-z]+\.example\.com$"
        ));
        assert!(!origin_matches_pattern(
            "https://dev-abc.example.com",
            "re:[invalid"
        ));
    }

    #[test]
    fn cors_list_any_of() {
        let patterns = parse_cors_patterns(
            "https://app.example.com, https://*.preview.example.com, http://localhost:*",
        );
        assert!(origin_matches_any("https://app.example.com", &patterns));
        assert!(origin_matches_any(
            "https://pr-12.preview.example.com",
            &patterns
        ));
        assert!(origin_matches_any("http://localhost:3000", &patterns));
        assert!(!origin_matches_any("https://evil.com", &patterns));
    }
}
