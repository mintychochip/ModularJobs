package net.aincraft.protection;

import com.google.inject.Provider;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.popcraft.bolt.BoltAPI;

final class BlockProtectionAdapterProvider implements Provider<BlockProtectionAdapter> {

  @Override
  public BlockProtectionAdapter get() {
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

  static BlockProtectionAdapter boltAdapter(BoltAPI bolt) {
    return block -> {
      org.popcraft.bolt.protection.Protection protection = bolt.findProtection(block);
      return Optional.ofNullable(protection.getOwner());
    };
  }
}
