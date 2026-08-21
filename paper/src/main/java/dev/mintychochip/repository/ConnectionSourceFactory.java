package dev.mintychochip.repository;

import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Builds a MySQL {@link ConnectionSource} (HikariCP). Schema is never applied here —
 * ops must run {@code scripts/apply-mysql-schema.sh} first.
 */
public final class ConnectionSourceFactory {

  @NotNull
  private final Plugin plugin;

  @NotNull
  private final ConfigurationSection configuration;

  public ConnectionSourceFactory(
      @NotNull Plugin plugin, @NotNull ConfigurationSection configuration) {
    this.plugin = plugin;
    this.configuration = configuration;
  }

  @NotNull
  public ConnectionSource create() {
    Preconditions.checkState(
        configuration.contains("type"),
        "database section missing type (must be mysql)");
    DatabaseType type = DatabaseType.fromIdentifier(configuration.getString("type"));

    Logger log = plugin.getLogger();
    if (SchemaPolicy.hasIgnoredRemoteAutoSchema(type, configuration)) {
      log.warning(
          "database.yml sets auto-schema but ModularJobs never runs DDL in-process. "
              + "Ignore this key and provision with scripts/apply-mysql-schema.sh "
              + "(sql/mysql.sql).");
    }

    ConnectionSource source =
        new HikariSourceImpl(new HikariConfigProvider(configuration, type).create(), type);

    if (SchemaPolicy.shouldVerifySchemaPresent(type)) {
      try (Connection connection = source.getConnection()) {
        SchemaPresence.requireTables(connection, type, SchemaPresence.REQUIRED_TABLES);
      } catch (SQLException e) {
        throw new RuntimeException(
            "Failed to verify MySQL schema (is the database up and schema applied?)", e);
      }
    }

    return source;
  }
}
