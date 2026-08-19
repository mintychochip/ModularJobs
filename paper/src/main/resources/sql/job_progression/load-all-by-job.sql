SELECT player_id,experience FROM {table} WHERE job_key = ?
ORDER BY (experience IS NULL), CAST(experience AS DECIMAL(38,10))
DESC LIMIT {limit};
