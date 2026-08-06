package net.aincraft.editor.json;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata about the editor export operation.
 * Includes timestamp, player info, and session verification data.
 */
public record EditorMetadata(
    @SerializedName("exportedAt")
    @NotNull String exportedAt,

    @SerializedName("exportedBy")
    @NotNull String exportedBy,

    @SerializedName("sessionToken")
    @NotNull String sessionToken,

    @SerializedName("serverName")
    @Nullable String serverName
) {

  /**
   * Creates metadata for an export operation.
   *
   * @param exportedAt ISO 8601 timestamp
   * @param exportedBy player UUID as string
   * @param sessionToken session verification token
   * @param serverName optional server identifier
   * @return new metadata instance
   */
  public static EditorMetadata create(
      @NotNull String exportedAt,
      @NotNull String exportedBy,
      @NotNull String sessionToken,
      @Nullable String serverName) {
    return new EditorMetadata(exportedAt, exportedBy, sessionToken, serverName);
  }
}
