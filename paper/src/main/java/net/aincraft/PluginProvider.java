package net.aincraft;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper-only access to the ModularJobs {@link JavaPlugin} instance for schedulers and similar.
 */
public final class PluginProvider {

  private static JavaPlugin plugin;

  private PluginProvider() {}

  public static void set(JavaPlugin p) {
    plugin = p;
  }

  public static JavaPlugin get() {
    if (plugin == null) {
      throw new IllegalStateException("Plugin not set");
    }
    return plugin;
  }
}
