package net.aincraft.internal;

import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.container.BoostSource;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.Store;
import net.aincraft.economy.VaultEconomyProviderImpl;
import net.aincraft.hooks.McMMOBoostSourceImpl;
import net.aincraft.service.BlockOwnershipService.BlockProtectionAdapter;
import net.aincraft.service.ownership.BoltBlockProtectionAdapterImpl;
import net.aincraft.service.ownership.LWCXBlockProtectionAdapterImpl;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.BoltAPI;

public class BridgeDependencyResolverImpl implements BridgeDependencyResolver {

  private final Plugin plugin;

  public BridgeDependencyResolverImpl(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public @NotNull Optional<EconomyProvider> getEconomyProvider() {
    PluginManager pluginManager = Bukkit.getPluginManager();
    ServicesManager servicesManager = Bukkit.getServicesManager();
    Plugin vault = pluginManager.getPlugin("Vault");
    if (vault != null && vault.isEnabled()) {
      RegisteredServiceProvider<Economy> registration = servicesManager.getRegistration(
          Economy.class);
      if (registration != null) {
        Economy provider = registration.getProvider();
        return Optional.of(new VaultEconomyProviderImpl(provider));
      }
    }
    return Optional.empty();
  }


  @Override
  public Optional<BlockProtectionAdapter> getBlockProtectionAdapter() {
    Plugin lwcPlugin = Bukkit.getPluginManager().getPlugin("LWC");
    if (lwcPlugin instanceof LWCPlugin lp && lp.isEnabled()) {
      LWC lwc = lp.getLWC();
      return Optional.of(new LWCXBlockProtectionAdapterImpl(lwc));
    }
    Plugin boltPlugin = Bukkit.getPluginManager().getPlugin("Bolt");
    if (boltPlugin != null && boltPlugin.isEnabled()) {
      RegisteredServiceProvider<BoltAPI> registration = Bukkit.getServicesManager()
          .getRegistration(BoltAPI.class);
      if (registration != null) {
        BoltAPI bolt = registration.getProvider();
        return Optional.of(new BoltBlockProtectionAdapterImpl(bolt));
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<BoostSource> getMcMMOBoostSource() {
    Plugin mcMMO = Bukkit.getPluginManager().getPlugin("McMMO");
    if (mcMMO != null && mcMMO.isEnabled()) {
      Map<SuperAbilityType,BigDecimal> amounts = new HashMap<>();
      amounts.put(SuperAbilityType.SUPER_BREAKER,BigDecimal.valueOf(10000));
      return Optional.of(McMMOBoostSourceImpl.create(plugin, amounts));
    }
    return Optional.empty();
  }
}
