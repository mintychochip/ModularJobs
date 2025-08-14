CREATE TABLE IF NOT EXISTS job_progression (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id TEXT NOT NULL,
    job_key TEXT NOT NULL,
    experience TEXT NOT NULL DEFAULT '0.0'
);
