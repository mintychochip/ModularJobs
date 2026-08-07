-- PostgreSQL schema for ModularJobs (remote-compatible DDL).
-- Uses SERIAL and NUMERIC for identity columns and precise decimal amounts.

CREATE TABLE IF NOT EXISTS job_progression
(
    player_id  TEXT           NOT NULL,
    job_key    TEXT           NOT NULL,
    experience NUMERIC(38, 10) NOT NULL DEFAULT 0.0,
    PRIMARY KEY (player_id, job_key)
);

CREATE TABLE IF NOT EXISTS archive_job_progression
(
    player_id  TEXT           NOT NULL,
    job_key    TEXT           NOT NULL,
    experience NUMERIC(38, 10) NOT NULL,
    PRIMARY KEY (player_id, job_key)
);

CREATE TABLE IF NOT EXISTS time_boost_identity
(
    target_id TEXT NOT NULL,
    source_id TEXT NOT NULL,
    PRIMARY KEY (target_id, source_id)
);

CREATE TABLE IF NOT EXISTS time_boosts
(
    target_id    TEXT    NOT NULL,
    source_id    TEXT    NOT NULL,
    epoch_millis BIGINT  NOT NULL,
    duration     BYTEA   NULL,
    boost_source BYTEA   NOT NULL,
    PRIMARY KEY (target_id, source_id)
);

CREATE TABLE IF NOT EXISTS payable_records
(
    job_key          TEXT            NOT NULL,
    action_type_key  TEXT            NOT NULL,
    context_key      TEXT            NOT NULL,
    payable_type_key TEXT            NOT NULL,
    amount           NUMERIC(38, 10) NOT NULL,
    currency         TEXT            NOT NULL,
    PRIMARY KEY (job_key, action_type_key, context_key, payable_type_key)
);

CREATE TABLE IF NOT EXISTS job_tasks
(
    task_id         SERIAL PRIMARY KEY,
    job_key         TEXT NOT NULL,
    action_type_key TEXT NOT NULL,
    context_key     TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS job_task_payables
(
    job_task_id         INTEGER         NOT NULL,
    payable_type_key    TEXT            NOT NULL,
    amount              NUMERIC(38, 10) NOT NULL,
    currency_identifier TEXT            NULL,
    FOREIGN KEY (job_task_id) REFERENCES job_tasks (task_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS player_upgrades
(
    player_id          TEXT    NOT NULL,
    job_key            TEXT    NOT NULL,
    total_skill_points INTEGER NOT NULL DEFAULT 0,
    unlocked_nodes     TEXT    NOT NULL DEFAULT '',
    -- v2 skill tree: JSON map of nodeKey -> level (authoritative when non-empty)
    node_levels        TEXT    NOT NULL DEFAULT '',
    PRIMARY KEY (player_id, job_key)
);

CREATE TABLE IF NOT EXISTS job_pet_selections
(
    player_id       VARCHAR(36)  NOT NULL,
    job_key         VARCHAR(255) NOT NULL,
    pet_config_name VARCHAR(255) NOT NULL,
    selected_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_id, job_key)
);

-- Editor sessions for the secure web session API (shared Postgres store).
CREATE TABLE IF NOT EXISTS editor_sessions
(
    session_code   TEXT PRIMARY KEY,
    session_token  TEXT            NOT NULL,
    payload        JSONB           NOT NULL,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMPTZ     NOT NULL
);
