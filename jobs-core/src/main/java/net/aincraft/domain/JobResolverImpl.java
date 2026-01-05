package net.aincraft.domain;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.aincraft.Job;
import net.aincraft.service.JobResolver;
import net.aincraft.service.JobService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Singleton
final class JobResolverImpl implements JobResolver {

  private final JobService jobService;

  @Inject
  public JobResolverImpl(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public @Nullable Job resolve(@NotNull String identifier) {
    // Check if full namespacedkey format (contains ':')
    if (identifier.contains(":")) {
      try {
        return jobService.getJob(identifier);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

    // Plain name lookup (case-insensitive)
    String normalizedInput = identifier.toLowerCase(Locale.ROOT);
    return jobService.getJobs().stream()
        .filter(job -> job.getPlainName().toLowerCase(Locale.ROOT).equals(normalizedInput))
        .findFirst()
        .orElse(null);
  }

  @Override
  public @Nullable Job resolveInNamespace(@NotNull String plainName, @NotNull String namespace) {
    // Try exact namespacedkey first
    String fullKey = namespace + ":" + plainName.toLowerCase(Locale.ROOT);
    try {
      Job job = jobService.getJob(fullKey);
      if (job != null) {
        return job;
      }
    } catch (IllegalArgumentException e) {
      // Continue to fallback
    }

    // Fallback: search by plain name within namespace
    String normalizedInput = plainName.toLowerCase(Locale.ROOT);
    return jobService.getJobs().stream()
        .filter(job -> job.key().namespace().equals(namespace))
        .filter(job -> job.getPlainName().toLowerCase(Locale.ROOT).equals(normalizedInput))
        .findFirst()
        .orElse(null);
  }

  @Override
  public @NotNull List<String> suggestSimilar(@NotNull String input, int maxSuggestions) {
    String normalizedInput = input.toLowerCase(Locale.ROOT);

    return jobService.getJobs().stream()
        .map(job -> new ScoredJob(job, calculateSimilarity(normalizedInput, job.getPlainName())))
        .filter(scored -> scored.score() > 0) // Only include matches with positive scores
        .sorted(Comparator.comparingInt(ScoredJob::score).reversed())
        .limit(maxSuggestions)
        .map(scored -> scored.job().getPlainName())
        .collect(Collectors.toList());
  }

  @Override
  public @NotNull List<String> getPlainNames() {
    return jobService.getJobs().stream()
        .map(Job::getPlainName)
        .collect(Collectors.toList());
  }

  /**
   * Calculate similarity score between input and job name.
   * Higher score = better match.
   * <p>
   * Scoring system:
   * - Exact match: 1000 points
   * - Prefix match: 500 + bonus for length similarity
   * - Contains match: 250 points
   * - Levenshtein distance: 100 - (distance * 10), capped at 0
   * - Very dissimilar matches (distance > length/2): 0 points (filtered out)
   */
  private int calculateSimilarity(String input, String jobName) {
    String normalizedJobName = jobName.toLowerCase(Locale.ROOT);

    // Exact match (shouldn't typically reach here, but included for completeness)
    if (normalizedJobName.equals(input)) {
      return 1000;
    }

    // Prefix match (high priority)
    if (normalizedJobName.startsWith(input)) {
      int lengthDiff = Math.abs(normalizedJobName.length() - input.length());
      return 500 + (100 - lengthDiff);
    }

    // Contains match (medium priority)
    if (normalizedJobName.contains(input)) {
      return 250;
    }

    // Levenshtein distance (lower distance = higher score)
    int distance = levenshteinDistance(input, normalizedJobName);
    int maxLength = Math.max(input.length(), normalizedJobName.length());

    // Reject very dissimilar matches
    if (distance > maxLength / 2) {
      return 0;
    }

    return Math.max(0, 100 - (distance * 10));
  }

  /**
   * Calculate Levenshtein distance between two strings.
   * <p>
   * The Levenshtein distance is the minimum number of single-character edits
   * (insertions, deletions, or substitutions) required to change one string into the other.
   *
   * @param a First string
   * @param b Second string
   * @return Levenshtein distance
   */
  private int levenshteinDistance(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];

    // Initialize base cases
    for (int i = 0; i <= a.length(); i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= b.length(); j++) {
      dp[0][j] = j;
    }

    // Fill the DP table
    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
        dp[i][j] = Math.min(
            Math.min(dp[i - 1][j] + 1,      // Deletion
                dp[i][j - 1] + 1),           // Insertion
            dp[i - 1][j - 1] + cost          // Substitution
        );
      }
    }

    return dp[a.length()][b.length()];
  }

  /**
   * Internal record for pairing jobs with their similarity scores during fuzzy matching.
   */
  private record ScoredJob(Job job, int score) {}
}
