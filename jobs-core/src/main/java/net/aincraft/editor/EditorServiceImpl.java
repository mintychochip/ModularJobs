package net.aincraft.editor;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.aincraft.Job;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableType;
import net.aincraft.editor.json.EditorMetadata;
import net.aincraft.editor.json.EditorPayload;
import net.aincraft.editor.json.JobData;
import net.aincraft.editor.json.PayableData;
import net.aincraft.editor.json.TaskData;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import net.aincraft.registry.RegistryView;
import net.aincraft.service.JobService;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

@Singleton
public final class EditorServiceImpl implements EditorService {

    private final JobService jobService;
    private final BytebinClient bytebinClient;
    private final EditorSessionStore sessionStore;
    private final EditorConfig config;
    private final Gson gson;

    @Inject
    public EditorServiceImpl(
        JobService jobService,
        BytebinClient bytebinClient,
        EditorSessionStore sessionStore,
        EditorConfig config,
        Gson gson) {
        this.jobService = jobService;
        this.bytebinClient = bytebinClient;
        this.sessionStore = sessionStore;
        this.config = config;
        this.gson = gson;
    }

    @Override
    public CompletableFuture<ExportResult> exportTasks(@Nullable String jobKey, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate session token
                String sessionToken = UUID.randomUUID().toString();

                // Get jobs to export
                List<Job> jobs = jobKey != null
                    ? List.of(getJobOrThrow(jobKey))
                    : jobService.getJobs();

                // Build job data map
                Map<String, JobData> jobDataMap = new LinkedHashMap<>();
                for (Job job : jobs) {
                    String key = job.key().toString();
                    JobData jobData = buildJobData(job);
                    jobDataMap.put(key, jobData);
                }

                // Get registered types
                List<String> actionTypes = getRegisteredActionTypes();
                List<String> payableTypes = getRegisteredPayableTypes();

                // Build metadata
                EditorMetadata metadata = EditorMetadata.create(
                    Instant.now().toString(),
                    playerId.toString(),
                    sessionToken,
                    getServerName()
                );

                // Create payload
                EditorPayload payload = EditorPayload.create(
                    metadata,
                    jobDataMap,
                    actionTypes,
                    payableTypes
                );

                // Serialize to JSON
                String json = gson.toJson(payload);

                // Upload to bytebin
                return bytebinClient.post(json)
                    .thenApply(bytebinCode -> {
                        // Create and store session
                        EditorSession session = new EditorSession(
                            sessionToken,
                            playerId,
                            Instant.now(),
                            bytebinCode
                        );
                        sessionStore.store(session);

                        // Build web editor URL (using query parameter for static site compatibility)
                        String webEditorUrl = config.webEditorUrl() + "/session?code=" + bytebinCode;

                        return new ExportResult(bytebinCode, webEditorUrl, sessionToken);
                    })
                    .join();
            } catch (Exception e) {
                throw new EditorException("Failed to export tasks: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<ImportResult> importTasks(String bytebinCode, UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new ArrayList<>();
            int tasksImported = 0;
            int tasksDeleted = 0;

            try {
                // Fetch JSON from bytebin
                String json = bytebinClient.get(bytebinCode).join();

                // Deserialize payload
                EditorPayload payload = gson.fromJson(json, EditorPayload.class);

                // Validate session token
                String sessionToken = payload.metadata().sessionToken();
                if (!sessionStore.validate(sessionToken, playerId)) {
                    errors.add("Invalid session token or session expired");
                    return new ImportResult(0, 0, errors);
                }

                // TODO: Implement actual task persistence
                // For now, just count tasks in the payload
                for (Map.Entry<String, JobData> entry : payload.jobs().entrySet()) {
                    JobData jobData = entry.getValue();
                    tasksImported += jobData.tasks().size();
                }

                // Remove session after successful import
                sessionStore.remove(sessionToken);

                return new ImportResult(tasksImported, tasksDeleted, errors);
            } catch (BytebinClientImpl.BytebinException e) {
                errors.add(e.getMessage());
                return new ImportResult(tasksImported, tasksDeleted, errors);
            } catch (Exception e) {
                errors.add("Failed to import tasks: " + e.getMessage());
                return new ImportResult(tasksImported, tasksDeleted, errors);
            }
        });
    }

    /**
     * Builds JobData from a Job instance.
     */
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

    /**
     * Builds TaskData from a JobTask instance.
     */
    private TaskData buildTaskData(JobTask task) {
        List<PayableData> payables = task.payables().stream()
            .map(this::buildPayableData)
            .collect(Collectors.toList());

        return TaskData.create(
            task.actionTypeKey().toString(),
            task.contextKey().toString(),
            payables
        );
    }

    /**
     * Builds PayableData from a Payable instance.
     */
    private PayableData buildPayableData(Payable payable) {
        PayableType type = payable.type();
        String amount = payable.amount().value().toString();
        return PayableData.create(type.key().toString(), amount);
    }

    /**
     * Gets all registered action type keys.
     */
    private List<String> getRegisteredActionTypes() {
        RegistryView<ActionType> registry = RegistryContainer.registryContainer()
            .getRegistry(RegistryKeys.ACTION_TYPES);
        return registry.stream()
            .map(type -> type.key().toString())
            .collect(Collectors.toList());
    }

    /**
     * Gets all registered payable type keys.
     */
    private List<String> getRegisteredPayableTypes() {
        RegistryView<PayableType> registry = RegistryContainer.registryContainer()
            .getRegistry(RegistryKeys.PAYABLE_TYPES);
        return registry.stream()
            .map(type -> type.key().toString())
            .collect(Collectors.toList());
    }

    /**
     * Gets the server name from Bukkit configuration.
     */
    @Nullable
    private String getServerName() {
        try {
            return Bukkit.getServer().getName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a job by key or throws an exception.
     */
    private Job getJobOrThrow(String jobKey) {
        Job job = jobService.getJob(jobKey);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobKey);
        }
        return job;
    }

    /**
     * Exception thrown when editor operations fail.
     */
    public static final class EditorException extends RuntimeException {
        public EditorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
