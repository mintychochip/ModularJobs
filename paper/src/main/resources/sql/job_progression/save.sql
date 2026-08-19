INSERT INTO {table} (player_id, job_key, experience)
VALUES (?,?,?)
ON DUPLICATE KEY UPDATE experience = VALUES(experience);
