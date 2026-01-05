package net.aincraft.editor.json;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Job data for editor operations.
 * Contains display name and all tasks associated with the job.
 */
public record JobData(
    @SerializedName("displayName")
    @NotNull String displayName,

    @SerializedName("tasks")
    @NotNull List<TaskData> tasks
) {

  /**
   * Creates job data for export.
   *
   * @param displayName human-readable job name
   * @param tasks list of tasks for this job
   * @return new job data instance
   */
  public static JobData create(@NotNull String displayName, @NotNull List<TaskData> tasks) {
    return new JobData(displayName, tasks);
  }
}
