package dev.mintychochip.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * File-backed YAML configuration: plugin resource ops plus {@link ConfigurationSection} accessors.
 */
public interface YamlConfiguration extends ConfigurationSection {

  /** API member. */
  @NotNull
  Plugin getPlugin();

  /** Reload. */
  void reload();

  /** Save. */
  void save();

  /** API member. */
  @NotNull
  static YamlConfiguration create(Plugin plugin, String path) {
    return YamlFileBackedConfigurationImpl.create(plugin, path);
  }
}
