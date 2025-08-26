package net.aincraft.domain.repository;

import java.util.List;
import java.util.Map;
import net.aincraft.domain.model.JobTaskRecord;
import org.jetbrains.annotations.Nullable;

public interface JobTaskRepository {

  @Nullable
  JobTaskRecord load(String jobKey, String actionTypeKey, String contextKey);

  Map<String,List<JobTaskRecord>> getRecords(String jobKey);

  List<JobTaskRecord> getRecords(String jobKey, String actionTypeKey);
}
