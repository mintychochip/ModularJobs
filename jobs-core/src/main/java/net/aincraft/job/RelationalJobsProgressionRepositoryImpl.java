package net.aincraft.job;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.aincraft.job.model.JobProgressionRecord;
import net.aincraft.repository.ConnectionSource;

final class RelationalJobsProgressionRepositoryImpl implements JobsProgressionRepository {

  private final ConnectionSource connectionSource;

  RelationalJobsProgressionRepositoryImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public void save(JobProgressionRecord record) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            """
                  INSERT INTO job_progression (player_id, job_key, experience)
                  VALUES (?, ?, ?)
                  ON CONFLICT(player_id, job_key) DO UPDATE SET experience = excluded.experience
                """)) {
      ps.setString(1, record.playerId());
      ps.setString(2, record.jobKey());
      ps.setBigDecimal(3, record.experience());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<JobProgressionRecord> getRecord(String playerId, String jobKey) {
    return Optional.empty();
  }

  @Override
  public List<JobProgressionRecord> getRecords(String jobKey) {
    List<JobProgressionRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT player_id, experience FROM job_progression WHERE job_key=? ORDER BY (experience IS NULL), CAST(experience AS REAL) DESC")) {
      ps.setString(1, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String playerId = rs.getString("player_id");
          BigDecimal experience = rs.getBigDecimal("experience");
          records.add(new JobProgressionRecord(playerId,jobKey,experience));
        }
      }
      return records;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
