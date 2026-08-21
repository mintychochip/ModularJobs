package net.aincraft.protection;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.popcraft.bolt.BoltAPI;

/**
 * Manufacturing a {@link BlockProtectionAdapter} by probing the runtime for a
 * Bolt integration, returning {@code null} when none is available/enabled.
 */
public final class BlockProtectionAdapterProvider {

  /** Creates an adapter backed by an installed Bolt service, or null when absent. */
  public static BlockProtectionAdapter create() {
    return new BlockProtectionAdapterProvider().get();
  }

  /**
   * Returns a Bolt-backed adapter when the Bolt plugin is enabled and registered
   * as a services-manager provider; otherwise returns null.
   */
  BlockProtectionAdapter get() {
    Plugin boltPlugin = Bukkit.getPluginManager().getPlugin("Bolt");
    if (boltPlugin != null && boltPlugin.isEnabled()) {
      RegisteredServiceProvider<BoltAPI> registration = Bukkit.getServicesManager()
          .getRegistration(BoltAPI.class);
      if (registration != null) {
        BoltAPI bolt = registration.getProvider();
        return boltAdapter(bolt);
      }
    }
    return null;
  }

  /**
   * Builds a Bolt-backed {@link BlockProtectionAdapter} resolving the owner of
   * each block's protection, or empty when no protection exists.
   */
  public static BlockProtectionAdapter boltAdapter(BoltAPI bolt) {
    return block -> {
      org.popcraft.bolt.protection.Protection protection = bolt.findProtection(block);
      return Optional.ofNullable(protection.getOwner());
    };
  }
}
