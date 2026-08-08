\set ON_ERROR_STOP on

BEGIN;

-- Data-only migration. Table creation remains owned by postgres.sql and the
-- operator-run schema script.
DO $$
DECLARE
  conflicts bigint;
BEGIN
  SELECT COUNT(*) INTO conflicts
  FROM job_progression old_row
  JOIN job_progression new_row ON new_row.player_id = old_row.player_id
  WHERE old_row.job_key = 'modularjobs:fisher'
    AND new_row.job_key = 'modularjobs:fisherman';
  IF conflicts > 0 THEN
    RAISE EXCEPTION 'fisher -> fisherman conflict in job_progression (% rows)', conflicts;
  END IF;

  SELECT COUNT(*) INTO conflicts
  FROM archive_job_progression old_row
  JOIN archive_job_progression new_row ON new_row.player_id = old_row.player_id
  WHERE old_row.job_key = 'modularjobs:fisher'
    AND new_row.job_key = 'modularjobs:fisherman';
  IF conflicts > 0 THEN
    RAISE EXCEPTION 'fisher -> fisherman conflict in archive_job_progression (% rows)', conflicts;
  END IF;

  SELECT COUNT(*) INTO conflicts
  FROM player_upgrades old_row
  JOIN player_upgrades new_row ON new_row.player_id = old_row.player_id
  WHERE old_row.job_key = 'modularjobs:fisher'
    AND new_row.job_key = 'modularjobs:fisherman';
  IF conflicts > 0 THEN
    RAISE EXCEPTION 'fisher -> fisherman conflict in player_upgrades (% rows)', conflicts;
  END IF;

  SELECT COUNT(*) INTO conflicts
  FROM job_tasks old_row
  JOIN job_tasks new_row
    ON new_row.action_type_key = old_row.action_type_key
   AND new_row.context_key = old_row.context_key
  WHERE old_row.job_key = 'modularjobs:fisher'
    AND new_row.job_key = 'modularjobs:fisherman';
  IF conflicts > 0 THEN
    RAISE EXCEPTION 'fisher -> fisherman conflict in job_tasks (% rows)', conflicts;
  END IF;

  SELECT COUNT(*) INTO conflicts
  FROM payable_records old_row
  JOIN payable_records new_row
    ON new_row.action_type_key = old_row.action_type_key
   AND new_row.context_key = old_row.context_key
   AND new_row.payable_type_key = old_row.payable_type_key
  WHERE old_row.job_key = 'modularjobs:fisher'
    AND new_row.job_key = 'modularjobs:fisherman';
  IF conflicts > 0 THEN
    RAISE EXCEPTION 'fisher -> fisherman conflict in payable_records (% rows)', conflicts;
  END IF;
END
$$;

UPDATE job_progression
SET job_key = 'modularjobs:fisherman'
WHERE job_key = 'modularjobs:fisher';

UPDATE archive_job_progression
SET job_key = 'modularjobs:fisherman'
WHERE job_key = 'modularjobs:fisher';

UPDATE player_upgrades
SET job_key = 'modularjobs:fisherman'
WHERE job_key = 'modularjobs:fisher';

-- job_task_payables has no job_key column. Its rows follow task_id, so
-- updating job_tasks preserves every payable row through the existing FK.
UPDATE job_tasks
SET job_key = 'modularjobs:fisherman'
WHERE job_key = 'modularjobs:fisher';

UPDATE payable_records
SET job_key = 'modularjobs:fisherman'
WHERE job_key = 'modularjobs:fisher';

COMMIT;
