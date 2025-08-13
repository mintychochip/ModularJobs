CREATE TABLE IF NOT EXISTS job_progression (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id TEXT NOT NULL,
    job_key TEXT NOT NULL,
    experience REAL NOT NULL DEFAULT 0.0
);

CREATE TABLE IF NOT EXISTS job_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_key TEXT NOT NULL,
    action_type_key TEXT NOT NULL,
    context_key TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS job_payables (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    payable_key TEXT NOT NULL,
    amount TEXT NOT NULL,
    FOREIGN KEY (task_id) REFERENCES job_tasks(id) ON DELETE CASCADE
);
