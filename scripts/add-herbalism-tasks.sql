\set ON_ERROR_STOP on

BEGIN;
-- Data-only migration. Table creation remains owned by mysql.sql and the
-- operator-run schema script.
WITH seed(job_key, action_type_key, context_key, experience, economy) AS (
  VALUES
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:short_grass', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:fern', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:dandelion', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:poppy', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:brown_mushroom', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:red_mushroom', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:tall_grass', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:large_fern', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:azure_bluet', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:red_tulip', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:orange_tulip', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:white_tulip', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:pink_tulip', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:lily_pad', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:seagrass', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:kelp', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:blue_orchid', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:allium', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:oxeye_daisy', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:cornflower', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:lily_of_the_valley', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:moss_block', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:moss_carpet', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:glow_lichen', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:hanging_roots', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:spore_blossom', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:big_dripleaf', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:small_dripleaf', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:big_dripleaf_stem', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:sea_pickle', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:crimson_fungus', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:warped_fungus', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:nether_sprouts', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:twisting_vines', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:weeping_vines', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:wither_rose', 8::numeric, 3::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:torchflower', 8::numeric, 3::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:pink_petals', 8::numeric, 3::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:pitcher_plant', 8::numeric, 3::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:pitcher_crop', 8::numeric, 3::numeric)
)
INSERT INTO job_tasks (job_key, action_type_key, context_key)
SELECT seed.job_key, seed.action_type_key, seed.context_key
FROM seed
WHERE NOT EXISTS (
  SELECT 1
  FROM job_tasks existing
  WHERE existing.job_key = seed.job_key
    AND existing.action_type_key = seed.action_type_key
    AND existing.context_key = seed.context_key
);

WITH seed(job_key, action_type_key, context_key, experience, economy) AS (
  VALUES
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:short_grass', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:fern', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:dandelion', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:poppy', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:brown_mushroom', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:red_mushroom', 1::numeric, 0.1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:tall_grass', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:large_fern', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:azure_bluet', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:red_tulip', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:orange_tulip', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:white_tulip', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:pink_tulip', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:lily_pad', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:seagrass', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:kelp', 2::numeric, 0.2::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:blue_orchid', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:allium', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:oxeye_daisy', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:cornflower', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:lily_of_the_valley', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:moss_block', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:moss_carpet', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:glow_lichen', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:hanging_roots', 3::numeric, 0.5::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:spore_blossom', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:big_dripleaf', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:small_dripleaf', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:big_dripleaf_stem', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:sea_pickle', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:crimson_fungus', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:warped_fungus', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:nether_sprouts', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:twisting_vines', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:weeping_vines', 5::numeric, 1::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:wither_rose', 8::numeric, 3::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:torchflower', 8::numeric, 3::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:pink_petals', 8::numeric, 3::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:pitcher_plant', 8::numeric, 3::numeric),
    ('modularjobs:herbalism', 'modularjobs:block_break', 'minecraft:pitcher_crop', 8::numeric, 3::numeric)
)
INSERT INTO job_task_payables (job_task_id, payable_type_key, amount, currency_identifier)
SELECT tasks.task_id, payable.payable_type_key, payable.amount, NULL
FROM seed
JOIN job_tasks tasks
  ON tasks.job_key = seed.job_key
 AND tasks.action_type_key = seed.action_type_key
 AND tasks.context_key = seed.context_key
CROSS JOIN LATERAL (
  VALUES
    ('modularjobs:experience', seed.experience),
    ('modularjobs:economy', seed.economy)
) AS payable(payable_type_key, amount)
WHERE NOT EXISTS (
  SELECT 1
  FROM job_task_payables existing
  WHERE existing.job_task_id = tasks.task_id
    AND existing.payable_type_key = payable.payable_type_key
);

COMMIT;
