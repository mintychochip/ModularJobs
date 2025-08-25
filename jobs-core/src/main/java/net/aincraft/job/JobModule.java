package net.aincraft.job;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableType;
import net.aincraft.job.JobRecordRepository.JobRecord;
import net.aincraft.job.MemoryJobRecordRepositoryImpl.YamlRecordLoader;
import net.aincraft.job.model.ActionTypeRecord;
import net.aincraft.job.model.JobProgressionRecord;
import net.aincraft.job.model.JobTaskRecord;
import net.aincraft.job.model.PayableRecord;
import net.aincraft.registry.Registry;
import net.aincraft.repository.ConnectionSourceFactory;
import net.aincraft.service.JobService;
import net.aincraft.util.KeyFactory;
import net.aincraft.util.Mapper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public final class JobModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(JobService.class).to(JobServiceImpl.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  Mapper<JobTask, JobTaskRecord> taskRecordMapper(Mapper<Payable, PayableRecord> mapper) {
    return new JobTaskRecordMapper(mapper);
  }

  @Provides
  @Singleton
  Mapper<JobProgression, JobProgressionRecord> progressionRecordMapper(JobService jobService) {
    return new JobsProgressionRecordMapperImpl(jobService);
  }

  @Provides
  @Singleton
  Mapper<Job, JobRecord> jobRecordMapper(KeyFactory keyFactory, Registry<PayableType> registry) {
    return new JobRecordMapperImpl(keyFactory, registry);
  }

  @Provides
  @Singleton
  Mapper<Payable, PayableRecord> payableRecordMapper(Registry<PayableType> registry) {
    return new PayableRecordMapperImpl(registry);
  }

  @Provides
  @Singleton
  Mapper<ActionType, ActionTypeRecord> actionTypeRecordMapper(Registry<ActionType> registry,
      KeyFactory keyFactory) {
    return new ActionTypeRecordMapper(registry, keyFactory);
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
  JobRecordRepository jobRepository(Plugin plugin) {
    YamlRecordLoader loader = new YamlRecordLoader();
    Map<String, JobRecord> records = loader.load(YamlConfiguration.create(plugin, "jobs.yml"));
    return new MemoryJobRecordRepositoryImpl(records);
  }

  @Provides
  @Singleton
  JobsProgressionRepository jobsProgressionRepository(Plugin plugin) {
    YamlConfiguration db = YamlConfiguration.create(plugin, "database.yml");
    ConfigurationSection repositoryConfiguration = db.getConfigurationSection("payable");
    return new RelationalJobsProgressionRepositoryImpl(
        new ConnectionSourceFactory(plugin, repositoryConfiguration).create());
  }
}
