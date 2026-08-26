INSERT INTO time_boosts (target_id, source_id, epoch_millis, duration, boost_source)
VALUES (?, ?, ?, ?, ?)
ON DUPLICATE KEY UPDATE epoch_millis = VALUES(epoch_millis),
duration = VALUES(duration), boost_source = VALUES(boost_source)
