package net.aincraft.config;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.bukkit.plugin.Plugin;

public final class ConfigurationModule extends AbstractModule {

  private final Plugin plugin;

  public ConfigurationModule(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  protected void configure() {
    YamlConfiguration configuration = YamlConfiguration.create(plugin, "database.yml");
    bind(YamlConfiguration.class).annotatedWith(Names.named("database"))
        .toInstance(configuration);
  }

}
