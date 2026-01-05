package net.aincraft.service;

import net.aincraft.Job;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Resolves job identifiers to Job instances with fuzzy matching support.
 * <p>
 * This service provides ergonomic job resolution, allowing users to reference jobs
 * by plain names (e.g., "miner") instead of full namespacedkeys (e.g., "modularjobs:miner").
 * It also supports fuzzy matching to suggest similar job names when exact matches fail.
 */
public interface JobResolver {

  /**
   * Resolves a job by plain name or full namespacedkey.
   * <p>
   * This method accepts both formats:
   * <ul>
   *   <li>Plain name: "miner" (case-insensitive)</li>
   *   <li>Full namespacedkey: "modularjobs:miner"</li>
   * </ul>
   *
   * @param identifier Plain name or full namespacedkey
   * @return Job instance or null if not found
   */
  @Nullable
  Job resolve(@NotNull String identifier);

  /**
   * Resolves a job by plain name, restricted to a specific namespace.
   * <p>
   * This method first attempts to construct a full namespacedkey using the provided
   * namespace and plain name. If that fails, it searches for jobs within the namespace
   * whose plain name matches (case-insensitive).
   *
   * @param plainName Plain job name (e.g., "miner")
   * @param namespace Namespace to search within (e.g., "modularjobs")
   * @return Job instance or null if not found
   */
  @Nullable
  Job resolveInNamespace(@NotNull String plainName, @NotNull String namespace);

  /**
   * Finds similar job names when exact match fails.
   * <p>
   * Uses fuzzy matching algorithms (Levenshtein distance, prefix matching) to suggest
   * jobs that are similar to the user's input. Results are ordered by similarity score.
   *
   * @param input User input that didn't match exactly
   * @param maxSuggestions Maximum number of suggestions to return
   * @return List of suggested job plain names, ordered by similarity (most similar first)
   */
  @NotNull
  List<String> suggestSimilar(@NotNull String input, int maxSuggestions);

  /**
   * Gets all plain names for tab completion.
   * <p>
   * Returns a list of all job plain names (not full namespacedkeys) suitable for
   * use in command tab completion suggestions.
   *
   * @return List of all job plain names
   */
  @NotNull
  List<String> getPlainNames();
}
