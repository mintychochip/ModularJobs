package net.aincraft.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import org.jetbrains.annotations.Nullable;

public final class RelationalRepositoryImpl<K, V> implements Repository<K, V> {

  private final ConnectionSource connectionSource;

  private final RelationalRepositoryContext<K, V> context;

  private final Cache<K, V> readCache = Caffeine.newBuilder()
      .expireAfterAccess(Duration.ofMinutes(5)).maximumSize(1_000).build();

  public RelationalRepositoryImpl(ConnectionSource connectionSource,
      RelationalRepositoryContext<K, V> context) {
    this.connectionSource = connectionSource;
    this.context = context;
  }

  @Override
  @Nullable
  public V load(K key) {
    return readCache.get(key, ignored -> {
      try (Connection connection = connectionSource.getConnection();
          PreparedStatement ps = connection.prepareStatement(context.getSelectQuery())) {
        context.setKey(ps, key);
        try (ResultSet rs = ps.executeQuery()) {
          return rs.next() ? context.mapResult(rs, key) : null;
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public boolean save(K key, V value) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(context.getSaveQuery())) {
      context.setSaveValues(ps, key, value);
      readCache.put(key, value);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("failed to save entity for key: " + key, e);
    }
  }

  @Override
  public void delete(K key) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(context.getDeleteQuery())) {
      context.setKey(ps, key);
      if (ps.executeUpdate() > 0) {
        readCache.invalidate(key);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete entity for key: " + key, e);
    }
  }
}