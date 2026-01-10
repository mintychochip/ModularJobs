package net.aincraft.domain;

import com.google.inject.Inject;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.LevelingCurve;
import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import net.aincraft.container.PayableType;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.domain.repository.JobRepository;
import net.aincraft.domain.repository.JobTaskRepository;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.event.JobJoinEvent;
import net.aincraft.event.JobLeaveEvent;
import net.aincraft.registry.Registry;
import net.aincraft.service.JobService;
import net.aincraft.util.KeyResolver;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class JobServiceImpl implements JobService {

  private final Registry<ActionType> actionTypeRegistry;
  private final Registry<PayableType> payableTypeRegistry;
  private final JobTaskRepository jobTaskRepository;
  private final KeyResolver keyResolver;
  private final JobRepository jobRepository;
  private final ProgressionService progressionService;
  private final Plugin plugin;

  @Inject
  public JobServiceImpl(
      Registry<ActionType> actionTypeRegistry,
      Registry<PayableType> payableTypeRegistry,
      JobTaskRepository jobTaskRepository,
      KeyResolver keyResolver,
      JobRepository jobRepository,
      ProgressionService progressionService,
      Plugin plugin) {
    this.actionTypeRegistry = actionTypeRegistry;
    this.payableTypeRegistry = payableTypeRegistry;
    this.jobTaskRepository = jobTaskRepository;
    this.keyResolver = keyResolver;
    this.jobRepository = jobRepository;
    this.progressionService = progressionService;
    this.plugin = plugin;
  }

  @Override
  public @NotNull List<Job> getJobs() {
    return jobRepository.getJobs().stream()
        .map(r -> PersistenceConverters.fromRecord(r, plugin, payableTypeRegistry))
        .toList();
  }

  @Override
  public Job getJob(String jobKey) throws IllegalArgumentException {
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
  public boolean joinJob(String playerId, String jobKey) throws IllegalArgumentException {
    JobRecord jobRecord = jobRepository.load(jobKey);
    if (jobRecord == null) {
      throw new IllegalArgumentException("failed to joined job, the job does not exist");
    }

    UUID uuid = UUID.fromString(playerId);
    Player player = Bukkit.getPlayer(uuid);
    Job job = PersistenceConverters.fromRecord(jobRecord, plugin, payableTypeRegistry);

    // Try to restore from archive first (rejoin case)
    if (progressionService.restore(playerId, jobKey)) {
      if (player != null) {
        JobProgressionRecord restored = progressionService.load(playerId, jobKey);
        int level = PersistenceConverters.fromRecord(restored, plugin, payableTypeRegistry).level();
        Bukkit.getPluginManager().callEvent(new JobJoinEvent(player, job, level, true));
      }
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
      if (player != null) {
        Bukkit.getPluginManager().callEvent(new JobJoinEvent(player, job, 1, false));
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean leaveJob(String playerId, String jobKey) throws IllegalArgumentException {
    JobProgressionRecord record = progressionService.load(playerId, jobKey);
    if (record == null) {
      return false;
    }
    // Convert record to domain to get level
    JobProgression progression = PersistenceConverters.fromRecord(record, plugin, payableTypeRegistry);
    int finalLevel = progression.level();
    Job job = progression.job();

    // Get player if online and fire event
    UUID uuid = UUID.fromString(playerId);
    Player player = Bukkit.getPlayer(uuid);
    if (player != null) {
      JobLeaveEvent event = new JobLeaveEvent(player, job, finalLevel);
      Bukkit.getPluginManager().callEvent(event);
    }

    return progressionService.archive(playerId,jobKey);
  }

  @Override
  public JobProgression getProgression(String playerId, String jobKey)
      throws IllegalArgumentException {
    // Ensure jobKey has proper namespace
    String fullJobKey = jobKey.contains(":") ? jobKey : "modularjobs:" + jobKey;

    JobProgressionRecord record = progressionService.load(playerId, fullJobKey);
    if (record == null) {
      return null;
    }
    return PersistenceConverters.fromRecord(record, plugin, payableTypeRegistry);
  }

  @Override
  public List<JobProgression> getProgressions(OfflinePlayer player) {
    return progressionService.loadAllForPlayer(player.getUniqueId().toString(), 100).stream()
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
  public List<JobProgression> getArchivedProgressions(OfflinePlayer player) {
    return progressionService.loadAllArchivedForPlayer(player.getUniqueId().toString(), 100).stream()
        .map(r -> PersistenceConverters.fromRecord(r, plugin, payableTypeRegistry))
        .toList();
  }
}
