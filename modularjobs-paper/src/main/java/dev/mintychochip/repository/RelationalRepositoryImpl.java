package dev.mintychochip.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import org.jetbrains.annotations.Nullable;

/**
 * Temporary relational repository over a {@link ConnectionSource} driven by a {@link
 * RelationalRepositoryContext}.
 *
 * <p>Loads are cached in a short-lived Caffeine cache (entries expire after 5 minutes of access,
 * bounded to 1_000 entries): {@link #load} reads through the cache, {@link #save} writes through to
 * both cache and database, and {@link #delete} invalidates the cache only when the row is removed.
 */
public final class RelationalRepositoryImpl<K, V> {

  private final ConnectionSource connectionSource;

  private final RelationalRepositoryContext<K, V> context;

  private final Cache<K, V> readCache =
      Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(5)).maximumSize(1_000).build();

  /** Relational repository impl. */
  public RelationalRepositoryImpl(
      ConnectionSource connectionSource, RelationalRepositoryContext<K, V> context) {
    this.connectionSource = connectionSource;
    this.context = context;
  }

  /**
   * Loads the value for {@code key}, returning {@code null} if no row exists. Results are cached
   * and served from cache on subsequent calls.
   */
  @Nullable
  public V load(K key) {
    return readCache.get(
        key,
        ignored -> {
          try (Connection connection = connectionSource.getConnection();
              PreparedStatement ps = connection.prepareStatement(context.getSelectQuery())) {
            context.setKey(ps, key);
            try (ResultSet rs = ps.executeQuery()) {
              return rs.next() ? context.mapResult(rs, key) : null;
            }
          } catch (SQLException e) {
            throw new WriteBackException("Relational repository operation failed", e);
          }
        });
  }

  /**
   * Persists {@code value} for {@code key} and updates the cache.
   *
   * @return whether a row was updated (row count greater than zero)
   */
  public boolean save(K key, V value) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(context.getSaveQuery())) {
      context.setSaveValues(ps, key, value);
      readCache.put(key, value);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new WriteBackException("Failed to save entity for key: " + key, e);
    }
  }

  /** Removes the row for {@code key} and invalidates it from the cache if the row existed. */
  public void delete(K key) {
    try (Connection connection = connectionSource.getConnection();
        PreparedStatement ps = connection.prepareStatement(context.getDeleteQuery())) {
      context.setKey(ps, key);
      if (ps.executeUpdate() > 0) {
        readCache.invalidate(key);
      }
    } catch (SQLException e) {
      throw new WriteBackException("Failed to delete entity for key: " + key, e);
    }
  }
}
