package net.aincraft.editor;

import java.util.List;

/**
 * Result of an import operation.
 *
 * @param tasksImported number of tasks successfully imported
 * @param tasksDeleted number of tasks deleted during import
 * @param errors list of error messages encountered during import
 */
public record ImportResult(
    int tasksImported,
    int tasksDeleted,
    List<String> errors
) {
}
