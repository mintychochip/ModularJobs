package net.aincraft.repository;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.job.JobProgressionImpl;
import net.aincraft.service.JobService;
import net.aincraft.util.PlayerJobCompositeKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class RelationalProgressionRepositoryImpl implements
    ProgressionRepository {

  private final ConnectionSource connectionSource;
  private final Repository<PlayerJobCompositeKey, JobProgression> repository;
  private final JobService jobService;

  @Inject
  public RelationalProgressionRepositoryImpl(Plugin plugin, ConnectionSource connectionSource,
      JobService jobService) {
    this.jobService = jobService;
    this.repository = WriteBackRepositoryImpl.create(plugin,
        new RelationalRepositoryImpl<>(connectionSource, CONTEXT), 10L);
    this.connectionSource = connectionSource;
  }

  private final RelationalRepositoryContext<PlayerJobCompositeKey, JobProgression> CONTEXT =
      new RelationalRepositoryContext<>() {
        @Override
        public String getSelectQuery() {
          return "SELECT experience FROM job_progression WHERE player_id = ? AND job_key = ?";
        }

        @Override
        public String getSaveQuery() {
          return "INSERT INTO job_progression (player_id, job_key, experience) " +
              "VALUES (?, ?, ?) " +
              "ON CONFLICT(player_id, job_key) DO UPDATE SET experience = excluded.experience";
        }


        @Override
        public String getDeleteQuery() {
          return "DELETE FROM job_progression WHERE player_id = ? AND job_key = ?";
        }

        @Override
        public void setKey(PreparedStatement ps, PlayerJobCompositeKey key) throws SQLException {
          ps.setString(1, key.playerId().toString());
          ps.setString(2, key.jobKey().toString());
        }

        @Override
        public void setSaveValues(PreparedStatement ps, PlayerJobCompositeKey key,
            JobProgression value)
            throws SQLException {
          ps.setString(1, key.playerId().toString());
          ps.setString(2, key.jobKey().toString());
          ps.setBigDecimal(3, value.getExperience());
        }

        @Override
        public JobProgression mapResult(ResultSet rs, PlayerJobCompositeKey key)
            throws SQLException {
          BigDecimal experience = rs.getBigDecimal("experience");
          Bukkit.broadcastMessage(key.jobKey().toString());
          Optional<Job> job = jobService.getJob(key.jobKey());
          //TODO: idk what exception to throw here
          return new JobProgressionImpl(Bukkit.getOfflinePlayer(key.playerId()), job.get(), experience);
        }
      };

  @Override
  public JobProgression create(UUID uuid, Key jobKey) throws IllegalArgumentException {
    Optional<Job> job = jobService.getJob(jobKey);
    Preconditions.checkArgument(job.isPresent());
    PlayerJobCompositeKey key = new PlayerJobCompositeKey(uuid, jobKey);
    JobProgression progression = repository.load(key);
    if (progression != null) {
      throw new IllegalArgumentException(
          "progression already exists for player: " + uuid.toString());
    }
    progression = new JobProgressionImpl(Bukkit.getOfflinePlayer(uuid), job.get(), BigDecimal.ZERO);
    repository.save(key, progression);
    return progression;
  }

  @Override
  public void update(JobProgression progression) {
    PlayerJobCompositeKey key = PlayerJobCompositeKey.create(progression.getPlayer(),
        progression.getJob());
    JobProgression jobProgression = repository.load(key);
    if (jobProgression != null) {
      repository.save(key, progression);
    }
  }

  @Override
  public JobProgression get(UUID uuid, Key jobKey) {
    return repository.load(new PlayerJobCompositeKey(uuid, jobKey));
  }

  @Override
  public List<JobProgression> getAllProgressions(UUID playerId) {
    List<JobProgression> progressions = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT job_key FROM job_progression WHERE player_id = ?")) {
      ps.setString(1, playerId.toString());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String rawJobKey = rs.getString("job_key");
          Key jobKey = NamespacedKey.fromString(rawJobKey);
          JobProgression progression = repository.load(
              new PlayerJobCompositeKey(playerId, jobKey));
          if (progression == null) {
            continue;
          }
          progressions.add(progression);
        }
        return progressions;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
