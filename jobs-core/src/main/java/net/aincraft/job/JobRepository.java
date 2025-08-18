package net.aincraft.job;

import java.util.Optional;
import net.aincraft.Job;

public interface JobRepository {
  Optional<Job> getJob(String jobKey);
}
