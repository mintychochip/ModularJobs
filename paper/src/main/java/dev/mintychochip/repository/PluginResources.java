package dev.mintychochip.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/** Composition-owned lifecycle for database sources and write-back flush hooks. */
public final class PluginResources {

  private static final Logger LOGGER = Logger.getLogger(PluginResources.class.getName());

  private final List<ConnectionSource> connectionSources = new ArrayList<>();
  private final List<Runnable> flushHooks = new ArrayList<>();
  private boolean shutDown;

  /** Tracks a source opened during composition and returns it for chaining. */
  public <T extends ConnectionSource> T track(@NotNull T source) {
    Objects.requireNonNull(source, "source");
    connectionSources.add(source);
    return source;
  }

  /** Registers a write-back flush that runs before any source is closed. */
  public void onFlush(@NotNull Runnable flushHook) {
    Objects.requireNonNull(flushHook, "flushHook");
    flushHooks.add(flushHook);
  }

  /** Returns tracked sources in composition order. */
  public List<ConnectionSource> connectionSources() {
    return List.copyOf(connectionSources);
  }

  /** Source count. */
  public int sourceCount() {
    return connectionSources.size();
  }

  /** Flushes write-backs and then shuts down every tracked source. Idempotent. */
  public void shutdown() throws SQLException {
    if (shutDown) {
      return;
    }
    shutDown = true;

    WriteBackException flushFailure = null;
    for (Runnable hook : flushHooks) {
      try {
        hook.run();
      } catch (WriteBackException failure) {
        if (flushFailure == null) {
          flushFailure = failure;
        } else {
          flushFailure.addSuppressed(failure);
        }
      }
    }

    SQLException closeFailure = null;
    for (ConnectionSource source : connectionSources) {
      try {
        if (!source.isClosed()) {
          source.shutdown();
        }
      } catch (SQLException failure) {
        if (closeFailure == null) {
          closeFailure = failure;
        } else {
          closeFailure.addSuppressed(failure);
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

  /** Best-effort close used when composition fails, preserving the original failure. */
  public void closeQuietly() {
    try {
      shutdown();
    } catch (SQLException | WriteBackException failure) {
      LOGGER.log(Level.FINE, "Failed to close partially composed plugin resources", failure);
    }
  }
}
