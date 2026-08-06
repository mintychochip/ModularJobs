package net.aincraft.commands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import net.aincraft.JobProgression;
import net.aincraft.service.JobService;
import net.kyori.adventure.key.Key;

/**
 * Provides paginated job leaderboard entries with a short-lived read cache.
 */
public final class JobTopPageProvider {

  private static final int ENTRIES_PER_QUERY = 100;

  private final JobService jobService;
  private final Cache<Key, List<JobProgression>> readCache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(10)).build();

  public JobTopPageProvider(JobService jobService) {
    this.jobService = jobService;
  }

  public Page<JobProgression> getPage(Key jobKey, int pageNumber, int pageSize) {
    List<JobProgression> progressions = readCache.get(jobKey,
        __ -> jobService.getProgressions(jobKey, ENTRIES_PER_QUERY));

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

  public List<JobProgression> getAllEntries(Key jobKey) {
    List<JobProgression> progressions = readCache.get(jobKey,
        __ -> jobService.getProgressions(jobKey, ENTRIES_PER_QUERY));
    return progressions != null ? progressions : List.of();
  }
}
