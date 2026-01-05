package net.aincraft.editor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

/**
 * Service for managing web editor export/import operations.
 * Handles serialization of job tasks to JSON and persistence via bytebin.
 */
public interface EditorService {

    /**
     * Exports job tasks to the web editor.
     * Creates a session, uploads task data to bytebin, and generates an editor URL.
     *
     * @param jobKey optional job key to export specific job, null to export all jobs
     * @param playerId the player performing the export
     * @return future containing export result with bytebin code, URL, and session token
     */
    CompletableFuture<ExportResult> exportTasks(@Nullable String jobKey, UUID playerId);

    /**
     * Imports job tasks from the web editor.
     * Validates the session, fetches data from bytebin, and persists tasks.
     *
     * @param bytebinCode the bytebin paste code containing edited tasks
     * @param playerId the player performing the import
     * @return future containing import result with counts and error messages
     */
    CompletableFuture<ImportResult> importTasks(String bytebinCode, UUID playerId);
}
