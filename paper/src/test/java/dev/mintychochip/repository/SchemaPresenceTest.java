package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fail-fast presence checks against real MySQL (provision ≠ create-from-app).
 */
class SchemaPresenceTest {

  private static final String DEFAULT_URL = "jdbc:mysql://localhost:13306/modularjobs";
  private static final String DEFAULT_USER = "test";
  private static final String DEFAULT_PASSWORD = "test";

  private static String jdbcUrl;
  private static String user;
  private static String password;
  private static boolean mysqlAvailable;

  private Connection connection;

  @BeforeAll
  static void detectMysql() {
    jdbcUrl = envOr("MODULARJOBS_TEST_MYSQL_URL", DEFAULT_URL);
    user = envOr("MODULARJOBS_TEST_MYSQL_USER", DEFAULT_USER);
    password = envOr("MODULARJOBS_TEST_MYSQL_PASSWORD", DEFAULT_PASSWORD);
    try {
      Class.forName(DatabaseType.MYSQL.getClassName());
      try (Connection c = DriverManager.getConnection(jdbcUrl, user, password);
           Statement st = c.createStatement()) {
        st.execute("SELECT 1");
        mysqlAvailable = true;
      }
    } catch (ClassNotFoundException | SQLException e) {
      mysqlAvailable = false;
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    assumeTrue(mysqlAvailable, "MySQL required at " + jdbcUrl);
    connection = DriverManager.getConnection(jdbcUrl, user, password);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }

  @Test
  void missingTableThrowsWithProvisionHint() throws Exception {
    String probe = "mj_schema_presence_probe_" + System.nanoTime();
    assertFalse(SchemaPresence.tableExists(connection, DatabaseType.MYSQL, probe));

    SchemaPresence.SchemaMissingException ex = assertThrows(
        SchemaPresence.SchemaMissingException.class,
        () -> SchemaPresence.requireTables(
            connection, DatabaseType.MYSQL, List.of(probe)));

    assertTrue(ex.getMessage().contains("does not create")
        || ex.getMessage().contains("not provisioned"));
    assertTrue(ex.getMessage().contains("apply-mysql-schema")
        || ex.getMessage().contains("sql/mysql.sql"));
    assertTrue(ex.getMissingTables().contains(probe));
  }

  @Test
  void provisionedTablePassesPresenceCheck() throws Exception {
    // Apply shipped DDL the same way ops would (out-of-band from the app boot path).
    for (String sql : DatabaseType.MYSQL.getSQLTables()) {
      try (Statement st = connection.createStatement()) {
        st.execute(sql);
      }
    }
    assertTrue(
        SchemaPresence.tableExists(connection, DatabaseType.MYSQL, "job_tasks"));
    SchemaPresence.requireTables(
        connection, DatabaseType.MYSQL, List.of("job_tasks", "job_task_payables"));
  }

  private static String envOr(String key, String defaultValue) {
    String v = System.getenv(key);
    return v == null || v.isBlank() ? defaultValue : v;
  }
}
