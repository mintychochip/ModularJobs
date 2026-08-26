-- MySQL 8 schema for ModularJobs. Apply out-of-band; the plugin never runs DDL.

CREATE TABLE IF NOT EXISTS job_progression
(
    player_id  VARCHAR(191)   NOT NULL,
    job_key    VARCHAR(191)   NOT NULL,
    experience DECIMAL(38, 10) NOT NULL DEFAULT 0.0,
    PRIMARY KEY (player_id, job_key)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS archive_job_progression
(
    player_id  VARCHAR(191)    NOT NULL,
    job_key    VARCHAR(191)    NOT NULL,
    experience DECIMAL(38, 10) NOT NULL,
    PRIMARY KEY (player_id, job_key)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS time_boost_identity
(
    target_id VARCHAR(191) NOT NULL,
    source_id VARCHAR(191) NOT NULL,
    PRIMARY KEY (target_id, source_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS time_boosts
(
    target_id    VARCHAR(191) NOT NULL,
    source_id    VARCHAR(191) NOT NULL,
    epoch_millis BIGINT       NOT NULL,
    duration     BLOB         NULL,
    boost_source BLOB         NOT NULL,
    PRIMARY KEY (target_id, source_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payable_records
(
    job_key          VARCHAR(191)    NOT NULL,
    action_type_key  VARCHAR(191)    NOT NULL,
    context_key      VARCHAR(191)    NOT NULL,
    payable_type_key VARCHAR(191)    NOT NULL,
    amount           DECIMAL(38, 10) NOT NULL,
    currency         VARCHAR(191)    NOT NULL,
    PRIMARY KEY (job_key, action_type_key, context_key, payable_type_key)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_tasks
(
    task_id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    job_key         VARCHAR(191) NOT NULL,
    action_type_key VARCHAR(191) NOT NULL,
    context_key     VARCHAR(191) NOT NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_task_payables
(
    job_task_id         INT UNSIGNED    NOT NULL,
    payable_type_key    VARCHAR(191)    NOT NULL,
    amount              DECIMAL(38, 10) NOT NULL,
    currency_identifier VARCHAR(191)    NULL,
    FOREIGN KEY (job_task_id) REFERENCES job_tasks (task_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS player_upgrades
(
    player_id          VARCHAR(191) NOT NULL,
    job_key            VARCHAR(191) NOT NULL,
    total_skill_points INT         NOT NULL DEFAULT 0,
    unlocked_nodes     VARCHAR(4096) NOT NULL DEFAULT '',
    node_levels        VARCHAR(4096) NOT NULL DEFAULT '',
    PRIMARY KEY (player_id, job_key)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS editor_sessions
(
    session_code  VARCHAR(191) NOT NULL,
    session_token VARCHAR(191) NOT NULL,
    payload       JSON         NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (session_code)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
