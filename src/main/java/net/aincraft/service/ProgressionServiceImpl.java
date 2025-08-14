package net.aincraft.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.Objects;
import net.aincraft.api.Job;
import net.aincraft.api.JobProgression;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.container.JobProgressionImpl;
import net.aincraft.database.ConnectionSource;
import net.aincraft.database.RelationalRepositoryAdapter;
import net.aincraft.database.RelationalRepositoryImpl;
import net.aincraft.database.Repository;
import net.aincraft.util.PlayerJobCompositeKey;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProgressionServiceImpl implements ProgressionService {

  private final Repository<PlayerJobCompositeKey, JobProgression> repository;

  public ProgressionServiceImpl(Repository<PlayerJobCompositeKey, JobProgression> repository) {
    this.repository = repository;
  }

  public static ProgressionService create(ConnectionSource connectionSource) {
    RelationalRepositoryAdapter<PlayerJobCompositeKey, JobProgression> adapter =
        new RelationalRepositoryAdapter<>() {
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
            ps.setString(2, key.jobKey().value());
          }

          @Override
          public void setSaveValues(PreparedStatement ps, PlayerJobCompositeKey key,
              JobProgression value)
              throws SQLException {
            ps.setString(1, key.playerId().toString());
            ps.setString(2, key.jobKey().value());
            ps.setBigDecimal(3, value.getExperience());
          }

          @Override
          public JobProgression mapResult(ResultSet rs, PlayerJobCompositeKey key)
              throws SQLException {
            BigDecimal experience = rs.getBigDecimal("experience");
            RegistryView<Job> registry = RegistryContainer.registryContainer()
                .getRegistry(RegistryKeys.JOBS);
            Job job = registry.getOrThrow(key.jobKey());
            return new JobProgressionImpl(Bukkit.getOfflinePlayer(key.playerId()), job, experience);
          }
        };
    Repository<PlayerJobCompositeKey, JobProgression> repository = new RelationalRepositoryImpl<>(
        connectionSource, adapter,
        Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).maximumSize(1_000).build());
    return new ProgressionServiceImpl(repository);
  }

  @Override
  public @NotNull JobProgression create(OfflinePlayer player, Job job)
      throws IllegalArgumentException {
    PlayerJobCompositeKey key = PlayerJobCompositeKey.create(player, job);
    JobProgression progression = repository.load(key);
    if (progression == null) {
      throw new IllegalArgumentException("progression already exists for player: " + player);
    }
    progression = new JobProgressionImpl(player, job, BigDecimal.ZERO);
    repository.save(key, progression);
    return progression;
  }

  @Override
  public @Nullable JobProgression get(OfflinePlayer player, Job job) {
    PlayerJobCompositeKey key = PlayerJobCompositeKey.create(player, job);
    return repository.load(key);
  }

  @Override
  public @NotNull List<JobProgression> getAll(OfflinePlayer player) {
    RegistryView<Job> registry = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.JOBS);
    return registry.stream()
        .map(job -> PlayerJobCompositeKey.create(player, job)).map(repository::load).filter(
            Objects::nonNull).toList();
  }

  @Override
  public void update(JobProgression progression) {
    PlayerJobCompositeKey key = PlayerJobCompositeKey.create(progression.getPlayer(),
        progression.getJob());
    JobProgression jobProgression = repository.load(key);
    if (jobProgression != null) {
      repository.save(key,progression);
    }
  }

  @Override
  public void update(List<? extends JobProgression> progressions) {
    Map<PlayerJobCompositeKey,JobProgression> entities = new HashMap<>();
    for (JobProgression progression : progressions) {
      PlayerJobCompositeKey key = PlayerJobCompositeKey.create(progression.getPlayer(),
          progression.getJob());
      entities.put(key,progression);
    }
    repository.saveAll(entities);
  }
}
