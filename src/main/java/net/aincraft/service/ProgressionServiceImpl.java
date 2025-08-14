package net.aincraft.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
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
import net.aincraft.util.PlayerJobCompositeKey;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProgressionServiceImpl implements ProgressionService {

  private final ConnectionSource connectionSource;

  private final Cache<PlayerJobCompositeKey, JobProgression> progressionCache = Caffeine.newBuilder()
      .maximumSize(1_000)
      .expireAfterWrite(
          Duration.ofMinutes(1)).build();

  public ProgressionServiceImpl(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  @Override
  public @NotNull JobProgression create(OfflinePlayer player, Job job) {
    try (Connection connection = connectionSource.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO job_progression (player_id,job_key) VALUES (?,?)");
      ps.setString(1, player.getUniqueId().toString());
      ps.setString(2, job.key().value());
      ps.executeUpdate();
      JobProgressionImpl progression = new JobProgressionImpl(job, player, BigDecimal.ZERO);
      progressionCache.put(PlayerJobCompositeKey.create(player,job),progression);
      return progression;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @Nullable JobProgression get(OfflinePlayer player, Job job) {
    return progressionCache.get(PlayerJobCompositeKey.create(player,job),ignored -> {
      try (Connection connection = connectionSource.getConnection()) {
        PreparedStatement ps = connection.prepareStatement(
            "SELECT experience FROM job_progression WHERE player_id=? AND job_key=?");
        ps.setString(1, player.getUniqueId().toString());
        ps.setString(2, job.key().value());
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) {
            return null;
          }
          BigDecimal experience = rs.getBigDecimal("experience");
          return new JobProgressionImpl(job, player, experience);
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public @NotNull List<JobProgression> getAll(OfflinePlayer player) {
    RegistryContainer container = RegistryContainer.registryContainer();
    RegistryView<Job> registry = container.getRegistry(RegistryKeys.JOBS);
    List<JobProgression> progressions = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "SELECT job_key, experience FROM job_progression WHERE player_id=?");
      ps.setString(1, player.getUniqueId().toString());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Key key = new NamespacedKey("jobs", rs.getString("job_key"));
          BigDecimal experience = rs.getBigDecimal("experience");
          if (!registry.isRegistered(key)) {
            continue;
          }
          Job job = registry.getOrThrow(key);
          JobProgressionImpl progression = new JobProgressionImpl(job, player, experience);
          progressions.add(progression);
          progressionCache.put(PlayerJobCompositeKey.create(player,job),progression);
        }
      }
      return progressions;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void update(JobProgressionView progression) {
    OfflinePlayer player = progression.getPlayer();
    Job job = progression.getJob();
    Connection connection = null;
    try {
      connection = connectionSource.getConnection();
      connection.setAutoCommit(false);
      try (PreparedStatement ps = connection.prepareStatement(
          "UPDATE job_progression SET experience=? WHERE player_id=? AND job_key=?")) {
        ps.setBigDecimal(1, progression.getExperience());
        ps.setString(2, player.getUniqueId().toString());
        ps.setString(3, job.key().value());
        int count = ps.executeUpdate();
        if (count < 0) {
          throw new SQLException("update failed (count=" + count + ")");
        }
      }
      connection.commit();
      progressionCache.invalidate(PlayerJobCompositeKey.create(player,job));
    } catch (SQLException e) {
      if (connection != null) {
        try {
          connection.rollback();
        } catch (SQLException ignore) {
        }
      }
      throw new RuntimeException(e);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException ignore) {
        }
      }
    }
  }

  @Override
  public void update(List<? extends JobProgressionView> progressions) {
    Connection connection = null;
    try {
      List<PlayerJobCompositeKey> invalidationList = new ArrayList<>();
      connection = connectionSource.getConnection();
      connection.setAutoCommit(false);
      try (PreparedStatement ps = connection.prepareStatement(
          "UPDATE job_progression SET experience=? WHERE player_id=? AND job_key=?")) {

        for (JobProgressionView progression : progressions) {
          OfflinePlayer player = progression.getPlayer();
          Job job = progression.getJob();
          ps.setBigDecimal(1, progression.getExperience());
          ps.setString(2, player.getUniqueId().toString());
          ps.setString(3, job.key().value());
          ps.addBatch();
          invalidationList.add(PlayerJobCompositeKey.create(player,job));
        }

        int[] counts;
        try {
          counts = ps.executeBatch();
        } catch (BatchUpdateException e) {
          try {
            connection.rollback();
          } catch (SQLException ignore) {
          }
          throw new RuntimeException("batch failed at index " + e.getUpdateCounts().length, e);
        }

        if (counts.length != progressions.size()) {
          try {
            connection.rollback();
          } catch (SQLException ignore) {
          }
          throw new SQLException(
              "execute batch failed: " + counts.length + " results for " + progressions.size()
                  + " statements");
        }

        for (int i = 0; i < counts.length; i++) {
          int count = counts[i];
          if (!(count >= 0 || count == PreparedStatement.SUCCESS_NO_INFO)) {
            try {
              connection.rollback();
            } catch (SQLException ignore) {
            }
            throw new SQLException("batch item " + i + " failed");
          }
        }
      }
      connection.commit();
      progressionCache.invalidateAll(invalidationList);
    } catch (SQLException e) {
      if (connection != null) {
        try {
          connection.rollback();
        } catch (SQLException ignore) {
        }
      }
      throw new RuntimeException(e);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException ignore) {
        }
      }
    }
  }

  @Override
  public void delete(OfflinePlayer player) {
    try (Connection connection = connectionSource.getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "DELETE FROM job_progression WHERE player_id=?");
      ps.setString(1, player.getUniqueId().toString());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
