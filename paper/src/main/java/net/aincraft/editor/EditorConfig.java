package net.aincraft.editor;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Configuration for the REST-backed web editor.
 */
public record EditorConfig(
    boolean enabled,
    String sessionApiUrl,
    String webEditorUrl,
    String sessionCreateSecret,
    int sessionTtlMinutes
) {
    public static final String DEFAULT_SESSION_API_URL = "";
    public static final String DEFAULT_WEB_EDITOR_URL = "";
    public static final int DEFAULT_SESSION_TTL = 24 * 60;

    public static EditorConfig defaults() {
        return new EditorConfig(
            false,
            DEFAULT_SESSION_API_URL,
            DEFAULT_WEB_EDITOR_URL,
            "",
            DEFAULT_SESSION_TTL);
    }

    public static EditorConfig fromPlugin(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        EditorConfig defaults = defaults();
        return new EditorConfig(
            config.getBoolean("editor.enabled", defaults.enabled()),
            string(config, "editor.session-api-url", defaults.sessionApiUrl()),
            string(config, "editor.web-editor-url", defaults.webEditorUrl()),
            string(config, "editor.session-create-secret", defaults.sessionCreateSecret()),
            Math.max(1, config.getInt("editor.session-ttl-minutes", defaults.sessionTtlMinutes())));
    }

    private static String string(FileConfiguration config, String path, String fallback) {
        String value = config.getString(path);
        return value == null || value.isBlank() ? fallback : value;
    }
}
