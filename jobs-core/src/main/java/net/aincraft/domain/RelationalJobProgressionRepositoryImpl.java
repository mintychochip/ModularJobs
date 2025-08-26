package net.aincraft.domain;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.repository.JobRepository;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.repository.ConnectionSource;
import org.jetbrains.annotations.Nullable;

final class RelationalJobProgressionRepositoryImpl implements JobProgressionRepository {

  private final JobRepository jobRepository;
  private final ConnectionSource connectionSource;
  private final String tableName;

  private static final String SAVE_QUERY = """
      INSERT INTO %s (player_id, job_key, experience)
      VALUES (?,?,?)
      ON CONFLICT (player_id, job_key, experience)
      DO UPDATE SET experience = excluded.experience;
      """;

  private static final String LOAD_QUERY = """
      SELECT experience FROM %s WHERE player_id = ?
      AND job_key = ? LIMIT 1;
      """;

  private static final String LOAD_ALL_QUERY = """
      SELECT player_id,experience FROM %s WHERE job_key = ?
      ORDER BY (experience IS NULL) CAST (experience AS REAL)
      DESC LIMIT %d;
      """;

  private static final String DELETE_QUERY = """
      DELETE FROM %s WHERE player_id = ? AND job_key = ?
      """;

  RelationalJobProgressionRepositoryImpl(JobRepository jobRepository,
      ConnectionSource connectionSource, String tableName) {
    this.jobRepository = jobRepository;
    this.connectionSource = connectionSource;
    this.tableName = tableName;
  }

  @Override
  public void save(JobProgressionRecord record) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            String.format(SAVE_QUERY, tableName))) {
      ps.setString(1, record.playerId());
      ps.setString(2, record.jobRecord().jobKey());
      ps.setBigDecimal(3, record.experience());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @Nullable JobProgressionRecord load(String playerId, String jobKey)
      throws IllegalArgumentException {
    JobRecord jobRecord = jobRepository.getJob(jobKey);
    Preconditions.checkArgument(jobRecord != null,
        "failed to find job record for job key: " + jobKey);
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            String.format(LOAD_QUERY, tableName))) {
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        BigDecimal experience = rs.getBigDecimal("experience");
        return new JobProgressionRecord(playerId, jobRecord, experience);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<JobProgressionRecord> loadAll(String jobKey, int limit)
      throws IllegalArgumentException {
    JobRecord jobRecord = jobRepository.getJob(jobKey);
    if (jobRecord == null) {
      throw new IllegalArgumentException("the job key does not map to a valid job");
    }
    List<JobProgressionRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            String.format(LOAD_ALL_QUERY, tableName, limit))) {
      ps.setString(1, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String playerId = rs.getString("player_id");
          BigDecimal experience = rs.getBigDecimal("experience");
          records.add(new JobProgressionRecord(playerId, jobRecord, experience));
        }
      }
      return records;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean delete(String playerId, String jobKey) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(DELETE_QUERY.formatted(tableName))) {
      ps.setString(1, playerId);
      ps.setString(2, jobKey);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
