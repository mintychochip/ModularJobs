package net.aincraft.repository;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.serialization.CodecRegistry;
import net.aincraft.service.JobService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public final class RepositoryModule extends AbstractModule {

  @Provides
  @Singleton
  public ConnectionSource connectionSource(Plugin plugin,
      @Named("database") YamlConfiguration configuration) {
    Preconditions.checkArgument(configuration.contains("payable"));
    ConfigurationSection repositoryConfiguration = configuration.getConfigurationSection("payable");
    return new ConnectionSourceFactory(plugin, repositoryConfiguration).create();
  }

  @Provides
  @Singleton
  public TimedBoostRepository timedBoostRepository(Plugin plugin,
      @Named("database") YamlConfiguration configuration,
      CodecRegistry codecRegistry) {
    Preconditions.checkArgument(configuration.contains("timed-boost"));
    ConfigurationSection repositoryConfiguration = configuration.getConfigurationSection(
        "timed-boost");
    return new RelationalTimedBoostRepositoryImpl(plugin,
        new ConnectionSourceFactory(plugin, repositoryConfiguration).create(), codecRegistry);
  }

}
