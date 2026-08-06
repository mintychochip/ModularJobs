package net.aincraft.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Fail-fast check that required tables exist. Does not create them.
 *
 * <p>Used for remote databases so a missing provision step fails at pool connect with a clear
 * message pointing at out-of-band schema application.
 */
public final class SchemaPresence {

  /** Core tables the plugin and session API depend on. */
  public static final List<String> REQUIRED_TABLES = List.of(
      "job_progression",
      "job_tasks",
      "job_task_payables",
      "payable_records",
      "time_boosts",
      "player_upgrades"
  );

  /** Session API table (same Postgres store). Optional for pure game-only pools. */
  public static final String EDITOR_SESSIONS = "editor_sessions";

  private SchemaPresence() {}

  /**
   * Ensures every name in {@code required} exists in the connection's default schema.
   *
   * @throws SchemaMissingException if any table is absent
   */
  public static void requireTables(
      @NotNull Connection connection,
      @NotNull DatabaseType type,
      @NotNull List<String> required)
      throws SQLException, SchemaMissingException {
    List<String> missing = new ArrayList<>();
    for (String table : required) {
      if (!tableExists(connection, type, table)) {
        missing.add(table);
      }
    }
    if (!missing.isEmpty()) {
      throw new SchemaMissingException(type, missing);
    }
  }

  public static boolean tableExists(
      @NotNull Connection connection, @NotNull DatabaseType type, @NotNull String table)
      throws SQLException {
    return switch (type) {
      case POSTGRES -> existsPostgres(connection, table);
      case MYSQL, MARIADB -> existsMysql(connection, table);
      case SQLITE -> existsSqlite(connection, table);
      default -> existsPostgres(connection, table);
    };
  }

  private static boolean existsPostgres(Connection connection, String table) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(
        """
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = current_schema()
              AND table_name = ?
            """)) {
      ps.setString(1, table);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private static boolean existsMysql(Connection connection, String table) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(
        """
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = ?
            """)) {
      ps.setString(1, table);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private static boolean existsSqlite(Connection connection, String table) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
      ps.setString(1, table);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * Thrown when remote schema was not provisioned. Message points operators at the SQL script.
   */
  public static final class SchemaMissingException extends RuntimeException {
    private final DatabaseType type;
    private final List<String> missingTables;

    public SchemaMissingException(DatabaseType type, List<String> missingTables) {
      super(buildMessage(type, missingTables));
      this.type = type;
      this.missingTables = List.copyOf(missingTables);
    }

    public DatabaseType getType() {
      return type;
    }

    public List<String> getMissingTables() {
      return missingTables;
    }

    private static String buildMessage(DatabaseType type, List<String> missing) {
      return "Database schema not provisioned for type '" + type.getIdentifier()
          + "'. Missing tables: " + missing
          + ". The plugin does not create remote tables. Apply "
          + "jobs-core/src/main/resources/sql/" + type.getIdentifier() + ".sql "
          + "out-of-band (see scripts/apply-postgres-schema.sh for Postgres).";
    }
  }
}
