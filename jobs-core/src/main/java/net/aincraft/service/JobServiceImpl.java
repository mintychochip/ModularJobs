package net.aincraft.service;

import com.google.inject.Inject;
import java.util.Optional;
import net.aincraft.Job;
import net.aincraft.job.JobRepository;
import net.kyori.adventure.key.Key;

public final class JobServiceImpl implements JobService {

  private final JobRepository jobRepository;

  @Inject
  public JobServiceImpl(JobRepository jobRepository) {
    this.jobRepository = jobRepository;
  }

  @Override
  public Optional<Job> getJob(Key jobKey) {
    return jobRepository.getJob(jobKey.toString());
  }
}
