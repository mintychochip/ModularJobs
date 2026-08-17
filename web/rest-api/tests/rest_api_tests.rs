//! Integration tests against the shipped session REST handlers and real MySQL.

mod common;

use axum::body::Body;
use axum::http::{Request, StatusCode};
use http_body_util::BodyExt;
use rest_api::app;
use rest_api::db::SessionStore;
use rest_api::handlers::AppState;
use rest_api::models::{EditorMetadata, EditorPayload, JobData, PayableData, TaskData};
use rest_api::security::SlidingWindowLimiter;
use serde_json::{json, Value};
use std::collections::HashMap;
use std::env;
use std::sync::LazyLock;
use std::time::Duration;
use tokio::sync::{Mutex, MutexGuard, OnceCell};
use tower::ServiceExt;

fn database_url() -> String {
    env::var("DATABASE_URL").unwrap_or_else(|_| {
        "mysql://test:test@127.0.0.1:3306/modularjobs".to_string()
    })
}

static DATABASE_TEST_LOCK: LazyLock<Mutex<()>> = LazyLock::new(|| Mutex::new(()));

static DATABASE_RESET: OnceCell<()> = OnceCell::const_new();

async fn setup_store() -> (SessionStore, MutexGuard<'static, ()>) {
    let guard = DATABASE_TEST_LOCK.lock().await;
    let store = SessionStore::connect(&database_url(), 2)
        .await
        .expect("connect to mysql for session API tests");
    common::apply_shipped_mysql_schema(store.pool())
        .await
        .expect("apply shared mysql schema for tests");
    store
        .require_schema()
        .await
        .expect("editor_sessions must exist after provision");
    DATABASE_RESET
        .get_or_init(|| async {
            sqlx::query("TRUNCATE TABLE editor_sessions")
                .execute(store.pool())
                .await
                .expect("truncate editor_sessions");
        })
        .await;
    (store, guard)
}

/// Default test state: no create secret, high rate limit so tests do not 429.
async fn setup() -> (AppState, MutexGuard<'static, ()>) {
    let (store, guard) = setup_store().await;
    (
        AppState::with_create_policy(
            store,
            None,
            SlidingWindowLimiter::new(10_000, Duration::from_secs(60)),
        ),
        guard,
    )
}

#[tokio::test]
async fn require_table_fails_without_creating_missing_table() {
    let store = SessionStore::connect(&database_url(), 2)
        .await
        .expect("connect");
    // Probe a name that does not exist — must not CREATE it.
    let probe = format!("mj_missing_{}", std::process::id());
    let err = store
        .require_table(&probe)
        .await
        .expect_err("missing table must fail");
    let msg = err.to_string();
    assert!(
        msg.contains("schema not provisioned") || msg.contains("editor_sessions"),
        "unexpected: {msg}"
    );
    // Confirm still absent (no side-effect create)
    let exists: bool = sqlx::query_scalar(
        r#"
        SELECT EXISTS (
          SELECT 1 FROM information_schema.tables
          WHERE table_schema = DATABASE() AND table_name = ?
        )
        "#,
    )
    .bind(&probe)
    .fetch_one(store.pool())
    .await
    .expect("exists check");
    assert!(!exists, "require_table must not create {probe}");
}

fn sample_payload(token: &str) -> EditorPayload {
    let mut jobs = HashMap::new();
    jobs.insert(
        "modularjobs:miner".to_string(),
        JobData {
            display_name: "Miner".to_string(),
            tasks: vec![TaskData {
                action_type_key: "modularjobs:block_break".to_string(),
                context_key: "minecraft:stone".to_string(),
                payables: vec![PayableData {
                    payable_type: "modularjobs:experience".to_string(),
                    amount: "2.5".to_string(),
                }],
            }],
        },
    );
    EditorPayload {
        version: 1,
        metadata: EditorMetadata {
            exported_at: "2026-08-06T00:00:00Z".to_string(),
            exported_by: "test-player".to_string(),
            session_token: token.to_string(),
            server_name: Some("test-server".to_string()),
        },
        jobs,
        registered_action_types: vec!["modularjobs:block_break".to_string()],
        registered_payable_types: vec!["modularjobs:experience".to_string()],
    }
}

async fn body_json(response: axum::response::Response) -> Value {
    let bytes = response.into_body().collect().await.unwrap().to_bytes();
    serde_json::from_slice(&bytes).expect("json body")
}

async fn create_session(
    router: &axum::Router,
    payload: &EditorPayload,
) -> (String, String) {
    let response = router
        .clone()
        .oneshot(
            Request::builder()
                .method("POST")
                .uri("/api/v1/sessions")
                .header("content-type", "application/json")
                .body(Body::from(serde_json::to_vec(payload).unwrap()))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::CREATED);
    let body = body_json(response).await;
    let code = body["code"].as_str().unwrap().to_string();
    let token = body["token"].as_str().unwrap().to_string();
    (code, token)
}

#[tokio::test]
async fn create_returns_code_and_server_minted_token() {
    let (state, _database_guard) = setup().await;
    let router = app(state);
    // Client-supplied weak token must be ignored; server mints UUID.
    let payload = sample_payload("weak-client-token");

    let response = router
        .oneshot(
            Request::builder()
                .method("POST")
                .uri("/api/v1/sessions")
                .header("content-type", "application/json")
                .body(Body::from(serde_json::to_vec(&payload).unwrap()))
                .unwrap(),
        )
        .await
        .unwrap();

    assert_eq!(response.status(), StatusCode::CREATED);
    let body = body_json(response).await;
    assert!(body["code"].as_str().unwrap().len() >= 8);
    let token = body["token"].as_str().unwrap();
    assert!(!token.is_empty());
    assert_ne!(token, "weak-client-token");
    // UUID v4 shape
    assert_eq!(token.len(), 36);
    assert!(body["expiresAt"].as_str().is_some());
}

#[tokio::test]
async fn get_with_valid_token_returns_payload() {
    let (state, _database_guard) = setup().await;
    let router = app(state);
    let payload = sample_payload("");
    let (code, token) = create_session(&router, &payload).await;

    let get = router
        .oneshot(
            Request::builder()
                .method("GET")
                .uri(format!("/api/v1/sessions/{code}"))
                .header("authorization", format!("Bearer {token}"))
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(get.status(), StatusCode::OK);
    let body = body_json(get).await;
    assert_eq!(body["code"], code);
    assert_eq!(body["payload"]["metadata"]["sessionToken"], token);
    assert_eq!(
        body["payload"]["jobs"]["modularjobs:miner"]["tasks"][0]["payables"][0]["amount"],
        "2.5"
    );
    assert_eq!(
        body["payload"]["jobs"]["modularjobs:miner"]["tasks"][0]["actionTypeKey"],
        "modularjobs:block_break"
    );
}

#[tokio::test]
async fn get_without_token_fails() {
    let (state, _database_guard) = setup().await;
    let router = app(state);
    let (code, _) = create_session(&router, &sample_payload("")).await;

    let get = router
        .oneshot(
            Request::builder()
                .method("GET")
                .uri(format!("/api/v1/sessions/{code}"))
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(get.status(), StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn get_with_wrong_token_fails() {
    let (state, _database_guard) = setup().await;
    let router = app(state);
    let (code, _) = create_session(&router, &sample_payload("")).await;

    let get = router
        .oneshot(
            Request::builder()
                .method("GET")
                .uri(format!("/api/v1/sessions/{code}"))
                .header("x-session-token", "wrong-token")
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(get.status(), StatusCode::UNAUTHORIZED);
    let body = body_json(get).await;
    assert!(body["error"].as_str().unwrap().contains("token"));
}

#[tokio::test]
async fn get_unknown_code_does_not_reveal_existence() {
    let (state, _database_guard) = setup().await;
    let router = app(state);

    let get = router
        .oneshot(
            Request::builder()
                .method("GET")
                .uri("/api/v1/sessions/nosuchcode1")
                .header("authorization", "Bearer some-token-value-here")
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    // 401 not 404 — avoid session-code existence oracle
    assert_eq!(get.status(), StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn update_then_get_preserves_jobs_tasks_payables_and_token() {
    let (state, _database_guard) = setup().await;
    let router = app(state);
    let (code, token) = create_session(&router, &sample_payload("")).await;

    let mut payload = sample_payload(&token);
    payload.jobs.get_mut("modularjobs:miner").unwrap().tasks[0].payables[0].amount =
        "99.125".to_string();
    payload.jobs.get_mut("modularjobs:miner").unwrap().tasks.push(TaskData {
        action_type_key: "modularjobs:block_place".to_string(),
        context_key: "minecraft:cobblestone".to_string(),
        payables: vec![PayableData {
            payable_type: "modularjobs:experience".to_string(),
            amount: "1.0".to_string(),
        }],
    });

    let put = router
        .clone()
        .oneshot(
            Request::builder()
                .method("PUT")
                .uri(format!("/api/v1/sessions/{code}"))
                .header("content-type", "application/json")
                .header("authorization", format!("Bearer {token}"))
                .body(Body::from(serde_json::to_vec(&payload).unwrap()))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(put.status(), StatusCode::OK);
    let put_body = body_json(put).await;
    assert_eq!(
        put_body["payload"]["jobs"]["modularjobs:miner"]["tasks"][0]["payables"][0]["amount"],
        "99.125"
    );

    let get = router
        .oneshot(
            Request::builder()
                .method("GET")
                .uri(format!("/api/v1/sessions/{code}/payload"))
                .header("x-session-token", token.as_str())
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(get.status(), StatusCode::OK);
    let body = body_json(get).await;
    assert_eq!(body["metadata"]["sessionToken"], token);
    assert_eq!(
        body["jobs"]["modularjobs:miner"]["tasks"][0]["payables"][0]["amount"],
        "99.125"
    );
    assert_eq!(body["jobs"]["modularjobs:miner"]["tasks"].as_array().unwrap().len(), 2);
    assert_eq!(
        body["jobs"]["modularjobs:miner"]["tasks"][1]["contextKey"],
        "minecraft:cobblestone"
    );
}

#[tokio::test]
async fn update_without_token_cannot_overwrite() {
    let (state, _database_guard) = setup().await;
    let router = app(state);
    let (code, token) = create_session(&router, &sample_payload("")).await;

    let mut evil = sample_payload(&token);
    evil.jobs.get_mut("modularjobs:miner").unwrap().tasks[0].payables[0].amount =
        "0".to_string();

    let put = router
        .oneshot(
            Request::builder()
                .method("PUT")
                .uri(format!("/api/v1/sessions/{code}"))
                .header("content-type", "application/json")
                .body(Body::from(serde_json::to_vec(&evil).unwrap()))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(put.status(), StatusCode::UNAUTHORIZED);
}

#[tokio::test]
async fn update_rejects_payload_session_token_rewrite() {
    let (state, _database_guard) = setup().await;
    let router = app(state);
    let (code, token) = create_session(&router, &sample_payload("")).await;

    let mut evil = sample_payload(&token);
    evil.metadata.session_token = "attacker-new-token".to_string();
    evil.jobs.get_mut("modularjobs:miner").unwrap().tasks[0].payables[0].amount =
        "0".to_string();

    let put = router
        .oneshot(
            Request::builder()
                .method("PUT")
                .uri(format!("/api/v1/sessions/{code}"))
                .header("content-type", "application/json")
                .header("authorization", format!("Bearer {token}"))
                .body(Body::from(serde_json::to_vec(&evil).unwrap()))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(put.status(), StatusCode::FORBIDDEN);
    let body = body_json(put).await;
    assert!(
        body["error"]
            .as_str()
            .unwrap()
            .contains("sessionToken"),
        "body={body}"
    );
}

#[tokio::test]
async fn create_requires_secret_when_configured() {
    let (store, _database_guard) = setup_store().await;
    let state = AppState::with_create_policy(
        store,
        Some("super-secret-create".to_string()),
        SlidingWindowLimiter::new(10_000, Duration::from_secs(60)),
    );
    let router = app(state);
    let payload = sample_payload("");

    let denied = router
        .clone()
        .oneshot(
            Request::builder()
                .method("POST")
                .uri("/api/v1/sessions")
                .header("content-type", "application/json")
                .body(Body::from(serde_json::to_vec(&payload).unwrap()))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(denied.status(), StatusCode::UNAUTHORIZED);

    let allowed = router
        .oneshot(
            Request::builder()
                .method("POST")
                .uri("/api/v1/sessions")
                .header("content-type", "application/json")
                .header("x-create-secret", "super-secret-create")
                .body(Body::from(serde_json::to_vec(&payload).unwrap()))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(allowed.status(), StatusCode::CREATED);
}

#[tokio::test]
async fn create_rate_limit_returns_429() {
    let (store, _database_guard) = setup_store().await;
    let state = AppState::with_create_policy(
        store,
        None,
        SlidingWindowLimiter::new(2, Duration::from_secs(60)),
    );
    let router = app(state);
    let payload = sample_payload("");

    for _ in 0..2 {
        let r = router
            .clone()
            .oneshot(
                Request::builder()
                    .method("POST")
                    .uri("/api/v1/sessions")
                    .header("content-type", "application/json")
                    .body(Body::from(serde_json::to_vec(&payload).unwrap()))
                    .unwrap(),
            )
            .await
            .unwrap();
        assert_eq!(r.status(), StatusCode::CREATED);
    }

    let limited = router
        .oneshot(
            Request::builder()
                .method("POST")
                .uri("/api/v1/sessions")
                .header("content-type", "application/json")
                .body(Body::from(serde_json::to_vec(&payload).unwrap()))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(limited.status(), StatusCode::TOO_MANY_REQUESTS);
}

#[tokio::test]
async fn healthz_ok() {
    let (state, _database_guard) = setup().await;
    let router = app(state);
    let response = router
        .oneshot(
            Request::builder()
                .method("GET")
                .uri("/healthz")
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
    let body = body_json(response).await;
    assert_eq!(body["status"], "ok");
}

// silence unused import warning in some rustc versions
#[allow(dead_code)]
fn _json_helper() -> Value {
    json!({})
}
