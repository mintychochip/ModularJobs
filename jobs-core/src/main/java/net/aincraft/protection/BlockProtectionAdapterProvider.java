package net.aincraft.protection;

import com.google.inject.Provider;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.popcraft.bolt.BoltAPI;

final class BlockProtectionAdapterProvider implements Provider<BlockProtectionAdapter> {

  @Override
  public BlockProtectionAdapter get() {
    Plugin lwcPlugin = Bukkit.getPluginManager().getPlugin("LWC");
    if (lwcPlugin instanceof LWCPlugin lp && lp.isEnabled()) {
      LWC lwc = lp.getLWC();
      return new LWCXBlockProtectionAdapterImpl(lwc);
    }
    Plugin boltPlugin = Bukkit.getPluginManager().getPlugin("Bolt");
    if (boltPlugin != null && boltPlugin.isEnabled()) {
      RegisteredServiceProvider<BoltAPI> registration = Bukkit.getServicesManager()
          .getRegistration(BoltAPI.class);
      if (registration != null) {
        BoltAPI bolt = registration.getProvider();
        return new BoltBlockProtectionAdapterImpl(bolt);
      }
    }
    return null;
  }
}
