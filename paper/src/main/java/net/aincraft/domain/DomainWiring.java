package net.aincraft.domain;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.aincraft.config.YamlConfiguration;
import net.aincraft.container.ActionType;
import net.aincraft.container.PayableType;
import net.aincraft.domain.MemoryJobRepositoryImpl.YamlRecordLoader;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.domain.MemoryJobRepositoryImpl;
import net.aincraft.domain.RelationalJobTaskRepositoryImpl;
import net.aincraft.registry.Registry;
import net.aincraft.repository.ConnectionSource;
import net.aincraft.repository.PluginResources;
import net.aincraft.domain.JobResolver;
import net.aincraft.service.JobService;
import net.aincraft.service.JoinGate;
import net.aincraft.service.YamlJobTaskLoader;
import net.aincraft.util.KeyResolver;
import org.bukkit.plugin.Plugin;

/**
 * Manual composition for domain-layer services (replaces Guice DomainModule).
 */
public final class DomainWiring {

  public static final String LIVE_REPOSITORY = "job_progression";
  public static final String ARCHIVE_REPOSITORY = "archive_job_progression";

  public final MemoryJobRepositoryImpl jobRepository;
  public final RelationalJobTaskRepositoryImpl jobTaskRepository;
  public final ProgressionService progressionService;
  public final JobService jobService;
  public final JobResolver jobResolver;

  private DomainWiring(
      MemoryJobRepositoryImpl jobRepository,
      RelationalJobTaskRepositoryImpl jobTaskRepository,
      ProgressionService progressionService,
      JobService jobService,
      JobResolver jobResolver) {
    this.jobRepository = jobRepository;
    this.jobTaskRepository = jobTaskRepository;
    this.progressionService = progressionService;
    this.jobService = jobService;
    this.jobResolver = jobResolver;
  }

  /**
   * @param connectionSource shared payable DB source (already tracked by {@code resources})
   * @param resources        registers progression write-back flush hooks for disable
   */
  public static DomainWiring create(
      Plugin plugin,
      ConnectionSource connectionSource,
      PluginResources resources,
      Registry<ActionType> actionTypeRegistry,
      Registry<PayableType> payableTypeRegistry,
      KeyResolver keyResolver,
      JoinGate joinGate) {
    YamlRecordLoader loader = new YamlRecordLoader();
    Map<String, JobRecord> records = loader.load(YamlConfiguration.create(plugin, "jobs.yml"));
    MemoryJobRepositoryImpl jobRepository = new MemoryJobRepositoryImpl(records);

    YamlJobTaskLoader taskLoader = new YamlJobTaskLoader(plugin, connectionSource);
    taskLoader.loadIfEmpty();
    RelationalJobTaskRepositoryImpl jobTaskRepository = new RelationalJobTaskRepositoryImpl(connectionSource);

    // Reuse the composition-owned payable ConnectionSource for progression tables
    // (same DB section as before; avoids untracked extra pools).
    WriteBackJobProgressionRepositoryImpl live = WriteBackJobProgressionRepositoryImpl.create(
        plugin,
        RelationalJobProgressionRepositoryImpl.create(
            jobRepository, connectionSource, LIVE_REPOSITORY),
        50, 50, 10, TimeUnit.SECONDS);
    WriteBackJobProgressionRepositoryImpl archive = WriteBackJobProgressionRepositoryImpl.create(
        plugin,
        RelationalJobProgressionRepositoryImpl.create(
            jobRepository, connectionSource, ARCHIVE_REPOSITORY),
        50, 50, 10, TimeUnit.SECONDS);
    resources.onFlush(live::flushPending);
    resources.onFlush(archive::flushPending);

    JobProgressionRepository liveView = live;
    JobProgressionRepository archiveView = archive;
    ProgressionService progressionService = new ProgressionService(liveView, archiveView);
    JobService jobService = new JobServiceImpl(
        actionTypeRegistry,
        payableTypeRegistry,
        jobTaskRepository,
        keyResolver,
        jobRepository,
        progressionService,
        joinGate,
        plugin);
    JobResolver jobResolver = new JobResolver(jobService);
    return new DomainWiring(
        jobRepository, jobTaskRepository, progressionService, jobService, jobResolver);
  }
}
