package net.aincraft.domain;

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
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.repository.JobRepository;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.repository.ConnectionSource;
import org.jetbrains.annotations.Nullable;

final class RelationalJobProgressionRepositoryImpl implements JobProgressionRepository {

  private static final Duration CACHE_TIME_TO_LIVE = Duration.ofMinutes(10);
  private static final int CACHE_MAXIMUM_SIZE = 10_000;

  private final JobRepository jobRepository;
  private final ConnectionSource connectionSource;
  private final String tableName;
  private final Cache<String, JobProgressionRecord> readCache = Caffeine.newBuilder()
      .expireAfterWrite(CACHE_TIME_TO_LIVE).maximumSize(CACHE_MAXIMUM_SIZE).build();

  private static final String SAVE_QUERY = """
      INSERT INTO %s (player_id, job_key, experience)
      VALUES (?,?,?)
      ON CONFLICT (player_id, job_key)
      DO UPDATE SET experience = excluded.experience;
      """;

  private static final String LOAD_QUERY = """
      SELECT experience FROM %s WHERE player_id = ?
      AND job_key = ? LIMIT 1;
      """;

  private static final String LOAD_ALL_FOR_JOB = """
      SELECT player_id,experience FROM %s WHERE job_key = ?
      ORDER BY (experience IS NULL) CAST (experience AS REAL)
      DESC LIMIT %d;
      """;

  private static final String LOAD_ALL_FOR_PLAYER = """
      SELECT job_key,experience FROM %s WHERE player_id = ?
      LIMIT %d;
      """;

  private static final String DELETE_QUERY = """
      DELETE FROM %s WHERE player_id = ? AND job_key = ?
      """;

  private RelationalJobProgressionRepositoryImpl(JobRepository jobRepository,
      ConnectionSource connectionSource, String tableName) {
    this.jobRepository = jobRepository;
    this.connectionSource = connectionSource;
    this.tableName = tableName;
  }

  static JobProgressionRepository create(JobRepository jobRepository,
      ConnectionSource connectionSource, String tableName) {
    return new RelationalJobProgressionRepositoryImpl(jobRepository, connectionSource, tableName);
  }

  @Override
  public boolean save(JobProgressionRecord record) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            String.format(SAVE_QUERY, tableName))) {
      String jobKey = record.jobRecord().jobKey();
      ps.setString(1, record.playerId());
      ps.setString(2, jobKey);
      ps.setBigDecimal(3, record.experience());
      if (ps.executeUpdate() > 0) {
        readCache.put(createCacheKey(record.playerId(), jobKey), record);
        return true;
      }
      return false;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @Nullable JobProgressionRecord load(String playerId, String jobKey) {
    String cacheKey = createCacheKey(playerId, jobKey);
    JobProgressionRecord progressionRecord = readCache.getIfPresent(cacheKey);
    if (progressionRecord != null) {
      return progressionRecord;
    }
    JobRecord jobRecord = jobRepository.load(jobKey);
    if (jobRecord == null) {
      return null;
    }
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            String.format(LOAD_QUERY, tableName))) {
      ps.setString(1, playerId);
      ps.setString(2, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        BigDecimal experience = rs.getBigDecimal("experience");
        progressionRecord = new JobProgressionRecord(playerId, jobRecord, experience);
        readCache.put(cacheKey, progressionRecord);
        return progressionRecord;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
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
            String.format(LOAD_ALL_FOR_JOB, tableName, limit))) {
      ps.setString(1, jobKey);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String playerId = rs.getString("player_id");
          String cacheKey = createCacheKey(playerId, jobKey);
          JobProgressionRecord progressionRecord = readCache.getIfPresent(cacheKey);
          if (progressionRecord != null) {
            records.add(progressionRecord);
            continue;
          }
          BigDecimal experience = rs.getBigDecimal("experience");
          progressionRecord = new JobProgressionRecord(playerId, jobRecord, experience);
          readCache.put(cacheKey, progressionRecord);
          records.add(progressionRecord);
        }
      }
      return records;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<JobProgressionRecord> loadAllForPlayer(String playerId, int limit) {
    List<JobProgressionRecord> records = new ArrayList<>();
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            String.format(LOAD_ALL_FOR_PLAYER, tableName, limit))) {
      ps.setString(1, playerId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String jobKey = rs.getString("job_key");
          String cacheKey = createCacheKey(playerId, jobKey);
          JobProgressionRecord progressionRecord = readCache.getIfPresent(cacheKey);
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
          readCache.put(cacheKey, progressionRecord);
          records.add(progressionRecord);
        }
      }
      return records;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean delete(String playerId, String jobKey) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            String.format(DELETE_QUERY, tableName))) {
      ps.setString(1, playerId);
      ps.setString(2, jobKey);
      if (ps.executeUpdate() > 0) {
        readCache.invalidate(createCacheKey(playerId, jobKey));
        return true;
      }
      return false;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static String createCacheKey(String playerId, String jobKey) {
    return playerId + jobKey;
  }
}
