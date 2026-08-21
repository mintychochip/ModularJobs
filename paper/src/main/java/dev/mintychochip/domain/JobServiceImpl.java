package dev.mintychochip.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import dev.mintychochip.Job;
import dev.mintychochip.JobProgression;
import dev.mintychochip.JobTask;
import dev.mintychochip.LevelingCurve;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Context;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.domain.model.JobProgressionRecord;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.model.JobTaskRecord;
import dev.mintychochip.Bridge;
import dev.mintychochip.event.JobJoinEvent;
import dev.mintychochip.event.JobLeaveEvent;
import dev.mintychochip.paper.event.PaperEventBridge;
import dev.mintychochip.registry.Registry;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.JoinGate;
import dev.mintychochip.util.KeyResolver;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link JobService} implementation wiring job definitions, tasks, and
 * player progression together.
 *
 * <p>Job definitions are served from the in-memory {@link MemoryJobRepositoryImpl}
 * (loaded once at composition from {@code jobs.yml}); tasks come from the relational
 * {@link RelationalJobTaskRepositoryImpl}; progression operations are delegated to
 * {@link ProgressionService} which fronts the write-back relational progression
 * stores (live + archive).
 *
 * <p>This class is stateless and safe to share; it does not own connections or
 * background tasks (those live in the repositories it is given).
 *
 * <p>Failure semantics: callers that look up jobs by key might throw unchecked
 * {@link IllegalArgumentException}/{@link IllegalStateException} rather than return
 * a sentinel—see individual methods, and {@link #getProgression(String, String)}
 * returns {@code null} when the player has no progression for that job.
 */
final class JobServiceImpl implements JobService {

  private final Registry<ActionType> actionTypeRegistry;
  private final Registry<PayableType> payableTypeRegistry;
  private final RelationalJobTaskRepositoryImpl jobTaskRepository;
  private final KeyResolver keyResolver;
  private final MemoryJobRepositoryImpl jobRepository;
  private final ProgressionService progressionService;
  private final JoinGate joinGate;
  private final Plugin plugin;

  /**
   * Wires job definitions, tasks, and progression into a single service facade.
   *
   * @param actionTypeRegistry     registry of known action types
   * @param payableTypeRegistry    registry of known payable types
   * @param jobTaskRepository      relational task store
   * @param keyResolver            resolves {@link Context} to keys for task lookup
   * @param jobRepository          in-memory job definitions
   * @param progressionService     live/archive progression store facade
   * @param joinGate               join-eligibility gate
   * @param plugin                 plugin for key namespaces and events
   */
  JobServiceImpl(
      Registry<ActionType> actionTypeRegistry,
      Registry<PayableType> payableTypeRegistry,
      RelationalJobTaskRepositoryImpl jobTaskRepository,
      KeyResolver keyResolver,
      MemoryJobRepositoryImpl jobRepository,
      ProgressionService progressionService,
      JoinGate joinGate,
      Plugin plugin) {
    this.actionTypeRegistry = actionTypeRegistry;
    this.payableTypeRegistry = payableTypeRegistry;
    this.jobTaskRepository = jobTaskRepository;
    this.keyResolver = keyResolver;
    this.jobRepository = jobRepository;
    this.progressionService = progressionService;
    this.joinGate = joinGate;
    this.plugin = plugin;
  }

  @Override
  public @NotNull List<Job> getJobs() {
    return jobRepository.getJobs().stream()
        .map(r -> PersistenceConverters.fromRecord(r, plugin, payableTypeRegistry))
        .toList();
  }

  @Override
  public Job getJob(String jobKey) {
    JobRecord record = jobRepository.load(jobKey);
    if (record == null) {
      throw new IllegalArgumentException();
    }
    return PersistenceConverters.fromRecord(record, plugin, payableTypeRegistry);
  }

  @Override
  public JobTask getTask(Job job, ActionType type, Context context) {
    Key contextKey = keyResolver.resolve(context);
    if (contextKey == null) {
      throw new IllegalStateException("No KeyResolver strategy registered for context type: " + context.getClass().getSimpleName());
    }
    JobTaskRecord record = jobTaskRepository.load(job.key().toString(), type.key().toString(),
        contextKey.toString());
    return PersistenceConverters.fromRecord(record, keyString -> payableTypeRegistry.getOrThrow(Key.key(keyString)));
  }

  @Override
  public Map<ActionType, List<JobTask>> getAllTasks(Job job) {
    Map<String, List<JobTaskRecord>> records = jobTaskRepository.getRecords(
        job.key().toString());
    Map<ActionType, List<JobTask>> domain = new LinkedHashMap<>();
    for (Entry<String, List<JobTaskRecord>> entry : records.entrySet()) {
      ActionType type = actionTypeRegistry.getOrThrow(NamespacedKey.fromString(entry.getKey()));
      List<JobTask> tasks = entry.getValue().stream()
          .map(r -> PersistenceConverters.fromRecord(r, keyString -> payableTypeRegistry.getOrThrow(Key.key(keyString))))
          .toList();
      domain.put(type, tasks);
    }
    return domain;
  }

  @Override
  public boolean update(JobProgression progression) {
    return progressionService.save(PersistenceConverters.toRecord(progression));
  }

  @Override
  public boolean joinJob(String playerId, String jobKey) {
    JobRecord jobRecord = jobRepository.load(jobKey);
    if (jobRecord == null) {
      throw new IllegalArgumentException("failed to joined job, the job does not exist");
    }

    UUID uuid = UUID.fromString(playerId);
    Player player = Bukkit.getPlayer(uuid);
    Job job = PersistenceConverters.fromRecord(jobRecord, plugin, payableTypeRegistry);

    // Enforce join eligibility (max jobs, per-job permission, world restriction) when the
    // player is online. This is the single enforcement point shared by /jobs join and the GUI.
    if (player != null) {
      List<JobProgression> current = progressionService.loadAllForPlayer(playerId, 100).stream()
          .map(r -> PersistenceConverters.fromRecord(r, plugin, payableTypeRegistry))
          .toList();
      JoinGate.JoinResult result = joinGate.canJoin(player, job, current);
      if (result != JoinGate.JoinResult.ALLOWED) {
        return false;
      }
    }

    PaperEventBridge events = new PaperEventBridge(Bridge.bridge().eventBus());

    // Try to restore from archive first (rejoin case)
    if (progressionService.restore(playerId, jobKey)) {
      JobProgressionRecord restored = progressionService.load(playerId, jobKey);
      int level = restored == null
          ? 1
          : PersistenceConverters.fromRecord(restored, plugin, payableTypeRegistry).level();
      events.publishJoin(new JobJoinEvent(uuid, job, level, true), player);
      return true;
    }

    // Check if already in job
    JobProgressionRecord record = progressionService.load(playerId, jobKey);
    if (record != null) {
      return false; // Already in job
    }

    // New join - use starting experience from leveling curve
    BigDecimal startExperience = job.levelingCurve().evaluate(new LevelingCurve.Parameters(1));
    if (progressionService.save(new JobProgressionRecord(playerId, jobRecord, startExperience))) {
      events.publishJoin(new JobJoinEvent(uuid, job, 1, false), player);
      return true;
    }
    return false;
  }

  @Override
  public boolean leaveJob(String playerId, String jobKey) {
    JobProgressionRecord record = progressionService.load(playerId, jobKey);
    if (record == null) {
      return false;
    }
    // Convert record to domain to get level
    JobProgression progression = PersistenceConverters.fromRecord(record, plugin, payableTypeRegistry);
    int finalLevel = progression.level();
    Job job = progression.job();

    UUID uuid = UUID.fromString(playerId);
    Player player = Bukkit.getPlayer(uuid);
    new PaperEventBridge(Bridge.bridge().eventBus())
        .publishLeave(new JobLeaveEvent(uuid, job, finalLevel), player);

    return progressionService.archive(playerId, jobKey);
  }

  @Override
  public JobProgression getProgression(String playerId, String jobKey) {
    // Ensure jobKey has proper namespace
    String fullJobKey = jobKey.contains(":") ? jobKey : "modularjobs:" + jobKey;

    JobProgressionRecord record = progressionService.load(playerId, fullJobKey);
    if (record == null) {
      return null;
    }
    return PersistenceConverters.fromRecord(record, plugin, payableTypeRegistry);
  }

  @Override
  public List<JobProgression> getProgressions(UUID playerId) {
    return progressionService.loadAllForPlayer(playerId.toString(), 100).stream()
        .map(r -> PersistenceConverters.fromRecord(r, plugin, payableTypeRegistry))
        .toList();
  }

  @Override
  public List<JobProgression> getProgressions(Key jobKey, int limit) {
    return progressionService.loadAllForJob(jobKey.toString(), limit).stream()
        .map(r -> PersistenceConverters.fromRecord(r, plugin, payableTypeRegistry))
        .toList();
  }

  @Override
  public List<JobProgression> getArchivedProgressions(UUID playerId) {
    return progressionService.loadAllArchivedForPlayer(playerId.toString(), 100).stream()
        .map(r -> PersistenceConverters.fromRecord(r, plugin, payableTypeRegistry))
        .toList();
  }
}
