package net.aincraft.repository;

import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.logging.Logger;
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
    ConnectionSource source = switch (type) {
      case SQLITE -> {
        if (!configuration.contains("file-path")) {
          throw new IllegalArgumentException(
              "provided configuration does not contain file-path for a SQLite database");
        }
        yield SQLiteSourceImpl.create(plugin, configuration.getString("file-path"));
      }
      case MYSQL, MARIADB, POSTGRES -> new HikariSourceImpl(
          new HikariConfigProvider(configuration, type).create(), type);
      default -> throw new IllegalStateException("Unsupported database type: " + type);
    };

    Logger log = plugin.getLogger();

    if (SchemaPolicy.hasIgnoredRemoteAutoSchema(type, configuration)) {
      log.warning(
          "database.yml sets auto-schema for type '" + type.getIdentifier()
              + "' but remote dialects never run DDL in the plugin process. "
              + "Ignore this key and provision schema with sql/" + type.getIdentifier()
              + ".sql (scripts/apply-postgres-schema.sh for Postgres).");
    }

    // SQLite only: bootstrap local file tables in-process.
    if (SchemaPolicy.shouldApplySchemaOnConnect(type, configuration)) {
      applyShippedSchema(source, type);
    }

    // Remote: connect-only + fail fast if ops never applied DDL.
    if (SchemaPolicy.shouldVerifySchemaPresent(type)) {
      try (Connection connection = source.getConnection()) {
        SchemaPresence.requireTables(connection, type, SchemaPresence.REQUIRED_TABLES);
      } catch (SQLException e) {
        throw new RuntimeException(
            "Failed to verify database schema for type " + type.getIdentifier(), e);
      }
    }

    return source;
  }

  /**
   * Applies dialect DDL from {@code sql/&lt;type&gt;.sql}. Intended for SQLite bootstrap only
   * (and tests). Remote provision uses {@code scripts/apply-postgres-schema.sh}.
   */
  static void applyShippedSchema(@NotNull ConnectionSource source, @NotNull DatabaseType type) {
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
}
