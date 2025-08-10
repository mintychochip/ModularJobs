package net.aincraft.bridge;

import java.io.File;
import java.io.IOException;
import net.aincraft.config.FileBackedConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

public final class YamlFileBackedConfigurationImpl implements FileBackedConfiguration {

  private final String path;
  private final Plugin plugin;
  private YamlConfiguration config;
  private File configFile;

  public YamlFileBackedConfigurationImpl(String path, Plugin plugin) {
    this.path = path;
    this.plugin = plugin;
    this.configFile = new File(plugin.getDataFolder(), path);
    if (!configFile.exists()) {
      plugin.saveResource(path, false);
    }
    assert (configFile != null);
    config = YamlConfiguration.loadConfiguration(configFile);
  }

  @Internal
  YamlConfiguration getConfig() {
    return config;
  }

  @Override
  public @NotNull Plugin getPlugin() {
    return plugin;
  }

  @Override
  public void reload() {
    try {
      configFile = new File(plugin.getDataFolder(), path);
      config = YamlConfiguration.loadConfiguration(configFile);
    } catch (NullPointerException | IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void save() {
    try {
      config.save(configFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
