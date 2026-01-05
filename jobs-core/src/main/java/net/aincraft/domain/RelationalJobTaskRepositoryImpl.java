package net.aincraft.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.domain.model.PayableRecord;
import net.aincraft.domain.repository.JobTaskRepository;
import net.aincraft.repository.ConnectionSource;

final class RelationalJobTaskRepositoryImpl implements JobTaskRepository {

  private static final Duration CACHE_TIME_TO_LIVE = Duration.ofMinutes(10);
  private static final int CACHE_MAXIMUM_SIZE = 10_000;
  private final ConnectionSource connectionSource;

  private final Cache<String, JobTaskRecord> readCache = Caffeine.newBuilder()
      .expireAfterWrite(CACHE_TIME_TO_LIVE).maximumSize(CACHE_MAXIMUM_SIZE).build();

  private static final String GET_RECORDS_MAP = """
      SELECT t.context_key, t.task_id, t.action_type_key, 
             p.payable_type_key, p.amount, p.currency
      FROM job_tasks t
      LEFT JOIN job_task_payables p ON p.job_task_id = t.task_id
      WHERE t.job_key = ?
      ORDER BY t.action_type_key, t.task_id
      """;

  public RelationalJobTaskRepositoryImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public JobTaskRecord load(String jobKey, String actionTypeKey, String contextKey) {
    String cacheKey = jobKey + actionTypeKey + contextKey;
    JobTaskRecord taskRecord = readCache.getIfPresent(cacheKey);
    if (taskRecord != null) {
      return taskRecord;
    }
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT p.payable_type_key, p.amount, p.currency FROM job_task_payables p JOIN job_tasks t ON p.job_task_id = t.task_id WHERE t.job_key=? AND t.action_type_key=? AND t.context_key=?;")) {
      ps.setString(1, jobKey);
      ps.setString(2, actionTypeKey);
      ps.setString(3, contextKey);
      try (ResultSet rs = ps.executeQuery()) {
        List<PayableRecord> records = new ArrayList<>();
        while (rs.next()) {
          String payableTypeKey = rs.getString("payable_type_key");
          BigDecimal amount = rs.getBigDecimal("amount");
          String currency = rs.getString("currency");
          PayableRecord record = new PayableRecord(payableTypeKey, amount, currency);
          records.add(record);
        }
        taskRecord = new JobTaskRecord(jobKey, actionTypeKey, contextKey, records);
        readCache.put(cacheKey, taskRecord);
        return taskRecord;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean save(JobTaskRecord record) {
    String cacheKey = createCacheKey(record.jobKey(), record.actionTypeKey(), record.contextKey());
    try (Connection connection = connectionSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        // Check if task exists
        Integer taskId = null;
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT task_id FROM job_tasks WHERE job_key=? AND action_type_key=? AND context_key=?")) {
          ps.setString(1, record.jobKey());
          ps.setString(2, record.actionTypeKey());
          ps.setString(3, record.contextKey());
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              taskId = rs.getInt("task_id");
            }
          }
        }

        if (taskId == null) {
          // Insert new task
          try (PreparedStatement ps = connection.prepareStatement(
              "INSERT INTO job_tasks (job_key, action_type_key, context_key) VALUES (?, ?, ?)",
              PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.jobKey());
            ps.setString(2, record.actionTypeKey());
            ps.setString(3, record.contextKey());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
              if (rs.next()) {
                taskId = rs.getInt(1);
              }
            }
          }
        } else {
          // Delete existing payables for update
          try (PreparedStatement ps = connection.prepareStatement(
              "DELETE FROM job_task_payables WHERE job_task_id=?")) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
          }
        }

        // Insert payables
        if (taskId != null && record.payables() != null) {
          try (PreparedStatement ps = connection.prepareStatement(
              "INSERT INTO job_task_payables (job_task_id, payable_type_key, amount, currency_identifier) VALUES (?, ?, ?, ?)")) {
            for (PayableRecord payable : record.payables()) {
              ps.setInt(1, taskId);
              ps.setString(2, payable.payableTypeKey());
              ps.setBigDecimal(3, payable.amount());
              ps.setString(4, payable.currencyIdentifier());
              ps.addBatch();
            }
            ps.executeBatch();
          }
        }

        connection.commit();
        readCache.put(cacheKey, record);
        return true;
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean delete(String jobKey, String actionTypeKey, String contextKey) {
    String cacheKey = createCacheKey(jobKey, actionTypeKey, contextKey);
    try (Connection connection = connectionSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        // Get task_id first
        Integer taskId = null;
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT task_id FROM job_tasks WHERE job_key=? AND action_type_key=? AND context_key=?")) {
          ps.setString(1, jobKey);
          ps.setString(2, actionTypeKey);
          ps.setString(3, contextKey);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              taskId = rs.getInt("task_id");
            }
          }
        }

        if (taskId == null) {
          return false;
        }

        // Delete payables first (foreign key)
        try (PreparedStatement ps = connection.prepareStatement(
            "DELETE FROM job_task_payables WHERE job_task_id=?")) {
          ps.setInt(1, taskId);
          ps.executeUpdate();
        }

        // Delete task
        try (PreparedStatement ps = connection.prepareStatement(
            "DELETE FROM job_tasks WHERE task_id=?")) {
          ps.setInt(1, taskId);
          ps.executeUpdate();
        }

        connection.commit();
        readCache.invalidate(cacheKey);
        return true;
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, List<JobTaskRecord>> getRecords(String jobKey) {
    Map<String, Map<Integer, TaskRecordAccumulator>> actionTypeTaskMap = new LinkedHashMap<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            GET_RECORDS_MAP)) {
      ps.setString(1, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          int taskId = rs.getInt("task_id");
          String actionTypeKey = rs.getString("action_type_key");
          String payableTypeKey = rs.getString("payable_type_key");
          BigDecimal amount = rs.getBigDecimal("amount");
          String currency = rs.getString("currency");
          String contextKey = rs.getString("context_key");
          Map<Integer, TaskRecordAccumulator> taskMap = actionTypeTaskMap.computeIfAbsent(
              actionTypeKey, ignored -> new LinkedHashMap<>());
          TaskRecordAccumulator accumulator = taskMap.computeIfAbsent(taskId,
              ignored -> new TaskRecordAccumulator(contextKey));
          if (payableTypeKey != null) {
            accumulator.payables.add(new PayableRecord(payableTypeKey, amount, currency));
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    Map<String, List<JobTaskRecord>> records = new LinkedHashMap<>();
    for (Entry<String, Map<Integer, TaskRecordAccumulator>> entry : actionTypeTaskMap.entrySet()) {
      String actionTypeKey = entry.getKey();
      List<JobTaskRecord> taskRecords = entry.getValue().values().stream()
          .map(a -> new JobTaskRecord(jobKey, actionTypeKey, a.contextKey, List.copyOf(a.payables)))
          .toList();
      records.put(actionTypeKey, taskRecords);
    }
    return records;
  }

  private static final class TaskRecordAccumulator {

    private final String contextKey;
    private final List<PayableRecord> payables = new ArrayList<>();

    private TaskRecordAccumulator(String contextKey) {
      this.contextKey = contextKey;
    }
  }

  @Override
  public List<JobTaskRecord> getRecords(String jobKey, String actionTypeKey) {
    List<JobTaskRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT context_key FROM job_tasks WHERE job_key=? AND action_type_key=?;")) {
      ps.setString(1, jobKey);
      ps.setString(2, actionTypeKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String contextKey = rs.getString("context_key");
          JobTaskRecord record = load(jobKey, actionTypeKey, contextKey);
          records.add(record);
        }
      }
      return records;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<JobTaskRecord> getAllRecords(String jobKey) {
    Map<String, List<JobTaskRecord>> grouped = getRecords(jobKey);
    List<JobTaskRecord> all = new ArrayList<>();
    for (List<JobTaskRecord> records : grouped.values()) {
      all.addAll(records);
    }
    return all;
  }

  private static String createCacheKey(String jobKey, String actionTypeKey, String contextKey) {
    return jobKey + actionTypeKey + contextKey;
  }
}
