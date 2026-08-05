package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises the shipped {@link PluginResources} path used by PluginContext shutdown
 * and failed-composition cleanup — not a reimplementation.
 */
class PluginResourcesLifecycleTest {

  @TempDir
  Path tempDir;

  @Test
  void multiSourceShutdownClosesEveryTrackedSource() throws Exception {
    PluginResources resources = new PluginResources();
    ConnectionSource payable = resources.track(
        SQLiteSourceImpl.createAtPath(tempDir.resolve("payable.db")));
    ConnectionSource timed = resources.track(
        SQLiteSourceImpl.createAtPath(tempDir.resolve("timed.db")));
    ConnectionSource upgrades = resources.track(
        SQLiteSourceImpl.createAtPath(tempDir.resolve("upgrades.db")));

    assertEquals(3, resources.sourceCount());
    assertFalse(payable.isClosed());
    assertFalse(timed.isClosed());
    assertFalse(upgrades.isClosed());

    // production disable sequence
    resources.shutdown();

    assertTrue(payable.isClosed());
    assertTrue(timed.isClosed());
    assertTrue(upgrades.isClosed());
    for (ConnectionSource s : resources.connectionSources()) {
      assertTrue(s.isClosed());
    }
  }

  @Test
  void flushHooksRunBeforeSourcesClose() throws Exception {
    PluginResources resources = new PluginResources();
    List<String> order = new ArrayList<>();
    TrackingSource source = resources.track(new TrackingSource(order));
    resources.onFlush(() -> order.add("flush-a"));
    resources.onFlush(() -> order.add("flush-b"));

    resources.shutdown();

    assertEquals(List.of("flush-a", "flush-b", "shutdown"), order);
    assertTrue(source.isClosed());
  }

  @Test
  void closeQuietlyClosesSourcesOpenedBeforeSimulatedCompositionFailure() throws Exception {
    PluginResources resources = new PluginResources();
    ConnectionSource first = resources.track(
        SQLiteSourceImpl.createAtPath(tempDir.resolve("first.db")));
    ConnectionSource second = resources.track(
        SQLiteSourceImpl.createAtPath(tempDir.resolve("second.db")));

    // Simulate mid-create failure cleanup (PluginContext.create catch path)
    assertThrows(IllegalStateException.class, () -> {
      try {
        throw new IllegalStateException("simulated composition failure after opening sources");
      } catch (Throwable t) {
        resources.closeQuietly();
        throw t;
      }
    });

    assertTrue(first.isClosed(), "first source must close on composition failure");
    assertTrue(second.isClosed(), "second source must close on composition failure");
  }

  @Test
  void shutdownIsIdempotent() throws Exception {
    PluginResources resources = new PluginResources();
    AtomicInteger flushes = new AtomicInteger();
    ConnectionSource source = resources.track(
        SQLiteSourceImpl.createAtPath(tempDir.resolve("once.db")));
    resources.onFlush(flushes::incrementAndGet);

    resources.shutdown();
    resources.shutdown();

    assertEquals(1, flushes.get(), "flush hooks run once");
    assertTrue(source.isClosed());
  }

  /** Minimal ConnectionSource that records shutdown order. */
  private static final class TrackingSource implements ConnectionSource {
    private final List<String> order;
    private boolean closed;

    TrackingSource(List<String> order) {
      this.order = order;
    }

    @Override
    public void shutdown() {
      order.add("shutdown");
      closed = true;
    }

    @Override
    public DatabaseType getType() {
      return DatabaseType.SQLITE;
    }

    @Override
    public boolean isClosed() {
      return closed;
    }

    @Override
    public java.sql.Connection getConnection() throws SQLException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSetup() {
      return true;
    }
  }
}
