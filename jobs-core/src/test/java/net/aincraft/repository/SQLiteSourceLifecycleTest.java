package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives the real {@link SQLiteSourceImpl} disable path used by bootstrap.
 * Before the fix, isClosed/shutdown NPEd when no long-lived connection field was set.
 */
class SQLiteSourceLifecycleTest {

  @TempDir
  Path tempDir;

  @Test
  void isClosedAndShutdownSafeWithoutPriorGetConnection() throws Exception {
    Path db = tempDir.resolve("never-used.db");
    ConnectionSource source = SQLiteSourceImpl.createAtPath(db);

    assertFalse(source.isClosed(), "fresh source must report open");
    assertDoesNotThrow(source::shutdown);
    assertTrue(source.isClosed(), "after shutdown must report closed");
    // idempotent
    assertDoesNotThrow(source::shutdown);
    assertTrue(source.isClosed());
  }

  @Test
  void isClosedAndShutdownSafeAfterGetConnectionUsage() throws Exception {
    Path db = tempDir.resolve("used.db");
    ConnectionSource source = SQLiteSourceImpl.createAtPath(db);

    try (Connection conn = source.getConnection();
        Statement st = conn.createStatement()) {
      st.execute("SELECT 1");
    }

    assertFalse(source.isClosed());
    assertDoesNotThrow(() -> {
      // matches ModularJobsBootstrap / PluginResources disable sequence
      if (!source.isClosed()) {
        source.shutdown();
      }
    });
    assertTrue(source.isClosed());
  }

  @Test
  void getConnectionRejectedAfterShutdown() throws Exception {
    Path db = tempDir.resolve("closed.db");
    ConnectionSource source = SQLiteSourceImpl.createAtPath(db);
    source.shutdown();
    assertThrows(IllegalStateException.class, source::getConnection);
  }

  @Test
  void createAtPathCreatesFile() throws Exception {
    Path db = tempDir.resolve("nested/dir/jobs.db");
    ConnectionSource source = SQLiteSourceImpl.createAtPath(db);
    assertTrue(Files.isRegularFile(db));
    try (Connection conn = source.getConnection()) {
      assertFalse(conn.isClosed());
    }
    source.shutdown();
  }
}
