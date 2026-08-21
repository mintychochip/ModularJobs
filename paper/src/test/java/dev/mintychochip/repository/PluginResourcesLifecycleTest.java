package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Exercises the shipped {@link PluginResources} path used by PluginContext shutdown
 * and failed-composition cleanup — not a reimplementation.
 */
class PluginResourcesLifecycleTest {

  @Test
  void multiSourceShutdownClosesEveryTrackedSource() throws Exception {
    PluginResources resources = new PluginResources();
    ConnectionSource payable = resources.track(new TrackingSource(new ArrayList<>()));
    ConnectionSource timed = resources.track(new TrackingSource(new ArrayList<>()));
    final ConnectionSource upgrades = resources.track(new TrackingSource(new ArrayList<>()));

    assertEquals(3, resources.sourceCount());
    assertFalse(payable.isClosed());
    assertFalse(timed.isClosed());
    assertFalse(upgrades.isClosed());

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
    final TrackingSource source = resources.track(new TrackingSource(order));
    resources.onFlush(() -> order.add("flush-a"));
    resources.onFlush(() -> order.add("flush-b"));

    resources.shutdown();

    assertEquals(List.of("flush-a", "flush-b", "shutdown"), order);
    assertTrue(source.isClosed());
  }

  @Test
  void closeQuietlyClosesSourcesOpenedBeforeSimulatedCompositionFailure() throws Exception {
    PluginResources resources = new PluginResources();
    ConnectionSource first = resources.track(new TrackingSource(new ArrayList<>()));
    ConnectionSource second = resources.track(new TrackingSource(new ArrayList<>()));

    assertThrows(IllegalStateException.class, () -> {
      try {
        throw new IllegalStateException("simulated composition failure after opening sources");
      } catch (IllegalStateException failure) {
        resources.closeQuietly();
        throw failure;
      }
    });

    assertTrue(first.isClosed(), "first source must close on composition failure");
    assertTrue(second.isClosed(), "second source must close on composition failure");
  }

  @Test
  void shutdownIsIdempotent() throws Exception {
    PluginResources resources = new PluginResources();
    AtomicInteger flushes = new AtomicInteger();
    final ConnectionSource source = resources.track(new TrackingSource(new ArrayList<>()));
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
      return DatabaseType.MYSQL;
    }

    @Override
    public boolean isClosed() {
      return closed;
    }

    @Override
    public Connection getConnection() throws SQLException {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSetup() {
      return true;
    }
  }
}
