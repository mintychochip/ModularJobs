package net.aincraft.job;

import java.util.Map;
import net.aincraft.job.JobRecordRepository.JobRecord;

public interface JobRecordLoader {

  Map<String, JobRecord> load();

  void reload();
}
