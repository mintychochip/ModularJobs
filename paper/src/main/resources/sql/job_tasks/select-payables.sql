SELECT p.payable_type_key, p.amount, p.currency_identifier FROM job_task_payables p JOIN job_tasks t ON p.job_task_id = t.task_id WHERE t.job_key=? AND t.action_type_key=? AND t.context_key=?;
