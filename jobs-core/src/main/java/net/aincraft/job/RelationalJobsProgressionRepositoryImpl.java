package net.aincraft.job;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
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
  public List<JobProgressionRecord> getProgressionRecords() {
    return List.of();
  }
}
