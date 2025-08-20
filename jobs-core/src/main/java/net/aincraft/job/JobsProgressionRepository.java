package net.aincraft.job;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface JobsProgressionRepository {

  void save(JobProgressionRecord record);

  Optional<JobProgressionRecord> getRecord(String playerId, String jobKey);

  List<JobProgressionRecord> getProgressionRecords();

  record JobProgressionRecord(String playerId, String jobKey, BigDecimal experience) {

  }

}
