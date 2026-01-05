package net.aincraft.domain;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import java.util.Map;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.PayableCurve;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.container.Payable;
import net.aincraft.domain.JobRecordDomainMapperImpl.PayableCurveMapperImpl;
import net.aincraft.domain.MemoryJobRepositoryImpl.YamlRecordLoader;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.domain.model.PayableRecord;
import net.aincraft.domain.repository.JobRepository;
import net.aincraft.domain.repository.JobTaskRepository;
import net.aincraft.repository.ConnectionSourceFactory;
import net.aincraft.service.JobService;
import net.aincraft.util.DomainMapper;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public final class DomainModule extends PrivateModule {
  
  @Override
  protected void configure() {
    install(new ProgressionServiceModule());
    bind(new TypeLiteral<DomainMapper<Job, JobRecord>>() {
    })
        .to(JobRecordDomainMapperImpl.class)
        .in(Singleton.class);
    bind(new TypeLiteral<DomainMapper<JobProgression, JobProgressionRecord>>() {
    })
        .to(JobsProgressionRecordDomainMapperImpl.class)
        .in(Singleton.class);
    bind(new TypeLiteral<DomainMapper<JobTask, JobTaskRecord>>() {
    })
        .to(JobTaskRecordDomainMapperImpl.class)
        .in(Singleton.class);
    bind(new TypeLiteral<DomainMapper<Payable, PayableRecord>>() {
    })
        .to(PayableRecordDomainMapperImpl.class)
        .in(Singleton.class);
    bind(new TypeLiteral<DomainMapper<Map<Key, PayableCurve>, Map<String, String>>>() {
    })
        .to(PayableCurveMapperImpl.class)
        .in(Singleton.class);
    bind(JobService.class).to(JobServiceImpl.class).in(Singleton.class);
    expose(JobService.class);
    expose(JobTaskRepository.class);
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
  JobTaskRepository jobTaskRepository(Plugin plugin) {
    YamlConfiguration configuration = YamlConfiguration.create(plugin, "database.yml");
    ConfigurationSection repositoryConfiguration = configuration.getConfigurationSection("payable");
    return new RelationalJobTaskRepositoryImpl(
        new ConnectionSourceFactory(plugin, repositoryConfiguration).create());
  }
}
