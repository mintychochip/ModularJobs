package net.aincraft.domain;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Map;
import net.aincraft.PayableCurve;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.domain.MemoryJobRepositoryImpl.YamlRecordLoader;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.repository.JobRepository;
import net.aincraft.domain.repository.JobTaskRepository;
import net.aincraft.repository.ConnectionSource;
import net.aincraft.service.JobResolver;
import net.aincraft.service.JobService;
import net.aincraft.service.YamlJobTaskLoader;
import org.bukkit.plugin.Plugin;

public final class DomainModule extends PrivateModule {

  @Override
  protected void configure() {
    install(new ProgressionServiceModule());
    bind(JobService.class).to(JobServiceImpl.class).in(Singleton.class);
    bind(JobResolver.class).to(JobResolverImpl.class).in(Singleton.class);
    expose(JobService.class);
    expose(JobResolver.class);
    expose(JobTaskRepository.class);
    expose(ProgressionService.class);
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
  JobTaskRepository jobTaskRepository(Plugin plugin, ConnectionSource connectionSource) {
    // Load YAML tasks if database is empty
    YamlJobTaskLoader loader = new YamlJobTaskLoader(plugin, connectionSource);
    loader.loadIfEmpty();

    return new RelationalJobTaskRepositoryImpl(connectionSource);
  }
}
