package net.aincraft.repository;

import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class ConnectionSourceFactory {

  @NotNull
  private final Plugin plugin;

  @NotNull
  private final ConfigurationSection configuration;

  public ConnectionSourceFactory(@NotNull Plugin plugin, @NotNull ConfigurationSection configuration) {
    this.plugin = plugin;
    this.configuration = configuration;
  }

  @NotNull
  public ConnectionSource create()
      throws IllegalStateException {
    Preconditions.checkState(configuration.contains("type"));
    DatabaseType type = DatabaseType.fromIdentifier(configuration.getString("type"));
    ConnectionSource source = switch(type) {
      case SQLITE -> {
        if (!configuration.contains("type")) {
          throw new IllegalArgumentException("provided configuration does not contain file-path for a SQLite database");
        }
        yield SQLiteSourceImpl.create(plugin, configuration.getString("file-path"));
      }
      case MYSQL, MARIADB, POSTGRES -> new HikariSourceImpl(new HikariConfigProvider(configuration).create(),type);
      default -> null;
    };
    if (!source.isSetup()) {
      String[] tables = type.getSQLTables();
      try (Connection connection = source.getConnection()) {
        connection.setAutoCommit(false);
        Savepoint savepoint = connection.setSavepoint();

        try (Statement stmt = connection.createStatement()) {
          for (String query : tables) {
            stmt.addBatch(query);
          }
          stmt.executeBatch();
          connection.commit();
        } catch (SQLException e) {
          connection.rollback(savepoint);
          throw new SQLException("Error executing bulk SQL", e);
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    return source;
  }
}
