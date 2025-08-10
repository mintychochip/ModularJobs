package net.aincraft.service;

import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.database.ConnectionSource;
import org.bukkit.OfflinePlayer;

public class ProgressionServiceImpl implements ProgressionService {

  private final ConnectionSource connectionSource;
  public ProgressionServiceImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public void create(OfflinePlayer player, Job job) {
    Preconditions.checkArgument(!connectionSource.isClosed());
    try (Connection connection = connectionSource.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO job_progression (player_id,job_key) VALUES (?,?)");
      ps.setString(1, player.getUniqueId().toString());
      ps.setString(2, job.key().toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JobProgression get(OfflinePlayer player) {
    return null;
  }

  @Override
  public void update(OfflinePlayer player, JobProgression progression) {

  }

  @Override
  public void delete(OfflinePlayer player) {

  }

  @Override
  public boolean exists(OfflinePlayer player) {
    return false;
  }
}
