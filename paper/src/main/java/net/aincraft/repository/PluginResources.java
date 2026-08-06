package net.aincraft.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Composition-owned lifecycle for DB sources and write-back flush hooks.
 * Shared by successful plugin disable and failed-composition cleanup.
 */
public final class PluginResources {

  private final List<ConnectionSource> connectionSources = new ArrayList<>();
  private final List<Runnable> flushHooks = new ArrayList<>();
  private boolean shutDown;

  /**
   * Track a source opened during composition. Returns the same instance for chaining.
   */
  public <T extends ConnectionSource> T track(@NotNull T source) {
    Objects.requireNonNull(source, "source");
    connectionSources.add(source);
    return source;
  }

  /**
   * Register a write-back / write-behind flush that runs before any source is closed.
   */
  public void onFlush(@NotNull Runnable flushHook) {
    Objects.requireNonNull(flushHook, "flushHook");
    flushHooks.add(flushHook);
  }

  /**
   * Ordered view of tracked sources (composition order). Exposed for tests/assertions.
   */
  public List<ConnectionSource> connectionSources() {
    return List.copyOf(connectionSources);
  }

  public int sourceCount() {
    return connectionSources.size();
  }

  /**
   * Production disable path: flush all write-backs, then shut down every tracked source.
   * Idempotent.
   */
  public void shutdown() throws SQLException {
    if (shutDown) {
      return;
    }
    shutDown = true;
    RuntimeException flushFailure = null;
    for (Runnable hook : flushHooks) {
      try {
        hook.run();
      } catch (RuntimeException e) {
        if (flushFailure == null) {
          flushFailure = e;
        } else {
          flushFailure.addSuppressed(e);
        }
      }
    }
    SQLException closeFailure = null;
    for (ConnectionSource source : connectionSources) {
      try {
        if (!source.isClosed()) {
          source.shutdown();
        }
      } catch (SQLException e) {
        if (closeFailure == null) {
          closeFailure = e;
        } else {
          closeFailure.addSuppressed(e);
        }
      } catch (RuntimeException e) {
        SQLException wrapped = new SQLException("ConnectionSource.shutdown failed", e);
        if (closeFailure == null) {
          closeFailure = wrapped;
        } else {
          closeFailure.addSuppressed(e);
        }
      }
    }
    if (closeFailure != null) {
      if (flushFailure != null) {
        closeFailure.addSuppressed(flushFailure);
      }
      throw closeFailure;
    }
    if (flushFailure != null) {
      throw flushFailure;
    }
  }

  /**
   * Best-effort close used when composition fails mid-create. Does not rethrow;
   * swallows individual failures so the original composition error is preserved.
   */
  public void closeQuietly() {
    try {
      shutdown();
    } catch (Exception ignored) {
      // composition failure path: original exception is primary
    }
  }
}
