package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.aincraft.boost.MultiplicativeBoostImpl;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.boost.conditions.SneakConditionImpl;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import net.aincraft.serialization.KryoCodecRegistry;
import net.aincraft.service.TimedBoostDataServiceImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises shipped {@link RelationalTimedBoostRepositoryImpl} SQL identity:
 * pure {@code target_id} in the DB (not composite write-back key), so
 * findAllBoosts(target) and delete(target, source) work after persistence.
 */
class RelationalTimedBoostRepositoryIdentityTest {

  private Connection connection;
  private RelationalTimedBoostRepositoryImpl repository;
  private BoostSource source;
  private static final String TARGET = "player-uuid-1111-2222-3333";
  private static final String SOURCE_ID = "modularjobs:timed_test";

  @BeforeEach
  void setUp() throws Exception {
    net.aincraft.test.TestMysql.assumeAvailable();
    // Wrap so try-with-resources in RelationalRepositoryImpl does not close the shared connection
    Connection raw = net.aincraft.test.TestMysql.open();
    connection = NonClosableConnection.create(raw);
    try (Statement st = connection.createStatement()) {
      st.execute("DROP TABLE IF EXISTS time_boosts");
      st.execute("""
          CREATE TABLE time_boosts (
            target_id    VARCHAR(191) NOT NULL,
            source_id    VARCHAR(191) NOT NULL,
            epoch_millis BIGINT       NOT NULL,
            duration     BLOB         NULL,
            boost_source BLOB         NOT NULL,
            PRIMARY KEY (target_id, source_id)
          )
          """);
    }

    ConnectionSource connectionSource = new FixedConnectionSource(connection);
    repository = RelationalTimedBoostRepositoryImpl.createSynchronous(
        connectionSource, new KryoCodecRegistry());

    source = new RuledBoostSourceImpl(
        List.of(new Rule(new SneakConditionImpl(false), 1,
            new MultiplicativeBoostImpl(new BigDecimal("2.0")))),
        Key.key("modularjobs", "timed_test"),
        "test"
    );
  }

  @AfterEach
  void tearDown() throws SQLException {
    if (connection != null) {
      try (Statement st = connection.createStatement()) {
        st.execute("DROP TABLE IF EXISTS time_boosts");
      } catch (SQLException ignored) {
        // best-effort
      }
      if (connection instanceof NonClosableConnection nonClosable) {
        nonClosable.shutdown();
      } else if (!connection.isClosed()) {
        connection.close();
      }
    }
  }

  @Test
  void saveStoresPureTargetIdNotCompositeCacheKey() throws SQLException {
    ActiveBoostData boost = new ActiveBoostData(
        TARGET,
        SOURCE_ID,
        Timestamp.from(Instant.now()),
        Duration.ofHours(1),
        source
    );
    repository.addBoost(boost);

    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT target_id, source_id FROM time_boosts");
        ResultSet rs = ps.executeQuery()) {
      assertTrue(rs.next());
      assertEquals(TARGET, rs.getString("target_id"),
          "DB target_id must be pure player/global id, not composite cache key");
      assertEquals(SOURCE_ID, rs.getString("source_id"));
      assertFalse(rs.getString("target_id").contains(SOURCE_ID),
          "target_id must not embed source id");
      assertFalse(rs.next());
    }
  }

  @Test
  void saveFindAllDeleteRoundTripWithPureTargetId() throws SQLException {
    ActiveBoostData boost = new ActiveBoostData(
        TARGET,
        SOURCE_ID,
        Timestamp.from(Instant.now()),
        Duration.ofHours(1),
        source
    );
    repository.addBoost(boost);

    List<ActiveBoostData> found = repository.findAllBoosts(TARGET);
    assertEquals(1, found.size());
    assertEquals(TARGET, found.get(0).targetIdentifier());
    assertEquals(SOURCE_ID, found.get(0).sourceIdentifier());
    assertNotNull(found.get(0).boostSource());

    ActiveBoostData single = repository.findBoost(TARGET, SOURCE_ID);
    assertNotNull(single);
    assertEquals(TARGET, single.targetIdentifier());

    repository.delete(TARGET, SOURCE_ID);
    assertTrue(repository.findAllBoosts(TARGET).isEmpty());
    assertNull(repository.findBoost(TARGET, SOURCE_ID));

    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT COUNT(*) FROM time_boosts");
        ResultSet rs = ps.executeQuery()) {
      assertTrue(rs.next());
      assertEquals(0, rs.getInt(1), "row must be deleted from real storage");
    }
  }

  @Test
  void serviceExpiryCleanupDeletesRealSqlRow() throws SQLException {
    Instant started = Instant.now().minus(Duration.ofHours(2));
    ActiveBoostData expired = new ActiveBoostData(
        TARGET,
        SOURCE_ID,
        Timestamp.from(started),
        Duration.ofMinutes(5),
        source
    );
    repository.addBoost(expired);

    TimedBoostDataServiceImpl service = new TimedBoostDataServiceImpl(repository);
    // Player target needs Player — use global path via findApplicableBoosts after forcing
    // target as global for this check: re-insert under "global"
    repository.delete(TARGET, SOURCE_ID);
    ActiveBoostData globalExpired = new ActiveBoostData(
        "global",
        SOURCE_ID,
        Timestamp.from(started),
        Duration.ofMinutes(5),
        source
    );
    repository.addBoost(globalExpired);
    assertEquals(1, countRows());

    List<ActiveBoostData> applicable = service.findApplicableBoosts(
        new net.aincraft.container.boost.TimedBoostDataService.Target.GlobalTarget());
    assertTrue(applicable.isEmpty());
    assertEquals(0, countRows(), "expired cleanup must DELETE real SQL row");
  }

  private int countRows() throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM time_boosts");
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      return rs.getInt(1);
    }
  }

  /** ConnectionSource that reuses a single open JDBC connection (live MySQL). */
  private static final class FixedConnectionSource implements ConnectionSource {
    private final Connection connection;

    FixedConnectionSource(Connection connection) {
      this.connection = connection;
    }

    @Override
    public @NotNull Connection getConnection() {
      return connection;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public boolean isClosed() {
      try {
        return connection.isClosed();
      } catch (SQLException e) {
        return true;
      }
    }

    @Override
    public DatabaseType getType() {
      return DatabaseType.MYSQL;
    }

    @Override
    public boolean isSetup() {
      return true;
    }
  }
}
