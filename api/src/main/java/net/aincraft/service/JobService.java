package net.aincraft.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;


/**
 * Provides access to jobs, their task definitions, and player job progression.
 *
 * <p>Job records are typically keyed by string or Adventure key. Methods that
 * address a single job by key throw {@link IllegalArgumentException} when the key
 * is unknown; mutation methods report success via their boolean return value.</p>
 */
public interface JobService {

  /**
   * Returns all jobs known to the service.
   *
   * @return the list of all jobs
   */
  @NotNull
  List<Job> getJobs();

  /**
   * Returns the job identified by the given key.
   *
   * @param jobKey the job key
   * @return the matching job
   * @throws IllegalArgumentException if no job matches the key
   */
  Job getJob(String jobKey) throws IllegalArgumentException;

  /**
   * Returns the task a job defines for the given action type and context.
   *
   * @param job the job to query
   * @param type the action type
   * @param context the action context
   * @return the matching task, or {@code null} if the job defines none
   */
  JobTask getTask(Job job, ActionType type, Context context);

  /**
   * Returns all tasks defined by the given job, grouped by action type.
   *
   * @param job the job to query
   * @return the job's tasks grouped by action type
   */
  Map<ActionType, List<JobTask>> getAllTasks(Job job);

  /**
   * Persists the given progression, returning whether the update succeeded.
   *
   * @param progression the progression to save
   * @return {@code true} if the progression was updated, {@code false} otherwise
   */
  boolean update(JobProgression progression);

  /**
   * Adds the given player to the given job.
   *
   * @param playerId the player identifier
   * @param jobKey the job key
   * @return {@code true} if the player joined, {@code false} otherwise
   * @throws IllegalArgumentException if the job key is unknown
   */
  boolean joinJob(String playerId, String jobKey) throws IllegalArgumentException;

  /**
   * Removes the given player from the given job.
   *
   * @param playerId the player identifier
   * @param jobKey the job key
   * @return {@code true} if the player left, {@code false} otherwise
   * @throws IllegalArgumentException if the job key is unknown
   */
  boolean leaveJob(String playerId, String jobKey) throws IllegalArgumentException;

  /**
   * Returns the player's progression in the given job.
   *
   * @param playerId the player identifier
   * @param jobKey the job key
   * @return the player's progression in the job
   * @throws IllegalArgumentException if the job key is unknown
   */
  JobProgression getProgression(String playerId, String jobKey) throws IllegalArgumentException;

  /**
   * Returns all progressions for the given player.
   *
   * @param playerId the player identifier
   * @return the player's progressions across jobs
   */
  List<JobProgression> getProgressions(UUID playerId);

  /**
   * Returns up to the given number of progressions for the given job.
   *
   * @param jobKey the job key
   * @param limit the maximum number of progressions to return
   * @return the latest progressions for the job, limited by {@code limit}
   */
  List<JobProgression> getProgressions(Key jobKey, int limit);

  /**
   * Returns the archived progressions for the given player.
   *
   * @param playerId the player identifier
   * @return the player's archived progressions
   */
  List<JobProgression> getArchivedProgressions(UUID playerId);
}
