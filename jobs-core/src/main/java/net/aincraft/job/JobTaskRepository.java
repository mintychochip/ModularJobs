package net.aincraft.job;

import java.util.List;
import java.util.Map;
import net.aincraft.domain.model.ActionTypeRecord;
import net.aincraft.domain.model.JobTaskRecord;

public interface JobTaskRepository {

  JobTaskRecord getRecord(String jobKey, String actionTypeKey, String contextKey);

  Map<ActionTypeRecord,List<JobTaskRecord>> getRecords(String jobKey);

  List<JobTaskRecord> getRecords(String jobKey, String actionTypeKey);

}
