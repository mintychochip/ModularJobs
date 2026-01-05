package net.aincraft.editor.json;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Task data for editor operations.
 * Represents a single action within a job with associated rewards.
 */
public record TaskData(
    @SerializedName("actionTypeKey")
    @NotNull String actionTypeKey,

    @SerializedName("contextKey")
    @NotNull String contextKey,

    @SerializedName("payables")
    @NotNull List<PayableData> payables
) {

  /**
   * Creates task data for export.
   *
   * @param actionTypeKey namespaced key for action type (e.g. "modularjobs:block_break")
   * @param contextKey namespaced key for context (e.g. "minecraft:stone")
   * @param payables list of rewards for this task
   * @return new task data instance
   */
  public static TaskData create(
      @NotNull String actionTypeKey,
      @NotNull String contextKey,
      @NotNull List<PayableData> payables) {
    return new TaskData(actionTypeKey, contextKey, payables);
  }
}
