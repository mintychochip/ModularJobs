package dev.mintychochip.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import dev.mintychochip.boost.BoostDataCodec;
import dev.mintychochip.container.BoostSource;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import dev.mintychochip.container.boost.TimedBoostDataService.ActiveBoostData;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Timed boost persistence. Write-back cache keys are {@code targetId + SEP + sourceId},
 * but SQL columns always store pure {@code target_id} and {@code source_id}.
 */
public final class RelationalTimedBoostRepositoryImpl implements TimedBoostRepository {

  /** Unit separator — must not appear in UUIDs or namespaced keys. */
  static final char KEY_SEP = '\u001f';

  private static final String SELECT_SOURCE_IDS =
      SqlStatements.load("time_boosts/select-source-ids.sql");
  private static final String SELECT_BOOST =
      SqlStatements.load("time_boosts/select.sql");
  private static final String SAVE_BOOST =
      SqlStatements.load("time_boosts/save.sql");
  private static final String DELETE_BOOST =
      SqlStatements.load("time_boosts/delete.sql");

  private final ConnectionSource connectionSource;
  private final RelationalRepositoryImpl<String, ActiveBoostData> relational;
  /** Null when constructed for synchronous (test) access without write-back. */
  @Nullable
  private final WriteBackRepositoryImpl<String, ActiveBoostData> writeBack;
  // Track boost keys per target to handle write-back cache delay
  private final Map<String, Set<String>> knownBoostKeys = new ConcurrentHashMap<>();

  public RelationalTimedBoostRepositoryImpl(Plugin plugin, ConnectionSource connectionSource,
      BoostDataCodec codec) {
    this(connectionSource, codec, plugin);
  }

  /**
   * Synchronous construction (no Bukkit write-back scheduler) for unit tests.
   * Still uses the production SQL context.
   */
  static RelationalTimedBoostRepositoryImpl createSynchronous(
      ConnectionSource connectionSource, BoostDataCodec codec) {
    return new RelationalTimedBoostRepositoryImpl(connectionSource, codec, null);
  }

  private RelationalTimedBoostRepositoryImpl(
      ConnectionSource connectionSource,
      BoostDataCodec codec,
      @Nullable Plugin plugin) {
    this.connectionSource = connectionSource;
    TimedBoostRelationalContext context = new TimedBoostRelationalContext(codec);
    this.relational = new RelationalRepositoryImpl<>(connectionSource, context);
    if (plugin != null) {
      this.writeBack = WriteBackRepositoryImpl.create(plugin, relational, 10L);
    } else {
      this.writeBack = null;
    }
  }

  static String toCacheKey(String targetId, String sourceId) {
    if (targetId.indexOf(KEY_SEP) >= 0 || sourceId.indexOf(KEY_SEP) >= 0) {
      throw new IllegalArgumentException(
          "target/source identifier must not contain unit separator");
    }
    return targetId + KEY_SEP + sourceId;
  }

  static String targetFromCacheKey(String cacheKey) {
    int i = cacheKey.indexOf(KEY_SEP);
    if (i < 0) {
      throw new IllegalArgumentException("invalid timed-boost cache key: " + cacheKey);
    }
    return cacheKey.substring(0, i);
  }

  static String sourceFromCacheKey(String cacheKey) {
    int i = cacheKey.indexOf(KEY_SEP);
    if (i < 0) {
      throw new IllegalArgumentException("invalid timed-boost cache key: " + cacheKey);
    }
    return cacheKey.substring(i + 1);
  }

  @Override
  public @NotNull List<ActiveBoostData> findAllBoosts(String targetIdentifier) {
    Set<String> sourceIds = new HashSet<>();

    // Get source IDs from database by pure target_id
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(SELECT_SOURCE_IDS)) {

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

    List<ActiveBoostData> boosts = new ArrayList<>();
    for (String sourceId : sourceIds) {
      ActiveBoostData boost = load(toCacheKey(targetIdentifier, sourceId));
      if (boost != null) {
        boosts.add(boost);
      }
    }

    return boosts;
  }

  @Override
  public ActiveBoostData findBoost(String targetIdentifier, String sourceIdentifier) {
    return load(toCacheKey(targetIdentifier, sourceIdentifier));
  }

  @Override
  public void delete(String targetIdentifier, String sourceIdentifier) {
    deleteKey(toCacheKey(targetIdentifier, sourceIdentifier));
    Set<String> keys = knownBoostKeys.get(targetIdentifier);
    if (keys != null) {
      keys.remove(sourceIdentifier);
    }
  }

  @Override
  public void addBoost(ActiveBoostData boost) {
    String cacheKey = toCacheKey(boost.targetIdentifier(), boost.sourceIdentifier());
    save(cacheKey, boost);
    knownBoostKeys.computeIfAbsent(boost.targetIdentifier(), k -> ConcurrentHashMap.newKeySet())
        .add(boost.sourceIdentifier());
  }

  private ActiveBoostData load(String cacheKey) {
    if (writeBack != null) {
      return writeBack.load(cacheKey);
    }
    return relational.load(cacheKey);
  }

  private void save(String cacheKey, ActiveBoostData boost) {
    if (writeBack != null) {
      writeBack.save(cacheKey, boost);
    } else {
      relational.save(cacheKey, boost);
    }
  }

  private void deleteKey(String cacheKey) {
    if (writeBack != null) {
      writeBack.delete(cacheKey);
    } else {
      relational.delete(cacheKey);
    }
  }

  /**
   * Flush write-back pending timed boosts before ConnectionSource shutdown.
   * No-op when constructed synchronously without write-back.
   */
  public void flushPending() {
    if (writeBack != null) {
      writeBack.flushPending();
    }
  }

  /**
   * Production SQL binding for timed boosts. Cache key is composite; columns are pure ids.
   */
  static final class TimedBoostRelationalContext
      implements RelationalRepositoryContext<String, ActiveBoostData> {

    private final BoostDataCodec codec;

    TimedBoostRelationalContext(BoostDataCodec codec) {
      this.codec = codec;
    }

    @Override
    public String getSelectQuery() {
      return SELECT_BOOST;
    }

    @Override
    public String getSaveQuery() {
      return SAVE_BOOST;
    }

    @Override
    public String getDeleteQuery() {
      return DELETE_BOOST;
    }

    @Override
    public void setKey(PreparedStatement ps, String cacheKey) throws SQLException {
      ps.setString(1, targetFromCacheKey(cacheKey));
      ps.setString(2, sourceFromCacheKey(cacheKey));
    }

    @Override
    public void setSaveValues(PreparedStatement ps, String cacheKey, ActiveBoostData value)
        throws SQLException {
      // Always persist pure identifiers from the value — never the composite cache key
      ps.setString(1, value.targetIdentifier());
      ps.setString(2, value.sourceIdentifier());
      ps.setLong(3, value.started().toEpochMilli());

      Duration duration = value.duration();
      if (duration != null) {
        ps.setBytes(4, duration.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      } else {
        ps.setNull(4, Types.BLOB);
      }

      Duration encodedDuration = duration == null ? Duration.ZERO : duration;
      ps.setBytes(5, codec.write(new ConsumableBoostData(value.boostSource(), encodedDuration)));
    }

    @Override
    public ActiveBoostData mapResult(ResultSet rs, String cacheKey) throws SQLException {
      String targetId = targetFromCacheKey(cacheKey);
      String sourceId = rs.getString("source_id");
      long millis = rs.getLong("epoch_millis");
      Instant started = Instant.ofEpochMilli(millis);

      byte[] durationBlob = rs.getBytes("duration");
      Duration duration = null;
      if (durationBlob != null && durationBlob.length > 0) {
        duration = Duration.parse(new String(durationBlob, java.nio.charset.StandardCharsets.UTF_8));
      }

      byte[] boostSourceBytes = rs.getBytes("boost_source");
      BoostSource boostSource = codec.readSource(boostSourceBytes);
      return new ActiveBoostData(targetId, sourceId, started, duration, boostSource);
    }
  }
}
