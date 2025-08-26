package net.aincraft.job;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Map;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.domain.JobRecordMapperImpl;
import net.aincraft.domain.JobServiceImpl;
import net.aincraft.domain.JobTaskRecordMapper;
import net.aincraft.domain.JobsProgressionRecordMapperImpl;
import net.aincraft.domain.MemoryJobRepositoryImpl;
import net.aincraft.domain.PayableRecordMapperImpl;
import net.aincraft.domain.RelationalJobsProgressionRepositoryImpl;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.repository.JobRepository;
import net.aincraft.domain.repository.JobTaskRepository;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.domain.MemoryJobRepositoryImpl.YamlRecordLoader;
import net.aincraft.repository.ConnectionSourceFactory;
import net.aincraft.service.JobService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public final class JobModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(JobService.class).to(JobServiceImpl.class).in(Singleton.class);
  }

//  @Provides
//  @Singleton
//  JobTaskRepository jobTaskRepository(@Named("database") YamlConfiguration configuration,
//      Plugin plugin) {
//    Preconditions.checkArgument(configuration.contains("payable"));
//    ConfigurationSection repositoryConfiguration = configuration.getConfigurationSection("payable");
//    return new RelationalJobTaskRepositoryImpl(
//        new ConnectionSourceFactory(plugin, repositoryConfiguration).create());
//  }

  @Provides
  @Singleton
  JobTaskRepository jobTaskRepository(Plugin plugin) {
    YamlConfiguration configuration = YamlConfiguration.create(plugin, "database.yml");
    ConfigurationSection repo = configuration.getConfigurationSection("payable");
    return new RelationalJobTaskRepositoryImpl(new ConnectionSourceFactory(plugin, repo).create());
  }

  @Provides
  @Singleton
  JobRepository jobRepository(Plugin plugin) {
    YamlRecordLoader loader = new YamlRecordLoader();
    Map<String, JobRecord> records = loader.load(YamlConfiguration.create(plugin, "jobs.yml"));
    return new MemoryJobRepositoryImpl(records);
  }

  @Provides
  @Singleton
  JobProgressionRepository jobsProgressionRepository(Plugin plugin) {
    YamlConfiguration db = YamlConfiguration.create(plugin, "database.yml");
    ConfigurationSection repositoryConfiguration = db.getConfigurationSection("payable");
    return new RelationalJobsProgressionRepositoryImpl(
        new ConnectionSourceFactory(plugin, repositoryConfiguration).create());
  }
}
