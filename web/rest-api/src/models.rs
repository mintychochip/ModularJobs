//! Editor payload contract aligned with web/src/lib/types.ts and
//! paper editor JSON records (shared DTOs in common).

use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct EditorPayload {
    pub version: i32,
    pub metadata: EditorMetadata,
    pub jobs: HashMap<String, JobData>,
    pub registered_action_types: Vec<String>,
    pub registered_payable_types: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct EditorMetadata {
    pub exported_at: String,
    pub exported_by: String,
    pub session_token: String,
    pub server_name: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct JobData {
    pub display_name: String,
    pub tasks: Vec<TaskData>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct TaskData {
    pub action_type_key: String,
    pub context_key: String,
    pub payables: Vec<PayableData>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct PayableData {
    #[serde(rename = "type")]
    pub payable_type: String,
    pub amount: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateSessionResponse {
    pub code: String,
    pub token: String,
    pub expires_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEnvelope {
    pub code: String,
    pub payload: EditorPayload,
    pub expires_at: String,
}

/// Accept create body as either a bare EditorPayload or `{ "payload": ... }`.
#[derive(Debug, Deserialize)]
#[serde(untagged)]
pub enum CreateSessionBody {
    Wrapped { payload: EditorPayload },
    Raw(EditorPayload),
}

impl CreateSessionBody {
    pub fn into_payload(self) -> EditorPayload {
        match self {
            CreateSessionBody::Wrapped { payload } => payload,
            CreateSessionBody::Raw(payload) => payload,
        }
    }
}

/// Update body: full payload replacement.
#[derive(Debug, Deserialize)]
pub struct UpdateSessionBody {
    pub payload: Option<EditorPayload>,
    /// Allow bare EditorPayload fields when client posts the payload root.
    #[serde(flatten)]
    pub raw: Option<Value>,
}

impl UpdateSessionBody {
    pub fn into_payload(self) -> Result<EditorPayload, serde_json::Error> {
        if let Some(p) = self.payload {
            return Ok(p);
        }
        // Re-parse flattened raw as EditorPayload
        let v = self.raw.unwrap_or(Value::Null);
        serde_json::from_value(v)
    }
}
