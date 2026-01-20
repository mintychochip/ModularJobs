package net.aincraft.protection;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.popcraft.bolt.BoltAPI;

/**
 * Bolt-specific protection adapter. This class is loaded dynamically only when
 * Bolt is present to avoid ClassNotFoundException on servers without Bolt.
 */
final class BoltProtectionAdapter implements BlockProtectionAdapter {

  private final BoltAPI bolt;

  private BoltProtectionAdapter(BoltAPI bolt) {
    this.bolt = bolt;
  }

  /**
   * Create the adapter if Bolt is available.
   * @return adapter instance or null if Bolt API not available
   */
  static BlockProtectionAdapter create() {
    RegisteredServiceProvider<BoltAPI> registration = Bukkit.getServicesManager()
        .getRegistration(BoltAPI.class);
    if (registration != null) {
      return new BoltProtectionAdapter(registration.getProvider());
    }
    return null;
  }

  @Override
  public Optional<UUID> getOwner(Block block) {
    org.popcraft.bolt.protection.Protection protection = bolt.findProtection(block);
    return Optional.ofNullable(protection.getOwner());
  }
}
