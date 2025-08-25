package net.aincraft.job;

import java.util.List;
import java.util.Optional;
import net.aincraft.job.model.JobProgressionRecord;

public interface JobsProgressionRepository {

  void save(JobProgressionRecord record);

  Optional<JobProgressionRecord> getRecord(String playerId, String jobKey);

  List<JobProgressionRecord> getRecords(String jobKey);

}
