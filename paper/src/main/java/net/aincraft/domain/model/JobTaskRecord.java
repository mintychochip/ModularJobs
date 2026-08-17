package net.aincraft.domain.model;

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable definition of a single task within a job, tying an action to its payout
 * {@link PayableRecord}s within a context.
 *
 * @param jobKey         the job key this task belongs to
 * @param actionTypeKey  the action type this task tracks
 * @param contextKey     the context scoping the action (may be {@code null} for global tasks)
 * @param payables       the reward records awarded for completing the task
 */
public record JobTaskRecord(@NotNull String jobKey, String actionTypeKey, String contextKey,
                            List<PayableRecord> payables) {

  /**
   * Composite key identifying a task by its job, action type, and context.
   *
   * @param jobKey        the job key
   * @param actionTypeKey the action type key
   * @param contextKey    the context key
   */
  public record JobTaskRecordKey(String jobKey, String actionTypeKey, String contextKey) {}
}
