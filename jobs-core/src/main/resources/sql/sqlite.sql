CREATE TABLE IF NOT EXISTS job_progression
(
    player_id  TEXT NOT NULL,
    job_key    TEXT NOT NULL,
    experience TEXT NOT NULL DEFAULT '0.0',
    PRIMARY KEY (player_id, job_key)
);

CREATE TABLE IF NOT EXISTS archived_progressions
(
    player_id  TEXT NOT NULL,
    job_key    TEXT NOT NULL,
    experience TEXT NOT NULL,
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
    epoch_millis INTEGER NOT NULL,
    duration     BLOB    NULL,
    boost_source BLOB    NOT NULL,
    PRIMARY KEY (target_id, source_id)
);

CREATE TABLE IF NOT EXISTS payable_records
(
    job_key          TEXT NOT NULL,
    action_type_key  TEXT NOT NULL,
    context_key      TEXT NOT NULL,
    payable_type_key TEXT NOT NULL,
    amount           TEXT NOT NULL,
    currency         TEXT NOT NULL,
    PRIMARY KEY (job_key, action_type_key, context_key, payable_type_key)
);

CREATE TABLE IF NOT EXISTS job_tasks
(
    task_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    job_key         TEXT NOT NULL,
    action_type_key TEXT NOT NULL,
    context_key     TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS job_task_payables
(
    job_task_id      INTEGER NOT NULL,
    payable_type_key TEXT    NOT NULL,
    amount           TEXT    NOT NULL,
    currency         TEXT    NULL,
    FOREIGN KEY (job_task_id) REFERENCES job_tasks (task_id) ON DELETE CASCADE
);


