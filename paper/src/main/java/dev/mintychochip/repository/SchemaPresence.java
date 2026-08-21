package dev.mintychochip.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Fail-fast check that required tables exist. Does not create them. */
public final class SchemaPresence {

  /** Core tables the plugin depends on. */
  public static final List<String> REQUIRED_TABLES =
      List.of(
          "job_progression",
          "job_tasks",
          "job_task_payables",
          "payable_records",
          "time_boosts",
          "player_upgrades");

  /** Session API table (same MySQL store). Optional for pure game-only pools. */
  public static final String EDITOR_SESSIONS = "editor_sessions";

  private static final String TABLE_EXISTS = SqlStatements.load("schema/table-exists.sql");

  private SchemaPresence() {}

  /**
   * Ensures every name in {@code required} exists in the connection's default schema.
   *
   * @throws SchemaMissingException if any table is absent
   */
  public static void requireTables(
      @NotNull Connection connection, @NotNull DatabaseType type, @NotNull List<String> required)
      throws SQLException {
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

  /** Table exists. */
  public static boolean tableExists(
      @NotNull Connection connection, @NotNull DatabaseType type, @NotNull String table)
      throws SQLException {
    if (type != DatabaseType.MYSQL) {
      throw new IllegalArgumentException("Only MySQL is supported, got " + type);
    }
    try (PreparedStatement ps = connection.prepareStatement(TABLE_EXISTS)) {
      ps.setString(1, table);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  /** Thrown when schema was not provisioned. Message points operators at the SQL script. */
  public static final class SchemaMissingException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final DatabaseType type;
    private final List<String> missingTables;

    /** Schema missing exception. */
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
      return "Database schema not provisioned for "
          + type
          + ". Missing tables: "
          + missing
          + ". The plugin does not create tables. Apply "
          + "paper/src/main/resources/sql/mysql.sql out-of-band "
          + "(see scripts/apply-mysql-schema.sh).";
    }
  }
}
