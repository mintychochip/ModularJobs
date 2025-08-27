package net.aincraft.domain;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.TimeUnit;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.domain.repository.JobRepository;
import net.aincraft.repository.ConnectionSourceFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

final class ProgressionServiceModule extends PrivateModule {

  static final String LIVE_REPOSITORY = "job_progression";
  static final String ARCHIVE_REPOSITORY = "archive_job_progression";

  @Override
  protected void configure() {
    bind(ProgressionService.class).to(ProgressionServiceImpl.class);
    expose(ProgressionService.class);
  }

  @Provides
  @Singleton
  @Named(LIVE_REPOSITORY)
  JobProgressionRepository liveRepository(JobRepository jobRepository, Plugin plugin) {
    YamlConfiguration database = YamlConfiguration.create(plugin, "database.yml");
    ConfigurationSection repositoryConfiguration = database.getConfigurationSection("payable");
    ConnectionSourceFactory factory = new ConnectionSourceFactory(plugin,
        repositoryConfiguration);
    JobProgressionRepository repository = RelationalJobProgressionRepositoryImpl.create(
        jobRepository, factory.create(), LIVE_REPOSITORY);
    return WriteBackJobProgressionRepositoryImpl.create(plugin, repository, 50, 50, 10,
        TimeUnit.SECONDS);
  }

  @Provides
  @Singleton
  @Named(ARCHIVE_REPOSITORY)
  JobProgressionRepository archiveRepository(JobRepository jobRepository, Plugin plugin) {
    YamlConfiguration database = YamlConfiguration.create(plugin, "database.yml");
    ConfigurationSection repositoryConfiguration = database.getConfigurationSection("payable");
    ConnectionSourceFactory factory = new ConnectionSourceFactory(plugin,
        repositoryConfiguration);
    JobProgressionRepository repository = RelationalJobProgressionRepositoryImpl.create(
        jobRepository, factory.create(), ARCHIVE_REPOSITORY);
    return WriteBackJobProgressionRepositoryImpl.create(plugin, repository, 50, 50, 10,
        TimeUnit.SECONDS);
  }
}
