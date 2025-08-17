CREATE TABLE IF NOT EXISTS job_progression
(
    player_id  TEXT NOT NULL,
    job_key    TEXT NOT NULL,
    experience TEXT NOT NULL DEFAULT '0.0',
    PRIMARY KEY (player_id, job_key)
);

CREATE TABLE IF NOT EXISTS active_time_boosts
(
    identifier TEXT NOT NULL,
    epoch_millis TEXT NOT NULL,
    duration BLOB NULL,
    boost_source BLOB NOT NULL,
    PRIMARY KEY (identifier)
);
