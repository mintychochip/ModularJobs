package net.aincraft.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.repository.JobProgressionRepository;
import net.aincraft.repository.ConnectionSource;
import net.aincraft.repository.DatabaseType;
import net.aincraft.repository.NonClosableConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * MySQL (real SQL path, fake job catalog only). Requires live MySQL (see {@link net.aincraft.test.TestMysql}).
 */
class RelationalJobProgressionRepositoryTest {

  private static final String TABLE = "job_progressions_test";

  private Connection connection;
  private JobProgressionRepository repository;
  private JobRecord miner;
  private JobRecord fisher;

  @BeforeEach
  void setUp() throws Exception {
    net.aincraft.test.TestMysql.assumeAvailable();
    Connection raw = net.aincraft.test.TestMysql.open();
    connection = NonClosableConnection.create(raw);
    try (Statement st = connection.createStatement()) {
      st.execute("DROP TABLE IF EXISTS " + TABLE);
      st.execute("""
          CREATE TABLE job_progressions_test (
            player_id  VARCHAR(191)    NOT NULL,
            job_key    VARCHAR(191)    NOT NULL,
            experience DECIMAL(38, 10) NOT NULL,
            PRIMARY KEY (player_id, job_key)
          )
          """);
    }

    miner = new JobRecord(
        "modularjobs:miner", "Miner", "Mines", 100, "level * 100",
        Map.of(), 30, Map.of()
    );
    fisher = new JobRecord(
        "modularjobs:fisherman", "Fisher", "Fish", 50, "level * 50",
        Map.of(), 20, Map.of()
    );

    Map<String, JobRecord> jobs = new ConcurrentHashMap<>();
    jobs.put(miner.jobKey(), miner);
    jobs.put(fisher.jobKey(), fisher);

    repository = RelationalJobProgressionRepositoryImpl.create(
        new MemoryJobRepositoryImpl(jobs),
        new FixedConnectionSource(connection),
        TABLE
    );
  }

  @AfterEach
  void tearDown() throws SQLException {
    if (connection != null) {
      try (Statement st = connection.createStatement()) {
        st.execute("DROP TABLE IF EXISTS " + TABLE);
      } catch (SQLException ignored) {
        // best-effort
      }
      if (connection instanceof NonClosableConnection nc) {
        nc.shutdown();
      }
    }
  }

  @Test
  void saveThenLoadReturnsExperience() {
    JobProgressionRecord record = new JobProgressionRecord(
        "player-1", miner, new BigDecimal("1234.50"));
    assertTrue(repository.save(record));

    JobProgressionRecord loaded = repository.load("player-1", "modularjobs:miner");
    assertNotNull(loaded);
    assertEquals("player-1", loaded.playerId());
    assertEquals("modularjobs:miner", loaded.jobRecord().jobKey());
    assertEquals(0, new BigDecimal("1234.50").compareTo(loaded.experience()));
  }

  @Test
  void saveUpsertsExperience() {
    repository.save(new JobProgressionRecord("player-1", miner, new BigDecimal("100")));
    repository.save(new JobProgressionRecord("player-1", miner, new BigDecimal("999")));

    JobProgressionRecord loaded = repository.load("player-1", "modularjobs:miner");
    assertNotNull(loaded);
    assertEquals(0, new BigDecimal("999").compareTo(loaded.experience()));
  }

  @Test
  void loadUnknownOrMissingJobReturnsNull() {
    assertNull(repository.load("nobody", "modularjobs:miner"));
    repository.save(new JobProgressionRecord("player-1", miner, BigDecimal.TEN));
    // job catalog missing → load must not invent a record for unknown job key
    assertNull(repository.load("player-1", "modularjobs:missing"));
  }

  @Test
  void loadAllForPlayerRespectsLimit() {
    repository.save(new JobProgressionRecord("p1", miner, new BigDecimal("10")));
    repository.save(new JobProgressionRecord("p1", fisher, new BigDecimal("20")));

    List<JobProgressionRecord> limited = repository.loadAllForPlayer("p1", 1);
    assertEquals(1, limited.size());

    List<JobProgressionRecord> all = repository.loadAllForPlayer("p1", 10);
    assertEquals(2, all.size());
  }

  @Test
  void loadAllForJobOrdersByExperienceDescending() {
    repository.save(new JobProgressionRecord("low", miner, new BigDecimal("10")));
    repository.save(new JobProgressionRecord("high", miner, new BigDecimal("500")));
    repository.save(new JobProgressionRecord("mid", miner, new BigDecimal("100")));

    List<JobProgressionRecord> top = repository.loadAllForJob("modularjobs:miner", 10);
    assertEquals(3, top.size());
    assertEquals("high", top.get(0).playerId());
    assertEquals(0, new BigDecimal("500").compareTo(top.get(0).experience()));
  }

  @Test
  void deleteRemovesRowAndCache() {
    repository.save(new JobProgressionRecord("player-1", miner, new BigDecimal("50")));
    assertNotNull(repository.load("player-1", "modularjobs:miner"));

    assertTrue(repository.delete("player-1", "modularjobs:miner"));
    assertNull(repository.load("player-1", "modularjobs:miner"));
    assertFalse(repository.delete("player-1", "modularjobs:miner"));
  }


  /** Reuses a single open JDBC connection (shared NonClosableConnection). */
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
