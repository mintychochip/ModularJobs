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
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class SQLiteSourceImpl implements ConnectionSource {

  private final Plugin plugin;
  private final Path databaseFilePath;
  private NonClosableConnection connection;

  private SQLiteSourceImpl(Plugin plugin, Path databaseFilePath) {
    this.plugin = plugin;
    this.databaseFilePath = databaseFilePath;
  }

  static SQLiteSourceImpl create(@NotNull Plugin plugin, String relativePath) {
    File dataFolder = plugin.getDataFolder();
    Path databaseFilePath = dataFolder.toPath().resolve(relativePath);
    try {
      Class.forName(DatabaseType.SQLITE.getClassName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    File databaseFile = new File(databaseFilePath.toString());
    File parentFile = databaseFile.getParentFile();
    if (!parentFile.exists()) {
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
    return new SQLiteSourceImpl(plugin, databaseFilePath);
  }

  @NotNull
  private static String getUrl(@NotNull Path databaseFilePath) {
    return String.format("jdbc:%s:%s", DatabaseType.SQLITE.getIdentifier(),
        databaseFilePath.toAbsolutePath());
  }

  @Override
  public void shutdown() throws SQLException {
    connection.shutdown();
  }

  @Override
  public DatabaseType getType() {
    return DatabaseType.SQLITE;
  }

  @Override
  public boolean isClosed() {
    try {
      return connection.isClosed();
    } catch (SQLException ex) {
      throw new RuntimeException();
    }
  }

  @Override
  public Connection getConnection() {
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
    try (Connection connection = getConnection();
         PreparedStatement ps = connection.prepareStatement(
             "SELECT 1 FROM sqlite_master WHERE type='table' LIMIT 1");
         ResultSet rs = ps.executeQuery()) {
      return rs.next();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
