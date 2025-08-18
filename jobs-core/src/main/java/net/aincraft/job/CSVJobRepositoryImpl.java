package net.aincraft.job;

import java.util.Optional;
import net.aincraft.Job;

public class CSVJobRepositoryImpl implements JobRepository {

  @Override
  public Optional<Job> getJob(String jobKey) {
    return Optional.empty();
  }
}
