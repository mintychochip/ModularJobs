package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Applies the shipped {@code sql/mysql.sql} DDL and verifies write→read fidelity for
 * job-task / payable / progression fields against a real MySQL instance.
 *
 * <p>Connection defaults to {@code jdbc:mysql://localhost:13306/modularjobs} (user/password
 * {@code test}). Override with env {@code MODULARJOBS_TEST_MYSQL_URL},
 * {@code MODULARJOBS_TEST_MYSQL_USER}, {@code MODULARJOBS_TEST_MYSQL_PASSWORD}.
 */
class MysqlSchemaFidelityTest {

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
    } catch (Exception e) {
      mysqlAvailable = false;
      System.err.println("MySQL not available for fidelity tests: " + e.getMessage());
    }
  }

  @BeforeEach
  void setUp() throws SQLException {
    // Connection opened only for live round-trip tests (see requireMysql()).
  }

  @AfterEach
  void tearDown() throws SQLException {
    if (connection != null && !connection.isClosed()) {
      cleanTables(connection);
      connection.close();
    }
  }
  private void requireMysql() throws SQLException {
    assumeTrue(mysqlAvailable, "MySQL must be reachable at " + jdbcUrl);
    if (connection == null || connection.isClosed()) {
      connection = DriverManager.getConnection(jdbcUrl, user, password);
      connection.setAutoCommit(true);
      applyShippedSchema(connection);
      cleanTables(connection);
    }
  }

  @Test
  void mysqlDriverClassIsConfigured() {
    assertEquals("com.mysql.cj.jdbc.Driver", DatabaseType.MYSQL.getClassName());
    assertEquals("mysql", DatabaseType.MYSQL.getIdentifier());
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier("mysql"));
  }

  @Test
  void shippedMysqlDdlUsesMysqlTypes() {
    String[] statements = DatabaseType.MYSQL.getSQLTables();
    assertTrue(statements.length > 0, "mysql.sql must produce statements");
    String joined = String.join("\n", statements).toUpperCase();
    assertFalse(joined.contains("SERIAL"), "MySQL DDL must not use SERIAL");
    assertTrue(joined.contains("AUTO_INCREMENT"), "task_id must use AUTO_INCREMENT");
    assertTrue(joined.contains("DECIMAL"), "amounts/experience must use DECIMAL");
    assertTrue(joined.contains("ENGINE=INNODB"), "tables must use InnoDB");
  }

  @Test
  void jobTaskPayableRoundTripPreservesKeysAndPreciseAmount() throws SQLException {
    requireMysql();
    // Insert task
    int taskId;
    try (PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO job_tasks (job_key, action_type_key, context_key) VALUES (?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, "modularjobs:miner");
      ps.setString(2, "modularjobs:block_break");
      ps.setString(3, "minecraft:diamond_ore");
      assertEquals(1, ps.executeUpdate());
      try (ResultSet keys = ps.getGeneratedKeys()) {
        assertTrue(keys.next());
        taskId = keys.getInt(1);
      }
    }
    assertTrue(taskId > 0);

    BigDecimal amount = new BigDecimal("1234.5678901234");
    try (PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO job_task_payables (job_task_id, payable_type_key, amount, currency_identifier) "
            + "VALUES (?, ?, ?, ?)")) {
      ps.setInt(1, taskId);
      ps.setString(2, "modularjobs:experience");
      ps.setBigDecimal(3, amount);
      ps.setString(4, null); // nullable currency
      assertEquals(1, ps.executeUpdate());
    }

    try (PreparedStatement ps = connection.prepareStatement(
        """
            SELECT t.job_key, t.action_type_key, t.context_key,
                   p.payable_type_key, p.amount, p.currency_identifier
            FROM job_tasks t
            JOIN job_task_payables p ON p.job_task_id = t.task_id
            WHERE t.task_id = ?
            """)) {
      ps.setInt(1, taskId);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("modularjobs:miner", rs.getString("job_key"));
        assertEquals("modularjobs:block_break", rs.getString("action_type_key"));
        assertEquals("minecraft:diamond_ore", rs.getString("context_key"));
        assertEquals("modularjobs:experience", rs.getString("payable_type_key"));
        BigDecimal readAmount = rs.getBigDecimal("amount");
        assertNotNull(readAmount);
        assertEquals(0, amount.compareTo(readAmount),
            "NUMERIC amount must round-trip without loss; expected " + amount + " got " + readAmount);
        assertNull(rs.getString("currency_identifier"));
        assertFalse(rs.next());
      }
    }
  }

  @Test
  void payableRecordsCompositeKeyAndCurrencyRoundTrip() throws SQLException {
    requireMysql();
    BigDecimal amount = new BigDecimal("99.1250000000");
    try (PreparedStatement ps = connection.prepareStatement(
        """
            INSERT INTO payable_records
              (job_key, action_type_key, context_key, payable_type_key, amount, currency)
            VALUES (?, ?, ?, ?, ?, ?)
            """)) {
      ps.setString(1, "modularjobs:fisherman");
      ps.setString(2, "modularjobs:fish");
      ps.setString(3, "minecraft:cod");
      ps.setString(4, "modularjobs:economy");
      ps.setBigDecimal(5, amount);
      ps.setString(6, "test:default");
      assertEquals(1, ps.executeUpdate());
    }

    try (PreparedStatement ps = connection.prepareStatement(
        """
            SELECT amount, currency FROM payable_records
            WHERE job_key = ? AND action_type_key = ? AND context_key = ? AND payable_type_key = ?
            """)) {
      ps.setString(1, "modularjobs:fisherman");
      ps.setString(2, "modularjobs:fish");
      ps.setString(3, "minecraft:cod");
      ps.setString(4, "modularjobs:economy");
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        assertEquals(0, amount.compareTo(rs.getBigDecimal("amount")));
        assertEquals("test:default", rs.getString("currency"));
      }
    }
  }

  @Test
  void progressionExperienceRoundTrip() throws SQLException {
    requireMysql();
    BigDecimal exp = new BigDecimal("5000.2500000000");
    try (PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO job_progression (player_id, job_key, experience) VALUES (?, ?, ?)")) {
      ps.setString(1, "11111111-2222-3333-4444-555555555555");
      ps.setString(2, "modularjobs:miner");
      ps.setBigDecimal(3, exp);
      assertEquals(1, ps.executeUpdate());
    }

    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT experience FROM job_progression WHERE player_id = ? AND job_key = ?")) {
      ps.setString(1, "11111111-2222-3333-4444-555555555555");
      ps.setString(2, "modularjobs:miner");
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        assertEquals(0, exp.compareTo(rs.getBigDecimal("experience")));
      }
    }
  }

  private static void applyShippedSchema(Connection connection) throws SQLException {
    String[] tables = DatabaseType.MYSQL.getSQLTables();
    assertNotNull(tables);
    assertTrue(tables.length > 0);
    try (Statement st = connection.createStatement()) {
      for (String sql : tables) {
        st.execute(sql);
      }
    }
  }

  private static void cleanTables(Connection connection) throws SQLException {
    try (Statement st = connection.createStatement()) {
      // order matters for FKs
      for (String table : Arrays.asList(
          "job_task_payables",
          "job_tasks",
          "payable_records",
          "job_progression",
          "archive_job_progression",
          "time_boosts",
          "time_boost_identity",
          "player_upgrades",
          "editor_sessions")) {
        st.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY CASCADE");
      }
    } catch (SQLException e) {
      // Tables may not all exist on first apply failure; ignore for cleanup
    }
  }

  private static String envOr(String key, String defaultValue) {
    String v = System.getenv(key);
    return v == null || v.isBlank() ? defaultValue : v;
  }
}
