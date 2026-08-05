package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves shipped {@link WriteBackRepositoryImpl#flushPending()} drains pending state
 * before ConnectionSource shutdown (disable path).
 */
class WriteBackFlushPendingTest {

  private Connection connection;
  private WriteBackRepositoryImpl<String, String> writeBack;

  @BeforeEach
  void setUp() throws Exception {
    Class.forName("org.sqlite.JDBC");
    Connection raw = DriverManager.getConnection("jdbc:sqlite::memory:");
    connection = NonClosableConnection.create(raw);
    try (Statement st = connection.createStatement()) {
      st.execute("CREATE TABLE kv (k TEXT PRIMARY KEY, v TEXT NOT NULL)");
    }
    ConnectionSource source = new FixedSource(connection);
    RelationalRepositoryImpl<String, String> relational =
        new RelationalRepositoryImpl<>(source, new KvContext());
    writeBack = new WriteBackRepositoryImpl<>(relational);
  }

  @AfterEach
  void tearDown() throws SQLException {
    if (connection instanceof NonClosableConnection nc) {
      nc.shutdown();
    }
  }

  @Test
  void flushPendingWritesStagedUpsertsToDatabase() throws Exception {
    writeBack.save("a", "1");
    writeBack.save("b", "2");

    // not yet on disk before flush (write-back only)
    assertNull(loadFromDb("a"));
    assertNull(loadFromDb("b"));

    writeBack.flushPending();

    assertEquals("1", loadFromDb("a"));
    assertEquals("2", loadFromDb("b"));
  }

  @Test
  void flushPendingAppliesDeletes() throws Exception {
    writeBack.save("x", "keep");
    writeBack.flushPending();
    assertEquals("keep", loadFromDb("x"));

    writeBack.delete("x");
    assertNull(writeBack.load("x"));
    // still on disk until flush
    assertEquals("keep", loadFromDb("x"));

    writeBack.flushPending();
    assertNull(loadFromDb("x"));
  }

  @Test
  void pluginResourcesFlushThenShutdownOrder() throws Exception {
    PluginResources resources = new PluginResources();
    ConnectionSource source = resources.track(new FixedSource(connection) {
      private boolean closed;

      @Override
      public void shutdown() {
        closed = true;
      }

      @Override
      public boolean isClosed() {
        return closed;
      }
    });
    writeBack.save("z", "9");
    resources.onFlush(writeBack::flushPending);

    resources.shutdown();

    assertTrue(source.isClosed());
    assertEquals("9", loadFromDb("z"));
  }

  private @Nullable String loadFromDb(String key) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement("SELECT v FROM kv WHERE k = ?")) {
      ps.setString(1, key);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return rs.getString(1);
      }
    }
  }

  private static final class KvContext implements RelationalRepositoryContext<String, String> {
    @Override
    public String getSelectQuery() {
      return "SELECT v FROM kv WHERE k = ?";
    }

    @Override
    public String getSaveQuery() {
      return "INSERT INTO kv (k, v) VALUES (?, ?) ON CONFLICT(k) DO UPDATE SET v = excluded.v";
    }

    @Override
    public String getDeleteQuery() {
      return "DELETE FROM kv WHERE k = ?";
    }

    @Override
    public void setKey(PreparedStatement ps, String key) throws SQLException {
      ps.setString(1, key);
    }

    @Override
    public void setSaveValues(PreparedStatement ps, String key, String value) throws SQLException {
      ps.setString(1, key);
      ps.setString(2, value);
    }

    @Override
    public String mapResult(ResultSet rs, String key) throws SQLException {
      return rs.getString("v");
    }
  }

  private static class FixedSource implements ConnectionSource {
    private final Connection connection;

    FixedSource(Connection connection) {
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
      return false;
    }

    @Override
    public DatabaseType getType() {
      return DatabaseType.SQLITE;
    }

    @Override
    public boolean isSetup() {
      return true;
    }
  }
}
