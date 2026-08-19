INSERT INTO player_upgrades (player_id, job_key, total_skill_points, unlocked_nodes)
VALUES (?, ?, ?, ?)
ON DUPLICATE KEY UPDATE
total_skill_points = VALUES(total_skill_points),
unlocked_nodes = VALUES(unlocked_nodes)
