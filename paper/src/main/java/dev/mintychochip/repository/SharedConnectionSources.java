package dev.mintychochip.repository;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Deduplicates {@link ConnectionSource} instances for the same MySQL jdbc-url + credentials so
 * payable / timed-boost / upgrades sections sharing one database do not open multiple pools.
 */
public final class SharedConnectionSources {

  private final Plugin plugin;
  private final PluginResources resources;
  private final Map<String, ConnectionSource> byIdentity = new HashMap<>();

  /** Shared connection sources. */
  public SharedConnectionSources(@NotNull Plugin plugin, @NotNull PluginResources resources) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.resources = Objects.requireNonNull(resources, "resources");
  }

  /** Create or reuse a connection source for the given database.yml section. */
  public @NotNull ConnectionSource getOrCreate(@NotNull ConfigurationSection section) {
    String identity = poolIdentity(section);
    ConnectionSource existing = byIdentity.get(identity);
    if (existing != null && !existing.isClosed()) {
      return existing;
    }
    ConnectionSource created =
        resources.track(new ConnectionSourceFactory(plugin, section).create());
    byIdentity.put(identity, created);
    return created;
  }

  /** Number of distinct pool identities currently shared (tests). */
  public int sharedPoolCount() {
    return byIdentity.size();
  }

  static String poolIdentity(@NotNull ConfigurationSection section) {
    String jdbc = section.getString("jdbc-url", "");
    String user = section.getString("username", "");
    return (jdbc + "\0" + user).toLowerCase(Locale.ROOT);
  }
}
