package dev.mintychochip.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import dev.mintychochip.repository.DatabaseType;

/** Shared helper for tests that need a live MySQL 8 instance. */
public final class MysqlTestSupport {

  public static final String DEFAULT_URL = "jdbc:mysql://localhost:13306/modularjobs";
  public static final String DEFAULT_USER = "test";
  public static final String DEFAULT_PASSWORD = "test";

  private MysqlTestSupport() {}

  public static String jdbcUrl() {
    return envOr("MODULARJOBS_TEST_MYSQL_URL", DEFAULT_URL);
  }

  public static String user() {
    return envOr("MODULARJOBS_TEST_MYSQL_USER", DEFAULT_USER);
  }

  public static String password() {
    return envOr("MODULARJOBS_TEST_MYSQL_PASSWORD", DEFAULT_PASSWORD);
  }

  public static boolean isAvailable() {
    try {
      Class.forName(DatabaseType.MYSQL.getClassName());
      try (Connection c = DriverManager.getConnection(jdbcUrl(), user(), password());
          Statement st = c.createStatement()) {
        st.execute("SELECT 1");
        return true;
      }
    } catch (ClassNotFoundException | SQLException e) {
      return false;
    }
  }

  public static void assumeAvailable() {
    assumeTrue(isAvailable(), "MySQL must be reachable at " + jdbcUrl());
  }

  public static Connection open() throws SQLException {
    return DriverManager.getConnection(jdbcUrl(), user(), password());
  }

  public static void applyShippedSchema(Connection connection) throws SQLException {
    for (String sql : DatabaseType.MYSQL.getSQLTables()) {
      try (Statement st = connection.createStatement()) {
        st.execute(sql);
      }
    }
  }

  private static String envOr(String key, String defaultValue) {
    String value = System.getenv(key);
    return value == null || value.isBlank() ? defaultValue : value;
  }
}
