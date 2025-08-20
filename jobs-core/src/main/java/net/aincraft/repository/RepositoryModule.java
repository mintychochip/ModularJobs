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
  public ProgressionRepository progressionRepository(Plugin plugin,
      @Named("database") YamlConfiguration configuration,
      JobService jobService) {
    Preconditions.checkArgument(configuration.contains("timed-boost"));
    ConfigurationSection repositoryConfiguration = configuration.getConfigurationSection(
        "timed-boost");
    return new RelationalProgressionRepositoryImpl(plugin,
        new ConnectionSourceFactory(plugin, repositoryConfiguration).create(), jobService);
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
