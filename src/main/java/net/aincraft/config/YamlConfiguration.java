package net.aincraft.config;

import net.aincraft.api.Bridge;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Union interface for {@link FileBackedConfiguration} and {@link ConfigurationSection}
 */
public interface YamlConfiguration extends FileBackedConfiguration, ConfigurationSection {

  @NotNull
  static YamlConfiguration create(Plugin plugin, String path) throws IllegalArgumentException {
    return Bridge.bridge().configurationFactory().yaml(plugin,path);
  }
}
