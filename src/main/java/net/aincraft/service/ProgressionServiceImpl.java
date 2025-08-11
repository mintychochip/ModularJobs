package net.aincraft.service;

import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.api.JobProgressionView;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.container.JobProgressionImpl;
import net.aincraft.database.ConnectionSource;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;

public class ProgressionServiceImpl implements ProgressionService {

  private final ConnectionSource connectionSource;

  public ProgressionServiceImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public JobProgression create(OfflinePlayer player, Job job) {
    Preconditions.checkArgument(!connectionSource.isClosed());
    try (Connection connection = connectionSource.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO job_progression (player_id,job_key) VALUES (?,?)");
      ps.setString(1, player.getUniqueId().toString());
      ps.setString(2, job.key().value());
      ps.executeUpdate();
      return new JobProgressionImpl(job, player, 0);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JobProgression get(OfflinePlayer player, Job job) {
    Preconditions.checkArgument(!connectionSource.isClosed());
    try (Connection connection = connectionSource.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "SELECT id,player_id,experience FROM job_progression WHERE player_id=? AND job_key=?");
      ps.setString(1, player.getUniqueId().toString());
      ps.setString(2, job.key().value());
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        double experience = rs.getDouble("experience");
        return new JobProgressionImpl(job, player, experience);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<JobProgression> getAll(OfflinePlayer player) {
    Preconditions.checkArgument(!connectionSource.isClosed());
    RegistryView<Job> registryView = RegistryContainer.registryContainer().getRegistry(
        RegistryKeys.JOBS);
    List<JobProgression> progressions = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "SELECT job_key, experience FROM job_progression WHERE player_id=?");
      ps.setString(1, player.getUniqueId().toString());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Key key = new NamespacedKey("jobs", rs.getString("job_key"));
          double experience = rs.getDouble("experience");
          if (!registryView.isRegistered(key)) {
            continue;
          }
          Job job = registryView.getOrThrow(key);
          progressions.add(new JobProgressionImpl(job, player, experience));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return progressions;
  }

  @Override
  public void update(JobProgressionView progression) {
    Preconditions.checkArgument(!connectionSource.isClosed());
    try (Connection connection = connectionSource.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "UPDATE job_progression SET experience=? WHERE player_id=? AND job_key=?");
      ps.setDouble(1, progression.getExperience());
      ps.setString(2, progression.getPlayer().getUniqueId().toString());
      ps.setString(3, progression.getJob().key().value());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void delete(OfflinePlayer player) {
    Preconditions.checkArgument(!connectionSource.isClosed());
    try (Connection connection = connectionSource.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "DELETE FROM job_progression WHERE player_id=?");
      ps.setString(1, player.getUniqueId().toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean exists(OfflinePlayer player) {
    return false;
  }
}
