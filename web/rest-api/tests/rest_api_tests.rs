//! Integration tests against the shipped session REST handlers and real Postgres.

mod common;

use axum::body::Body;
use axum::http::{Request, StatusCode};
use http_body_util::BodyExt;
use rest_api::app;
use rest_api::db::SessionStore;
use rest_api::handlers::AppState;
use rest_api::models::{EditorMetadata, EditorPayload, JobData, PayableData, TaskData};
use serde_json::{json, Value};
use std::collections::HashMap;
use std::env;
use tower::ServiceExt;

fn database_url() -> String {
    env::var("DATABASE_URL").unwrap_or_else(|_| {
        "postgres://test:test@127.0.0.1:55432/modularjobs".to_string()
    })
}

async fn setup() -> AppState {
    // Production path is connect-only. Tests provision schema out-of-band from the
    // shared paper sql/postgres.sql (same file as scripts/apply-postgres-schema.sh).
    let store = SessionStore::connect(&database_url(), 2)
        .await
        .expect("connect to postgres for session API tests");
    common::apply_shipped_postgres_schema(store.pool())
        .await
        .expect("apply shared postgres schema for tests");
    store
        .require_schema()
        .await
        .expect("editor_sessions must exist after provision");
    sqlx::query("TRUNCATE TABLE editor_sessions")
        .execute(store.pool())
        .await
        .expect("truncate editor_sessions");
    AppState { store }
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
          WHERE table_schema = current_schema() AND table_name = $1
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

#[tokio::test]
async fn create_returns_code_and_token() {
    let state = setup().await;
    let router = app(state);
    let payload = sample_payload("");

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
    assert!(!body["token"].as_str().unwrap().is_empty());
    assert!(body["expiresAt"].as_str().is_some());
}

#[tokio::test]
async fn get_with_valid_token_returns_payload() {
    let state = setup().await;
    let router = app(state);
    let token = "secret-token-abc";
    let payload = sample_payload(token);

    let create = router
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
    assert_eq!(create.status(), StatusCode::CREATED);
    let created = body_json(create).await;
    let code = created["code"].as_str().unwrap().to_string();
    let returned_token = created["token"].as_str().unwrap().to_string();
    assert_eq!(returned_token, token);

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
    let state = setup().await;
    let router = app(state);
    let payload = sample_payload("tok");

    let create = router
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
    let code = body_json(create).await["code"].as_str().unwrap().to_string();

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
    let state = setup().await;
    let router = app(state);
    let payload = sample_payload("correct-token");

    let create = router
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
    let code = body_json(create).await["code"].as_str().unwrap().to_string();

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
async fn update_then_get_preserves_jobs_tasks_payables_and_token() {
    let state = setup().await;
    let router = app(state);
    let token = "persist-token-1";
    let mut payload = sample_payload(token);

    let create = router
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
    let code = body_json(create).await["code"].as_str().unwrap().to_string();

    // Edit amount
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
                .header("x-session-token", token)
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
    let state = setup().await;
    let router = app(state);
    let payload = sample_payload("owner-token");

    let create = router
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
    let code = body_json(create).await["code"].as_str().unwrap().to_string();

    let mut evil = payload.clone();
    evil.jobs.get_mut("modularjobs:miner").unwrap().tasks[0].payables[0].amount =
        "0".to_string();
    evil.metadata.session_token = "owner-token".to_string();

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
async fn healthz_ok() {
    let state = setup().await;
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
