package net.aincraft.repository;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Supplies database connections and reports the configured database lifecycle.
 *
 * <p>The application connects to an existing schema; schema creation and migration are
 * performed externally rather than by this abstraction.
 */
public interface ConnectionSource {

  /** Closes the underlying connection pool or source. */
  void shutdown() throws SQLException;

  /** Returns the configured database dialect. */
  DatabaseType getType();

  /** Returns whether the underlying source has been closed. */
  boolean isClosed();

  /** Obtains a connection that the caller must close. */
  Connection getConnection() throws SQLException;

  /** Returns whether the connection source has completed setup. */
  boolean isSetup();
}
