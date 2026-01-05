package net.aincraft.editor;

/**
 * Result of an export operation.
 *
 * @param bytebinCode the paste code for retrieving data from bytebin
 * @param webEditorUrl the full URL to the web editor with the data loaded
 * @param sessionToken the session token for import verification
 */
public record ExportResult(
    String bytebinCode,
    String webEditorUrl,
    String sessionToken
) {
}
