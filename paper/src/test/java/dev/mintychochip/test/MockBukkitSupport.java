package dev.mintychochip.test;

import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

/**
 * Shared MockBukkit lifecycle helpers for unit tests that need Bukkit runtime types ({@link
 * OfflinePlayer}, live {@link org.bukkit.Material}, etc.).
 *
 * <p>Pure unit tests that never touch Bukkit should not call these.
 */
public final class MockBukkitSupport {

  private MockBukkitSupport() {}

  /**
   * Starts a MockBukkit {@link ServerMock} if none is active.
   *
   * @return the active mock server
   */
  public static ServerMock mockServer() {
    if (MockBukkit.isMocked()) {
      return MockBukkit.getMock();
    }
    return MockBukkit.mock();
  }

  /** Tears down the MockBukkit server if one is active. */
  public static void unmockServer() {
    if (MockBukkit.isMocked()) {
      MockBukkit.unmock();
    }
  }

  /** Offline player backed by MockBukkit's player list (not a hand-rolled proxy). */
  public static OfflinePlayer offlinePlayer(UUID id) {
    return mockServer().getOfflinePlayer(id);
  }
}
