package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import dev.mintychochip.test.MysqlTestSupport;
import dev.mintychochip.upgrade.PlayerUpgradeDataImpl;
import dev.mintychochip.upgrade.PlayerUpgradeRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link SqlStatements} DML through the production Hikari pool.
 */
class SqlStatementsHikariRepositoryTest {

  private static final String PLAYER_ID = "sql-statements-hikari-player";
  private static final String JOB_KEY = "modularjobs:miner";

  private ConnectionSource hikari;

  @AfterEach
  void tearDown() throws SQLException {
    if (hikari == null || hikari.isClosed()) {
      return;
    }
    try (Connection connection = hikari.getConnection();
        PreparedStatement ps = connection.prepareStatement(
            SqlStatements.load("player_upgrades/delete.sql"))) {
      ps.setString(1, PLAYER_ID);
      ps.setString(2, JOB_KEY);
      ps.executeUpdate();
    } catch (SQLException ignored) {
      // best-effort cleanup before pool close
    }
    hikari.shutdown();
  }

  @Test
  void loadReadsClasspathSqlAndFailsOnMissingResource() {
    String select = SqlStatements.load("player_upgrades/select.sql");
    assertFalse(select.isBlank());
    assertTrue(select.contains("player_upgrades"));
    assertTrue(SqlStatements.load("job_progression/save.sql").contains("{table}"));
    IllegalStateException missing = assertThrows(
        IllegalStateException.class,
        () -> SqlStatements.load("does-not-exist.sql"));
    assertTrue(missing.getMessage().contains("missing SQL resource"));
  }

  @Test
  void hikariPoolRepositoryRoundTripUsesLoadedSql() throws Exception {
    hikari = openHikari();
    assertInstanceOf(HikariSourceImpl.class, hikari);
    try (Connection pooled = hikari.getConnection()) {
      assertNotNull(pooled);
      assertFalse(pooled.isClosed());
      assertTrue(
          pooled.getClass().getName().contains("Hikari"),
          "connection must come from shipped Hikari pool, got " + pooled.getClass().getName());
    }

    PlayerUpgradeRepository upgrades = new PlayerUpgradeRepository(hikari);
    PlayerUpgradeDataImpl stored = new PlayerUpgradeDataImpl(
        PLAYER_ID, JOB_KEY, 9, Set.of("node-a", "node-b"));
    upgrades.savePlayerData(stored);

    PlayerUpgradeDataImpl loaded = upgrades.loadPlayerData(PLAYER_ID, JOB_KEY);
    assertNotNull(loaded);
    assertEquals(9, loaded.totalSkillPoints());
    assertEquals(Set.of("node-a", "node-b"), loaded.unlockedNodes());

    String select = SqlStatements.load("player_upgrades/select.sql");
    try (Connection connection = hikari.getConnection();
        PreparedStatement ps = connection.prepareStatement(select)) {
      ps.setString(1, PLAYER_ID);
      ps.setString(2, JOB_KEY);
      try (ResultSet rs = ps.executeQuery()) {
        boolean hasRow = rs.next();
        assertTrue(hasRow, "row must be readable with the same SqlStatements resource");
        assertEquals(9, rs.getInt("total_skill_points"));
      }
    }

    assertTrue(upgrades.deletePlayerData(PLAYER_ID, JOB_KEY));
    assertNull(upgrades.loadPlayerData(PLAYER_ID, JOB_KEY));
  }

  private ConnectionSource openHikari() throws SQLException {
    MysqlTestSupport.assumeAvailable();
    YamlConfiguration config = new YamlConfiguration();
    config.set("jdbc-url", MysqlTestSupport.jdbcUrl());
    config.set("username", MysqlTestSupport.user());
    config.set("password", MysqlTestSupport.password());
    config.set("maximum-pool-size", 2);
    config.set("minimum-idle", 0);
    ConnectionSource source = new HikariSourceImpl(
        new HikariConfigProvider(config, DatabaseType.MYSQL).create(),
        DatabaseType.MYSQL);
    try (Connection connection = source.getConnection()) {
      MysqlTestSupport.applyShippedSchema(connection);
      try (PreparedStatement ps = connection.prepareStatement(
          SqlStatements.load("player_upgrades/delete.sql"))) {
        ps.setString(1, PLAYER_ID);
        ps.setString(2, JOB_KEY);
        ps.executeUpdate();
      }
    }
    return source;
  }
}
