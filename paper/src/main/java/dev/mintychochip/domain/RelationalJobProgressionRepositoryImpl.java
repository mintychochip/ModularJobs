package dev.mintychochip.domain;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import dev.mintychochip.domain.model.JobProgressionRecord;
import dev.mintychochip.domain.model.JobRecord;
import dev.mintychochip.domain.repository.JobProgressionRepository;
import dev.mintychochip.repository.ConnectionSource;
import dev.mintychochip.repository.SqlStatements;
import org.jetbrains.annotations.Nullable;

/**
 * MySQL-backed {@link JobProgressionRepository} for active progression records.
 *
 * <p>Owns no connection pool itself; it draws connections on demand from the
 * provided shared {@link ConnectionSource} (composition-owned). Each operation
 * checks out one connection and closes it via try-with-resources.
 *
 * <p>Reads are cached in a Caffeine cache (10-minute write expiry, 10k entries)
 * to reduce DB load; {@code loadAll*}-style bulk reads prefer cached entries and
 * only fall back to fresh rows for uncached keys. {@link #save} and {@link #delete}
 * refresh or invalidate the cache accordingly, keeping single-key reads consistent
 * with writes through this instance.
 *
 * <p>Failure semantics: unchecked {@link RuntimeException} wrapping the underlying
 * {@link SQLException} is thrown on any connection or SQL failure; callers must
 * treat a throw as "operation not performed". Nullability: {@link #load(String, String)}
 * returns {@code null} when the player/job pair has no persisted row, when the job
 * is unknown to the in-memory job repository, or when the row is absent.
 *
 * <p>Table naming is parameterized; writes use MySQL {@code ON DUPLICATE KEY UPDATE}
 * semantics, so {@link #save} is an upsert keyed by (player_id, job_key).
 */
final class RelationalJobProgressionRepositoryImpl implements JobProgressionRepository {

  private static final Duration CACHE_TIME_TO_LIVE = Duration.ofMinutes(10);
  private static final int CACHE_MAXIMUM_SIZE = 10_000;

  private final MemoryJobRepositoryImpl jobRepository;
  private final ConnectionSource connectionSource;
  private final String tableName;
  private final String saveQuery;
  private final String loadQuery;
  private final String loadAllByJobQuery;
  private final String loadAllForPlayerQuery;
  private final String deleteQuery;
  private final Cache<JobProgressionRepository.Key, JobProgressionRecord> readCache = Caffeine.newBuilder()
      .expireAfterWrite(CACHE_TIME_TO_LIVE).maximumSize(CACHE_MAXIMUM_SIZE)
      .build();

  private RelationalJobProgressionRepositoryImpl(MemoryJobRepositoryImpl jobRepository,
      ConnectionSource connectionSource, String tableName) {
    this.jobRepository = jobRepository;
    this.connectionSource = connectionSource;
    this.tableName = tableName;
    this.saveQuery = bindTable(SqlStatements.load("job_progression/save.sql"));
    this.loadQuery = bindTable(SqlStatements.load("job_progression/load.sql"));
    this.loadAllByJobQuery = bindTable(SqlStatements.load("job_progression/load-all-by-job.sql"));
    this.loadAllForPlayerQuery = bindTable(SqlStatements.load("job_progression/load-all-for-player.sql"));
    this.deleteQuery = bindTable(SqlStatements.load("job_progression/delete.sql"));
  }

  private String bindTable(String sql) {
    return sql.replace("{table}", tableName);
  }

  private String withLimit(String sql, int limit) {
    return sql.replace("{limit}", Integer.toString(limit));
  }

  static JobProgressionRepository create(MemoryJobRepositoryImpl jobRepository,
      ConnectionSource connectionSource, String tableName) {
    return new RelationalJobProgressionRepositoryImpl(jobRepository, connectionSource, tableName);
  }

  @Override
  public boolean save(JobProgressionRecord record) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(saveQuery)) {
      String jobKey = record.jobRecord().jobKey();
      ps.setString(1, record.playerId());
      ps.setString(2, jobKey);
      ps.setBigDecimal(3, record.experience());
      if (ps.executeUpdate() > 0) {
        readCache.put(new Key(record.playerId(), jobKey), record);
        return true;
      }
      return false;
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException("Relational repository operation failed", e);
    }
  }

  @Override
  public @Nullable JobProgressionRecord load(String playerId, String jobKey) {
    Key key = new Key(playerId, jobKey);
    JobProgressionRecord progressionRecord = readCache.getIfPresent(key);
    if (progressionRecord != null) {
      return progressionRecord;
    }
    JobRecord jobRecord = jobRepository.load(jobKey);
    if (jobRecord == null) {
      return null;
    }
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(loadQuery)) {
      ps.setString(1, playerId);
      ps.setString(2, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        BigDecimal experience = rs.getBigDecimal("experience");
        progressionRecord = new JobProgressionRecord(playerId, jobRecord, experience);
        readCache.put(key, progressionRecord);
        return progressionRecord;
      }
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException("Relational repository operation failed", e);
    }
  }

  @Override
  public List<JobProgressionRecord> loadAllForJob(String jobKey, int limit) {
    JobRecord jobRecord = jobRepository.load(jobKey);
    if (jobRecord == null) {
      return List.of();
    }
    List<JobProgressionRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            withLimit(loadAllByJobQuery, limit))) {
      ps.setString(1, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String playerId = rs.getString("player_id");
          Key key = new Key(playerId, jobKey);
          JobProgressionRecord progressionRecord = readCache.getIfPresent(key);
          if (progressionRecord != null) {
            records.add(progressionRecord);
            continue;
          }
          BigDecimal experience = rs.getBigDecimal("experience");
          progressionRecord = new JobProgressionRecord(playerId, jobRecord, experience);
          readCache.put(key, progressionRecord);
          records.add(progressionRecord);
        }
      }
      return records;
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException("Relational repository operation failed", e);
    }
  }

  @Override
  public List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit) {
    List<JobProgressionRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            withLimit(loadAllForPlayerQuery, limit))) {
      ps.setString(1, playerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String jobKey = rs.getString("job_key");
          Key key = new Key(playerId, jobKey);
          JobProgressionRecord progressionRecord = readCache.getIfPresent(key);
          if (progressionRecord != null) {
            records.add(progressionRecord);
            continue;
          }
          JobRecord jobRecord = jobRepository.load(jobKey);
          if (jobRecord == null) {
            continue;
          }
          BigDecimal experience = rs.getBigDecimal("experience");
          progressionRecord = new JobProgressionRecord(playerId, jobRecord, experience);
          readCache.put(key, progressionRecord);
          records.add(progressionRecord);
        }
      }
      return records;
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException("Relational repository operation failed", e);
    }
  }

  @Override
  public boolean delete(String playerId, String jobKey) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(deleteQuery)) {
      ps.setString(1, playerId);
      ps.setString(2, jobKey);
      if (ps.executeUpdate() > 0) {
        readCache.invalidate(new Key(playerId, jobKey));
        return true;
      }
      return false;
    } catch (SQLException e) {
      throw new dev.mintychochip.repository.WriteBackException("Relational repository operation failed", e);
    }
  }
}
