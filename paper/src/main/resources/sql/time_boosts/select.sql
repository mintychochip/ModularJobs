SELECT source_id, epoch_millis, duration, boost_source FROM time_boosts
WHERE target_id = ? AND source_id = ?
