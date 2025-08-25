package net.aincraft.job;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.sound.sampled.Line;
import net.aincraft.container.ActionType;
import net.aincraft.job.model.ActionTypeRecord;
import net.aincraft.job.model.JobTaskRecord;
import net.aincraft.job.model.PayableRecord;
import net.aincraft.repository.ConnectionSource;
import org.bukkit.Bukkit;

public class RelationalJobTaskRepositoryImpl implements JobTaskRepository {

  private final ConnectionSource connectionSource;

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
  public JobTaskRecord getRecord(String jobKey, String actionTypeKey, String contextKey) {
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
        return new JobTaskRecord(jobKey, actionTypeKey, contextKey, records);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<ActionTypeRecord, List<JobTaskRecord>> getRecords(String jobKey) {
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

    Map<ActionTypeRecord, List<JobTaskRecord>> records = new LinkedHashMap<>();
    for (Entry<String, Map<Integer, TaskRecordAccumulator>> entry : actionTypeTaskMap.entrySet()) {
      String actionTypeKey = entry.getKey();
      List<JobTaskRecord> taskRecords = entry.getValue().values().stream()
          .map(a -> new JobTaskRecord(jobKey, actionTypeKey, a.contextKey, List.copyOf(a.payables)))
          .toList();
      records.put(new ActionTypeRecord(actionTypeKey), taskRecords);
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
          JobTaskRecord record = getRecord(jobKey, actionTypeKey, contextKey);
          records.add(record);
        }
      }
      return records;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
