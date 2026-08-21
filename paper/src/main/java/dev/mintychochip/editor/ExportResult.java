package dev.mintychochip.editor;

/**
 * Result of a REST editor export operation.
 *
 * @param sessionCode public session code used by {@code /jobs applyedits}
 * @param webEditorUrl full URL to the web editor with the session loaded
 * @param sessionToken secret token delivered in the URL fragment
 */
public record ExportResult(
    String sessionCode,
    String webEditorUrl,
    String sessionToken
) {
}
