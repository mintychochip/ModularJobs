CREATE TABLE IF NOT EXISTS job_progression
(
    player_id  TEXT NOT NULL,
    job_key    TEXT NOT NULL,
    experience TEXT NOT NULL DEFAULT '0.0',
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
    target_id TEXT NOT NULL,
    source_id TEXT NOT NULL,
    epoch_millis INTEGER NOT NULL,
    duration BLOB NULL,
    boost_source BLOB NOT NULL,
    PRIMARY KEY (target_id, source_id)
);
