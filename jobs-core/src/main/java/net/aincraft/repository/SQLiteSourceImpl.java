package net.aincraft.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Ephemeral-connection SQLite source: each {@link #getConnection()} opens a new JDBC
 * connection (callers close it). Lifecycle is a simple closed flag — not a long-lived
 * connection field — so {@link #isClosed()} / {@link #shutdown()} never NPE.
 */
final class SQLiteSourceImpl implements ConnectionSource {

  private final Path databaseFilePath;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private SQLiteSourceImpl(Path databaseFilePath) {
    this.databaseFilePath = databaseFilePath;
  }

  static SQLiteSourceImpl create(@NotNull Plugin plugin, String relativePath) {
    File dataFolder = plugin.getDataFolder();
    Path databaseFilePath = dataFolder.toPath().resolve(relativePath);
    return createAtPath(databaseFilePath);
  }

  /**
   * Test / tool entry that uses a concrete file path (no Bukkit plugin required).
   */
  static SQLiteSourceImpl createAtPath(@NotNull Path databaseFilePath) {
    try {
      Class.forName(DatabaseType.SQLITE.getClassName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    File databaseFile = databaseFilePath.toFile();
    File parentFile = databaseFile.getParentFile();
    if (parentFile != null && !parentFile.exists()) {
      parentFile.mkdirs();
    }
    if (!databaseFile.exists()) {
      try {
        if (!databaseFile.createNewFile()) {
          throw new IOException("failed to create database flat file");
        }
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
    return new SQLiteSourceImpl(databaseFilePath);
  }

  @NotNull
  private static String getUrl(@NotNull Path databaseFilePath) {
    return String.format("jdbc:%s:%s", DatabaseType.SQLITE.getIdentifier(),
        databaseFilePath.toAbsolutePath());
  }

  @Override
  public void shutdown() {
    closed.set(true);
  }

  @Override
  public DatabaseType getType() {
    return DatabaseType.SQLITE;
  }

  @Override
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public Connection getConnection() {
    if (closed.get()) {
      throw new IllegalStateException("SQLite ConnectionSource is closed: " + databaseFilePath);
    }
    try {
      Connection connection = DriverManager.getConnection(getUrl(databaseFilePath));
      try (Statement st = connection.createStatement()) {
        st.execute("PRAGMA foreign_keys=ON;");
        st.execute("PRAGMA synchronous=NORMAL;");
      }
      return connection;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to open SQLite connection", e);
    }
  }

  @Override
  public boolean isSetup() {
    try (Connection connection = getConnection()) {
      PreparedStatement ps = connection.prepareStatement(
          "SELECT 1 FROM sqlite_master WHERE type='table' LIMIT 1");
      ResultSet rs = ps.executeQuery();
      return rs.next();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
