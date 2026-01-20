package net.aincraft.protection;

import com.google.inject.Provider;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Provides a BlockProtectionAdapter based on available protection plugins.
 * Uses reflection to load Bolt adapter to avoid ClassNotFoundException when Bolt isn't installed.
 */
final class BlockProtectionAdapterProvider implements Provider<BlockProtectionAdapter> {

  @Override
  public BlockProtectionAdapter get() {
    Plugin boltPlugin = Bukkit.getPluginManager().getPlugin("Bolt");
    if (boltPlugin != null && boltPlugin.isEnabled()) {
      return loadBoltAdapter();
    }
    return null;
  }

  /**
   * Load the Bolt adapter via reflection to avoid loading BoltAPI class when Bolt isn't present.
   */
  private BlockProtectionAdapter loadBoltAdapter() {
    try {
      Class<?> adapterClass = Class.forName("net.aincraft.protection.BoltProtectionAdapter");
      Method createMethod = adapterClass.getDeclaredMethod("create");
      createMethod.setAccessible(true);
      return (BlockProtectionAdapter) createMethod.invoke(null);
    } catch (Exception e) {
      Bukkit.getLogger().warning("[ModularJobs] Failed to load Bolt integration: " + e.getMessage());
      return null;
    }
  }
}
