package net.aincraft.protection;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.popcraft.bolt.BoltAPI;

public final class BlockProtectionAdapterProvider {

  public static BlockProtectionAdapter create() {
    return new BlockProtectionAdapterProvider().get();
  }

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

  public static BlockProtectionAdapter boltAdapter(BoltAPI bolt) {
    return block -> {
      org.popcraft.bolt.protection.Protection protection = bolt.findProtection(block);
      return Optional.ofNullable(protection.getOwner());
    };
  }
}
