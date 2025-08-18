package net.aincraft.protection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import net.aincraft.protection.BlockOwnershipService.BlockProtectionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;
import org.popcraft.bolt.BoltAPI;

public final class ProtectionModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(BlockOwnershipService.class).to(BlockOwnershipServiceImpl.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  @Nullable
  BlockProtectionAdapter blockProtectionAdapter() {
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
