package dev.mintychochip.editor;

import dev.mintychochip.Job;
import dev.mintychochip.JobTask;
import dev.mintychochip.common.editor.EditorMetadata;
import dev.mintychochip.common.editor.EditorPayload;
import dev.mintychochip.common.editor.JobData;
import dev.mintychochip.common.editor.PayableData;
import dev.mintychochip.common.editor.TaskData;
import dev.mintychochip.container.ActionType;
import dev.mintychochip.container.Payable;
import dev.mintychochip.container.PayableType;
import dev.mintychochip.domain.RelationalJobTaskRepositoryImpl;
import dev.mintychochip.domain.model.JobTaskRecord;
import dev.mintychochip.domain.model.PayableRecord;
import dev.mintychochip.registry.RegistryContainer;
import dev.mintychochip.registry.RegistryKeys;
import dev.mintychochip.registry.RegistryView;
import dev.mintychochip.service.JobService;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

/** Editor service. */
public final class EditorService {

  private final JobService jobService;
  private final RelationalJobTaskRepositoryImpl jobTaskRepository;
  private final RestSessionClient restSessionClient;
  private final EditorSessionStore sessionStore;
  private final EditorConfig config;

  /** Editor service. */
  public EditorService(
      JobService jobService,
      RelationalJobTaskRepositoryImpl jobTaskRepository,
      RestSessionClient restSessionClient,
      EditorSessionStore sessionStore,
      EditorConfig config) {
    this.jobService = jobService;
    this.jobTaskRepository = jobTaskRepository;
    this.restSessionClient = restSessionClient;
    this.sessionStore = sessionStore;
    this.config = config;
  }

  private record ExportDraft(UUID playerId, EditorPayload payload) {}

  /** Created export. */
  private record CreatedExport(UUID playerId, RestSessionClient.CreatedSession session) {}

  /** Export tasks. */
  public CompletableFuture<ExportResult> exportTasks(@Nullable String jobKey, UUID playerId) {
    return CompletableFuture.supplyAsync(
            () -> {
              String sessionToken = UUID.randomUUID().toString();

              List<Job> jobs =
                  jobKey != null ? List.of(getJobOrThrow(jobKey)) : jobService.getJobs();

              Map<String, JobData> jobDataMap = new LinkedHashMap<>();
              for (Job job : jobs) {
                jobDataMap.put(job.key().toString(), buildJobData(job));
              }

              List<String> actionTypes = getRegisteredActionTypes();
              List<String> payableTypes = getRegisteredPayableTypes();
              EditorMetadata metadata =
                  EditorMetadata.create(
                      Instant.now().toString(), playerId.toString(), sessionToken, getServerName());
              EditorPayload payload =
                  EditorPayload.create(metadata, jobDataMap, actionTypes, payableTypes);
              return new ExportDraft(playerId, payload);
            })
        .thenCompose(
            draft ->
                restSessionClient
                    .create(draft.payload())
                    .thenApply(created -> new CreatedExport(draft.playerId(), created)))
        .thenApply(
            export -> {
              RestSessionClient.CreatedSession created = export.session();
              EditorSession session =
                  new EditorSession(
                      created.sessionCode(),
                      created.token(),
                      export.playerId(),
                      Instant.now(),
                      created.expiresAt());
              sessionStore.store(session);
              String webEditorUrl =
                  editorUrl(
                      config.webEditorUrl(),
                      config.sessionApiUrl(),
                      created.sessionCode(),
                      created.token());
              return new ExportResult(created.sessionCode(), webEditorUrl, created.token());
            })
        .exceptionally(
            failure -> {
              Throwable error =
                  failure instanceof CompletionException completion && completion.getCause() != null
                      ? completion.getCause()
                      : failure;
              throw new EditorException("Failed to export tasks: " + error.getMessage(), error);
            });
  }

  /** Import tasks. */
  public CompletableFuture<ImportResult> importTasks(String sessionCode, UUID playerId) {
    return CompletableFuture.supplyAsync(
        () -> {
          List<String> errors = new ArrayList<>();
          int tasksImported = 0;
          int tasksDeleted = 0;

          EditorSession session = sessionStore.getOwned(sessionCode, playerId).orElse(null);
          if (session == null) {
            errors.add(
                "Editor session is missing, expired, or belongs to another player; "
                    + "run /jobs editor again.");
            return new ImportResult(0, 0, errors);
          }

          try {
            EditorPayload payload =
                restSessionClient.fetchPayload(session.sessionCode(), session.token()).join();

            if (!session.token().equals(payload.metadata().sessionToken())) {
              errors.add("REST payload session token did not match the authenticated session.");
              return new ImportResult(0, 0, errors);
            }

            for (Map.Entry<String, JobData> entry : payload.jobs().entrySet()) {
              String jobKey = entry.getKey();
              JobData jobData = entry.getValue();
              List<JobTaskRecord> existingTasks = jobTaskRepository.getAllRecords(jobKey);
              Set<String> incomingKeys = new HashSet<>();

              for (TaskData taskData : jobData.tasks()) {
                String key = taskKey(jobKey, taskData.actionTypeKey(), taskData.contextKey());
                incomingKeys.add(key);

                List<PayableRecord> payableRecords = new ArrayList<>();
                for (PayableData pd : taskData.payables()) {
                  payableRecords.add(
                      new PayableRecord(pd.type(), new BigDecimal(pd.amount()), null));
                }
                JobTaskRecord record =
                    new JobTaskRecord(
                        jobKey, taskData.actionTypeKey(), taskData.contextKey(), payableRecords);

                if (jobTaskRepository.save(record)) {
                  tasksImported++;
                }
              }

              for (JobTaskRecord existing : existingTasks) {
                String key =
                    taskKey(existing.jobKey(), existing.actionTypeKey(), existing.contextKey());
                if (!incomingKeys.contains(key)
                    && jobTaskRepository.delete(
                        existing.jobKey(), existing.actionTypeKey(), existing.contextKey())) {
                  tasksDeleted++;
                }
              }
            }

            sessionStore.remove(session.sessionCode());
            return new ImportResult(tasksImported, tasksDeleted, errors);
          } catch (CompletionException e) {
            if (e.getCause() instanceof RestSessionClient.RestSessionException rest) {
              errors.add(
                  rest.expired() ? "Session expired; run /jobs editor again." : rest.getMessage());
              return new ImportResult(tasksImported, tasksDeleted, errors);
            }
            errors.add("Failed to import tasks: " + e.getMessage());
            return new ImportResult(tasksImported, tasksDeleted, errors);
          } catch (IllegalArgumentException | IllegalStateException e) {
            errors.add("Failed to import tasks: " + e.getMessage());
            return new ImportResult(tasksImported, tasksDeleted, errors);
          }
        });
  }

  static String editorUrl(String base, String apiBase, String code, String token) {
    String normalized = base.replaceFirst("/+$", "") + "/";
    String encodedApi = encode(apiBase);
    return normalized + "?api=" + encodedApi + "&code=" + encode(code) + "#token=" + encode(token);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private String taskKey(String jobKey, String actionTypeKey, String contextKey) {
    return jobKey + "|" + actionTypeKey + "|" + contextKey;
  }

  /** Builds JobData from a Job instance. */
  private JobData buildJobData(Job job) {
    Map<ActionType, List<JobTask>> tasksByAction = jobService.getAllTasks(job);
    List<TaskData> tasks = new ArrayList<>();

    for (Map.Entry<ActionType, List<JobTask>> entry : tasksByAction.entrySet()) {
      for (JobTask task : entry.getValue()) {
        TaskData taskData = buildTaskData(task);
        tasks.add(taskData);
      }
    }

    return JobData.create(job.getPlainName(), tasks);
  }

  /** Builds TaskData from a JobTask instance. */
  private TaskData buildTaskData(JobTask task) {
    List<PayableData> payables =
        task.payables().stream().map(this::buildPayableData).collect(Collectors.toList());

    return TaskData.create(task.actionTypeKey().toString(), task.contextKey().toString(), payables);
  }

  /** Builds PayableData from a Payable instance. */
  private PayableData buildPayableData(Payable payable) {
    PayableType type = payable.type();
    String amount = payable.amount().value().toString();
    return PayableData.create(type.key().toString(), amount);
  }

  /** Gets all registered action type keys. */
  private List<String> getRegisteredActionTypes() {
    RegistryView<ActionType> registry =
        RegistryContainer.registryContainer().getRegistry(RegistryKeys.ACTION_TYPES);
    return registry.stream().map(type -> type.key().toString()).collect(Collectors.toList());
  }

  /** Gets all registered payable type keys. */
  private List<String> getRegisteredPayableTypes() {
    RegistryView<PayableType> registry =
        RegistryContainer.registryContainer().getRegistry(RegistryKeys.PAYABLE_TYPES);
    return registry.stream().map(type -> type.key().toString()).collect(Collectors.toList());
  }

  /** Gets the server name from Bukkit configuration. */
  @Nullable
  private String getServerName() {
    try {
      return Bukkit.getServer().getName();
    } catch (IllegalStateException e) {
      return null;
    }
  }

  /** Gets a job by key or throws an exception. */
  private Job getJobOrThrow(String jobKey) {
    Job job = jobService.getJob(jobKey);
    if (job == null) {
      throw new IllegalArgumentException("Job not found: " + jobKey);
    }
    return job;
  }

  /** Exception thrown when editor operations fail. */
  public static final class EditorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Editor exception. */
    public EditorException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
