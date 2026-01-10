package net.aincraft.repository;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import net.aincraft.serialization.CodecRegistry;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class RelationalTimedBoostRepositoryImpl implements TimedBoostRepository {

  private final ConnectionSource connectionSource;
  private final WriteBackRepositoryImpl<String, ActiveBoostData> repository;
  private final CodecRegistry codecRegistry;
  // Track boost keys per target to handle write-back cache delay
  private final Map<String, Set<String>> knownBoostKeys = new ConcurrentHashMap<>();

  @Inject
  RelationalTimedBoostRepositoryImpl(Plugin plugin, ConnectionSource connectionSource,
      CodecRegistry codecRegistry) {

    RelationalRepositoryContext<String, ActiveBoostData> context = new RelationalRepositoryContext<>() {

      @Override
      public String getSelectQuery() {
        return "SELECT source_id, epoch_millis, duration, boost_source FROM time_boosts WHERE target_id = ?";
      }

      @Override
      public String getSaveQuery() {
        return
            "INSERT INTO time_boosts (target_id, source_id, epoch_millis, duration, boost_source) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(target_id, source_id) DO UPDATE SET epoch_millis = excluded.epoch_millis, " +
                "duration = excluded.duration, boost_source = excluded.boost_source";
      }

      @Override
      public String getDeleteQuery() {
        return "DELETE FROM time_boosts WHERE target_id = ?";
      }

      @Override
      public void setKey(PreparedStatement ps, String key) throws SQLException {
        ps.setString(1, key);
      }

      @Override
      public void setSaveValues(PreparedStatement ps, String key, ActiveBoostData value)
          throws SQLException {
        ps.setString(1, key);
        ps.setString(2, value.sourceIdentifier());
        ps.setLong(3, value.started().getTime()); // Use epoch millis instead of Timestamp

        Duration duration = value.duration();
        if (duration != null) {
          ps.setBytes(4, codecRegistry.encode(duration));
        } else {
          ps.setNull(4, Types.BLOB);
        }

        ps.setBytes(5, codecRegistry.encode(value.boostSource()));
      }

      @Override
      public ActiveBoostData mapResult(ResultSet rs, String targetIdentifier)
          throws SQLException {
        String sourceId = rs.getString("source_id");
        long millis = rs.getLong("epoch_millis");
        Timestamp started = new Timestamp(millis);

        byte[] durationBlob = rs.getBytes("duration");
        Duration duration = null;
        if (durationBlob != null) {
          duration = codecRegistry.decode(durationBlob, Duration.class);
        }

        byte[] boostSourceBytes = rs.getBytes("boost_source");
        BoostSource boostSource = codecRegistry.decode(boostSourceBytes, BoostSource.class);
        return new ActiveBoostData(targetIdentifier, sourceId, started, duration, boostSource);
      }
    };

    this.repository = WriteBackRepositoryImpl.create(plugin,
        new RelationalRepositoryImpl<>(connectionSource, context), 10L);
    this.connectionSource = connectionSource;
    this.codecRegistry = codecRegistry;
  }

  @Override
  public @NotNull List<ActiveBoostData> findAllBoosts(String targetIdentifier) {
    Set<String> sourceIds = new HashSet<>();

    // Get source IDs from database
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            "SELECT source_id FROM time_boosts WHERE target_id = ?")) {

      ps.setString(1, targetIdentifier);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          sourceIds.add(rs.getString("source_id"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    // Also include any locally tracked keys (handles write-back cache delay)
    Set<String> localKeys = knownBoostKeys.get(targetIdentifier);
    if (localKeys != null) {
      sourceIds.addAll(localKeys);
    }

    // Load all boosts from repository (which checks cache first)
    List<ActiveBoostData> boosts = new ArrayList<>();
    for (String sourceId : sourceIds) {
      ActiveBoostData boost = repository.load(targetIdentifier + sourceId);
      if (boost != null) {
        boosts.add(boost);
      }
    }

    return boosts;
  }

  @Override
  public ActiveBoostData findBoost(String targetIdentifier, String sourceIdentifier) {
    return repository.load(targetIdentifier + sourceIdentifier);
  }

  @Override
  public void delete(String targetIdentifier, String sourceIdentifier) {
    repository.delete(targetIdentifier + sourceIdentifier);
    // Remove from local tracking
    Set<String> keys = knownBoostKeys.get(targetIdentifier);
    if (keys != null) {
      keys.remove(sourceIdentifier);
    }
  }

  @Override
  public void addBoost(ActiveBoostData boost) {
    repository.save(boost.targetIdentifier() + boost.sourceIdentifier(), boost);
    // Track locally so findAllBoosts works before cache flush
    knownBoostKeys.computeIfAbsent(boost.targetIdentifier(), k -> ConcurrentHashMap.newKeySet())
        .add(boost.sourceIdentifier());
  }
}
