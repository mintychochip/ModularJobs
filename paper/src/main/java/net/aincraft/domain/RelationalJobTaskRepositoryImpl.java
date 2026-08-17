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
import net.aincraft.domain.RelationalJobTaskRepositoryImpl;
import net.aincraft.repository.ConnectionSource;

/**
 * SQL-backed repository for job task records, keyed by the tuple
 * {@code (jobKey, actionTypeKey, contextKey)}. Reads are serviced through an LRU-style
 * Caffeine cache (10-minute TTL, 10k entry cap) that is invalidated on deletion and
 * refreshed on successful saves; writes run in transactions against the shared
 * {@link ConnectionSource}.
 */
public final class RelationalJobTaskRepositoryImpl {

  private static final Duration CACHE_TIME_TO_LIVE = Duration.ofMinutes(10);
  private static final int CACHE_MAXIMUM_SIZE = 10_000;
  private final ConnectionSource connectionSource;

  /** Read-through cache keyed by (jobKey, actionTypeKey, contextKey). */
  private final Cache<String, JobTaskRecord> readCache = Caffeine.newBuilder()
      .expireAfterWrite(CACHE_TIME_TO_LIVE).maximumSize(CACHE_MAXIMUM_SIZE).build();

  private static final String GET_RECORDS_MAP = """
      SELECT t.context_key, t.task_id, t.action_type_key,
             p.payable_type_key, p.amount, p.currency_identifier
      FROM job_tasks t
      LEFT JOIN job_task_payables p ON p.job_task_id = t.task_id
      WHERE t.job_key = ?
      ORDER BY t.action_type_key, t.task_id
      """;

  /**
   * @param connectionSource the source of database connections for all operations
   */
  public RelationalJobTaskRepositoryImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  /**
   * Loads the task record for the given key tuple, consulting the cache first and
   * populating it on a cache miss. An absent task row yields a record with no payables.
   * @param jobKey the job key
   * @param actionTypeKey the action type key
   * @param contextKey the context key
   * @return the matching task record (never {@code null})
   */
  public JobTaskRecord load(String jobKey, String actionTypeKey, String contextKey) {
    String cacheKey = jobKey + actionTypeKey + contextKey;
    JobTaskRecord taskRecord = readCache.getIfPresent(cacheKey);
    if (taskRecord != null) {
      return taskRecord;
    }
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT p.payable_type_key, p.amount, p.currency_identifier FROM job_task_payables p JOIN job_tasks t ON p.job_task_id = t.task_id WHERE t.job_key=? AND t.action_type_key=? AND t.context_key=?;")) {
      ps.setString(1, jobKey);
      ps.setString(2, actionTypeKey);
      ps.setString(3, contextKey);
      try (ResultSet rs = ps.executeQuery()) {
        List<PayableRecord> records = new ArrayList<>();
        while (rs.next()) {
          String payableTypeKey = rs.getString("payable_type_key");
          BigDecimal amount = rs.getBigDecimal("amount");
          String currency = rs.getString("currency_identifier");
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

  /**
   * Persists a task record transactionally: inserts the task row when absent, otherwise
   * replaces its payables, then refreshes the cache with the stored record.
   * @param record the record to store
   * @return {@code true} if the record was persisted
   */
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

  /**
   * Deletes the task and its payables (children first to satisfy the foreign key)
   * in a transaction, invalidating the cache entry.
   * @param jobKey the job key
   * @param actionTypeKey the action type key
   * @param contextKey the context key
   * @return {@code true} if a task row was deleted, {@code false} if none matched
   */
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

  /**
   * Loads all task records for a job, grouped by action type key (map order follows
   * action type ordering).
   * @param jobKey the job key to match
   * @return a map of action type key to its task records
   */
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
          String currency = rs.getString("currency_identifier");
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

  /** Builds a {@link JobTaskRecord} while accumulating its payable rows across result rows. */
  private static final class TaskRecordAccumulator {

    private final String contextKey;
    private final List<PayableRecord> payables = new ArrayList<>();

    private TaskRecordAccumulator(String contextKey) {
      this.contextKey = contextKey;
    }
  }

  /**
   * Loads all task records for a single action type of a job.
   * @param jobKey the job key
   * @param actionTypeKey the action type key
   * @return the matching task records
   */
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

  /**
   * Loads every task record for a job across all action types.
   * @param jobKey the job key to match
   * @return all task records for the job
   */
  public List<JobTaskRecord> getAllRecords(String jobKey) {
    Map<String, List<JobTaskRecord>> grouped = getRecords(jobKey);
    List<JobTaskRecord> all = new ArrayList<>();
    for (List<JobTaskRecord> records : grouped.values()) {
      all.addAll(records);
    }
    return all;
  }

  /** Builds the cache key for a task's key tuple. */
  private static String createCacheKey(String jobKey, String actionTypeKey, String contextKey) {
    return jobKey + actionTypeKey + contextKey;
  }
}
