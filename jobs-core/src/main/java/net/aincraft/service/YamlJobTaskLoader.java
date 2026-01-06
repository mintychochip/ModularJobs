package net.aincraft.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.aincraft.repository.ConnectionSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class YamlJobTaskLoader {

  private final Plugin plugin;
  private final ConnectionSource connectionSource;
  private final Logger logger;

  public YamlJobTaskLoader(@NotNull Plugin plugin, @NotNull ConnectionSource connectionSource) {
    this.plugin = plugin;
    this.connectionSource = connectionSource;
    this.logger = plugin.getLogger();
  }

  public void loadIfEmpty() {
    if (!isTableEmpty()) {
      logger.info("Job tasks already exist in database, skipping import");
      return;
    }

    // Extract CSV if not present
    File csvFile = new File(plugin.getDataFolder(), "job_tasks.csv");
    if (!csvFile.exists()) {
      plugin.saveResource("job_tasks.csv", false);
    }

    // Check for CSV first (priority)
    if (csvFile.exists()) {
      logger.info("Job tasks table is empty, importing from job_tasks.csv");
      int imported = importFromCsv(csvFile);
      logger.info("Successfully imported " + imported + " job tasks from job_tasks.csv");
      return;
    }

    // Fall back to YAML
    logger.info("Job tasks table is empty, importing from job_tasks.yml");
    plugin.saveResource("job_tasks.yml", false);
    File yamlFile = new File(plugin.getDataFolder(), "job_tasks.yml");
    FileConfiguration config = YamlConfiguration.loadConfiguration(yamlFile);

    int imported = importTasks(config);
    logger.info("Successfully imported " + imported + " job tasks from job_tasks.yml");
  }

  private boolean isTableEmpty() {
    try (Connection connection = connectionSource.getConnection();
         Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM job_tasks")) {
      return rs.next() && rs.getInt(1) == 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check if job_tasks table is empty", e);
    }
  }

  private int importTasks(FileConfiguration config) {
    Set<String> jobKeys = config.getKeys(false);
    List<TaskEntry> tasks = new ArrayList<>();

    for (String jobKey : jobKeys) {
      ConfigurationSection jobSection = config.getConfigurationSection(jobKey);
      if (jobSection == null) continue;

      Set<String> actionTypes = jobSection.getKeys(false);
      for (String actionType : actionTypes) {
        ConfigurationSection actionSection = jobSection.getConfigurationSection(actionType);
        if (actionSection == null) continue;

        Set<String> contextKeys = actionSection.getKeys(false);
        for (String contextKey : contextKeys) {
          ConfigurationSection contextSection = actionSection.getConfigurationSection(contextKey);
          if (contextSection == null) continue;

          List<PayableEntry> payables = new ArrayList<>();
          for (String payableType : contextSection.getKeys(false)) {
            Object value = contextSection.get(payableType);
            if (value instanceof Number number) {
              BigDecimal amount = BigDecimal.valueOf(number.doubleValue());
              String namespacedPayableType = ensureNamespace(payableType, "modularjobs");
              payables.add(new PayableEntry(namespacedPayableType, amount, null));
            }
          }

          if (!payables.isEmpty()) {
            String namespacedActionType = ensureNamespace(actionType, "modularjobs");
            String namespacedContextKey = ensureNamespace(contextKey, "minecraft");
            tasks.add(new TaskEntry(jobKey, namespacedActionType, namespacedContextKey, payables));
          }
        }
      }
    }

    batchInsert(tasks);
    return tasks.size();
  }


  private int importFromCsv(File csvFile) {
    Map<String, TaskEntry> taskMap = new LinkedHashMap<>();

    try {
      List<String> lines = java.nio.file.Files.readAllLines(csvFile.toPath());
      // Skip header line
      for (int i = 1; i < lines.size(); i++) {
        String line = lines.get(i).trim();
        if (line.isEmpty()) continue;

        String[] parts = line.split(",");
        if (parts.length < 5) continue;

        String jobKey = parts[0].trim();
        String actionType = parts[1].trim();
        String contextKey = parts[2].trim();
        String payableType = parts[3].trim();
        BigDecimal amount = new BigDecimal(parts[4].trim());
        String currency = parts.length > 5 ? parts[5].trim() : null;
        if (currency != null && currency.isEmpty()) currency = null;

        // Group by task key
        String taskKey = jobKey + "|" + actionType + "|" + contextKey;
        TaskEntry entry = taskMap.computeIfAbsent(taskKey,
            k -> new TaskEntry(jobKey, actionType, contextKey, new ArrayList<>()));
        entry.payables().add(new PayableEntry(payableType, amount, currency));
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read job_tasks.csv", e);
    }

    List<TaskEntry> tasks = new ArrayList<>(taskMap.values());
    batchInsert(tasks);
    return tasks.size();
  }

  private String ensureNamespace(String key, String defaultNamespace) {
    return key.contains(":") ? key : defaultNamespace + ":" + key;
  }

  private void batchInsert(List<TaskEntry> tasks) {
    String insertTask = "INSERT INTO job_tasks (job_key, action_type_key, context_key) VALUES (?, ?, ?)";
    String insertPayable = "INSERT INTO job_task_payables (job_task_id, payable_type_key, amount, currency_identifier) VALUES (?, ?, ?, ?)";

    try (Connection connection = connectionSource.getConnection()) {
      connection.setAutoCommit(false);

      try (PreparedStatement taskStmt = connection.prepareStatement(insertTask, Statement.RETURN_GENERATED_KEYS);
           PreparedStatement payableStmt = connection.prepareStatement(insertPayable)) {

        for (TaskEntry task : tasks) {
          taskStmt.setString(1, task.jobKey);
          taskStmt.setString(2, task.actionType);
          taskStmt.setString(3, task.contextKey);
          taskStmt.executeUpdate();

          int taskId;
          try (ResultSet generatedKeys = taskStmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
              taskId = generatedKeys.getInt(1);
            } else {
              throw new SQLException("Failed to get generated task_id for " + task.jobKey + ":" + task.actionType + ":" + task.contextKey);
            }
          }

          for (PayableEntry payable : task.payables) {
            payableStmt.setInt(1, taskId);
            payableStmt.setString(2, payable.payableType);
            payableStmt.setString(3, payable.amount.toString());
            payableStmt.setString(4, payable.currency);
            payableStmt.addBatch();
          }
        }

        payableStmt.executeBatch();
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to batch insert job tasks", e);
    }
  }

  private record TaskEntry(String jobKey, String actionType, String contextKey, List<PayableEntry> payables) {}

  private record PayableEntry(String payableType, BigDecimal amount, String currency) {}
}
