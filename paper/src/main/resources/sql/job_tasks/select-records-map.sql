SELECT t.context_key, t.task_id, t.action_type_key,
       p.payable_type_key, p.amount, p.currency_identifier
FROM job_tasks t
LEFT JOIN job_task_payables p ON p.job_task_id = t.task_id
WHERE t.job_key = ?
ORDER BY t.action_type_key, t.task_id
