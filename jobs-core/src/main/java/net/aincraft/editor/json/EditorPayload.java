package net.aincraft.editor.json;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Root payload for web editor export/import operations.
 * Contains all data needed to edit and restore job configurations.
 */
public record EditorPayload(
    @SerializedName("version")
    int version,

    @SerializedName("metadata")
    @NotNull EditorMetadata metadata,

    @SerializedName("jobs")
    @NotNull Map<String, JobData> jobs,

    @SerializedName("registeredActionTypes")
    @NotNull List<String> registeredActionTypes,

    @SerializedName("registeredPayableTypes")
    @NotNull List<String> registeredPayableTypes
) {

  /**
   * Current schema version.
   */
  public static final int CURRENT_VERSION = 1;

  /**
   * Creates a payload with the current schema version.
   *
   * @param metadata export metadata
   * @param jobs job key to job data mapping
   * @param registeredActionTypes available action type keys
   * @param registeredPayableTypes available payable type keys
   * @return new payload instance
   */
  public static EditorPayload create(
      @NotNull EditorMetadata metadata,
      @NotNull Map<String, JobData> jobs,
      @NotNull List<String> registeredActionTypes,
      @NotNull List<String> registeredPayableTypes) {
    return new EditorPayload(CURRENT_VERSION, metadata, jobs, registeredActionTypes,
        registeredPayableTypes);
  }
}
