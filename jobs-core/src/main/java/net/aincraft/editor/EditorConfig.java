package net.aincraft.editor;

/**
 * Configuration for the web editor feature.
 */
public record EditorConfig(
    boolean enabled,
    String bytebinUrl,
    String webEditorUrl,
    int sessionTtlMinutes
) {
    public static final String DEFAULT_BYTEBIN_URL = "https://bytebin.lucko.me";
    public static final String DEFAULT_WEB_EDITOR_URL = "https://modularjobs.example.com/editor";
    public static final int DEFAULT_SESSION_TTL = 60;

    public static EditorConfig defaults() {
        return new EditorConfig(true, DEFAULT_BYTEBIN_URL, DEFAULT_WEB_EDITOR_URL, DEFAULT_SESSION_TTL);
    }
}
