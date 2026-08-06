package net.aincraft.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import net.aincraft.repository.DatabaseType;

/**
 * Shared helper for tests that need a live PostgreSQL instance.
 *
 * <p>Defaults: {@code jdbc:postgresql://localhost:55432/modularjobs} user/password {@code test}.
 * Override with {@code MODULARJOBS_TEST_PG_URL}, {@code MODULARJOBS_TEST_PG_USER},
 * {@code MODULARJOBS_TEST_PG_PASSWORD}.
 */
public final class TestPostgres {

  public static final String DEFAULT_URL = "jdbc:postgresql://localhost:55432/modularjobs";
  public static final String DEFAULT_USER = "test";
  public static final String DEFAULT_PASSWORD = "test";

  private TestPostgres() {}

  public static String jdbcUrl() {
    return envOr("MODULARJOBS_TEST_PG_URL", DEFAULT_URL);
  }

  public static String user() {
    return envOr("MODULARJOBS_TEST_PG_USER", DEFAULT_USER);
  }

  public static String password() {
    return envOr("MODULARJOBS_TEST_PG_PASSWORD", DEFAULT_PASSWORD);
  }

  public static boolean isAvailable() {
    try {
      Class.forName(DatabaseType.POSTGRES.getClassName());
      try (Connection c = DriverManager.getConnection(jdbcUrl(), user(), password());
          Statement st = c.createStatement()) {
        st.execute("SELECT 1");
        return true;
      }
    } catch (Exception e) {
      return false;
    }
  }

  /** Skip the calling test when Postgres is not reachable. */
  public static void assumeAvailable() {
    assumeTrue(isAvailable(), "PostgreSQL must be reachable at " + jdbcUrl());
  }

  public static Connection open() throws SQLException {
    assumeAvailable();
    return DriverManager.getConnection(jdbcUrl(), user(), password());
  }

  public static void applyShippedSchema(Connection connection) throws SQLException {
    String[] tables = DatabaseType.POSTGRES.getSQLTables();
    try (Statement st = connection.createStatement()) {
      for (String sql : tables) {
        st.execute(sql);
      }
    }
  }

  private static String envOr(String key, String fallback) {
    String v = System.getenv(key);
    return v == null || v.isBlank() ? fallback : v;
  }
}
