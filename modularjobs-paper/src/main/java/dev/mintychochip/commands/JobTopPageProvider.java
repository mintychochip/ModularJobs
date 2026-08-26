package dev.mintychochip.commands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.mintychochip.JobProgression;
import dev.mintychochip.service.JobService;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.key.Key;

/** Provides paginated job leaderboard entries with a short-lived read cache. */
public final class JobTopPageProvider {

  private static final int ENTRIES_PER_QUERY = 100;

  private final JobService jobService;
  private final Cache<Key, List<JobProgression>> readCache =
      Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build();

  /**
   * Creates the page provider backed by the job service.
   *
   * @param jobService service used to load progressions for a job key
   */
  public JobTopPageProvider(JobService jobService) {
    this.jobService = jobService;
  }

  /**
   * Returns the requested page of a job's leaderboard, backed by a short-lived cache of the most
   * recent {@value ENTRIES_PER_QUERY} progressions.
   *
   * @param jobKey job whose leaderboard is requested
   * @param pageNumber 1-based requested page, clamped to the available range
   * @param pageSize maximum entries per page
   * @return the page; an empty first page if the job has no progressions
   */
  public Page<JobProgression> getPage(Key jobKey, int pageNumber, int pageSize) {
    List<JobProgression> progressions =
        readCache.get(jobKey, ignoredKey -> jobService.getProgressions(jobKey, ENTRIES_PER_QUERY));

    if (progressions == null || progressions.isEmpty()) {
      return new Page<>(List.of(), 1, pageSize);
    }

    int total = Math.min(ENTRIES_PER_QUERY, progressions.size());
    int totalPages = Math.max(1, (total + pageSize - 1) / pageSize);
    int clamped = Math.min(Math.max(pageNumber, 1), totalPages);

    int from = (clamped - 1) * pageSize;
    int to = Math.min(from + pageSize, total);

    List<JobProgression> slice = progressions.subList(from, to);
    return new Page<>(slice, clamped, pageSize);
  }

  /**
   * Returns all cached leaderboard entries for the given job.
   *
   * @param jobKey job whose progressions are requested
   * @return the cached progressions, or an empty list if none are available
   */
  public List<JobProgression> getAllEntries(Key jobKey) {
    List<JobProgression> progressions =
        readCache.get(jobKey, ignoredKey -> jobService.getProgressions(jobKey, ENTRIES_PER_QUERY));
    return progressions != null ? progressions : List.of();
  }
}
