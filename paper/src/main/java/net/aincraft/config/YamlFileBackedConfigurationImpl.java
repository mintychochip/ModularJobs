package net.aincraft.config;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/** Internal file-backed implementation behind the public YAML configuration proxy. */
final class YamlFileBackedConfigurationImpl {

  private final Plugin plugin;
  private final String path;
  private YamlConfiguration config;
  private File configFile;

  YamlFileBackedConfigurationImpl(Plugin plugin, String path) {
    this.plugin = plugin;
    this.path = path;
    this.configFile = new File(plugin.getDataFolder(), path);
    plugin.getSLF4JLogger().info("Loading config file: {}, exists: {}, data folder: {}", path, configFile.exists(), plugin.getDataFolder());
    if (!configFile.exists()) {
      plugin.getSLF4JLogger().info("Config file doesn't exist, saving from resources...");
      plugin.saveResource(path, false);
      plugin.getSLF4JLogger().info("Config file saved, exists now: {}", configFile.exists());
    }
    config = YamlConfiguration.loadConfiguration(configFile);
    plugin.getSLF4JLogger().info("Loaded config with keys: {}", config.getKeys(false));
  }

  /** Creates a proxy exposing Bukkit configuration access plus reload/save operations. */
  static net.aincraft.config.YamlConfiguration create(Plugin plugin, String path) {
    String[] split = path.split("\\.");
    Preconditions.checkArgument(split.length >= 2);
    Preconditions.checkArgument(split[1].equals("yml") || split[1].equals("yaml"));
    YamlFileBackedConfigurationImpl impl = new YamlFileBackedConfigurationImpl(plugin, path);
    YamlConfiguration config = impl.config;
    return (net.aincraft.config.YamlConfiguration) Proxy.newProxyInstance(
        Thread.currentThread().getContextClassLoader(),
        new Class[]{
            net.aincraft.config.YamlConfiguration.class}, (proxy, method, args) -> {
          if ("getPlugin".equals(method.getName())) {
            return impl.getPlugin();
          }
          if ("reload".equals(method.getName())) {
            impl.reload();
            return null;
          }
          if ("save".equals(method.getName())) {
            impl.save();
            return null;
          }
          return method.invoke(config, args);
        });
  }

  /** Returns the plugin that owns this configuration file. */
  @NotNull
  Plugin getPlugin() {
    return plugin;
  }

  /** Reloads the current YAML file from the plugin data folder. */
  void reload() {
    configFile = new File(plugin.getDataFolder(), path);
    config = YamlConfiguration.loadConfiguration(configFile);
  }

  /** Persists the current configuration to its YAML file. */
  void save() {
    try {
      config.save(configFile);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
