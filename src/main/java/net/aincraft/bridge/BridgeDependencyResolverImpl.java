package net.aincraft.bridge;

import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.api.container.BoostSource;
import net.aincraft.api.container.Provider;
import net.aincraft.api.container.Store;
import net.aincraft.economy.EconomyProvider;
import net.aincraft.economy.VaultEconomyProviderImpl;
import net.aincraft.hooks.McMMOBoostSourceImpl;
import net.aincraft.service.ownership.BoltBlockOwnershipProviderImpl;
import net.aincraft.service.ownership.LWCXBlockOwnershipProviderImpl;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
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
  public Optional<Provider<Block, UUID>> getBlockOwnershipProvider() {
    Plugin lwcPlugin = Bukkit.getPluginManager().getPlugin("LWC");
    if (lwcPlugin instanceof LWCPlugin lp && lp.isEnabled()) {
      LWC lwc = lp.getLWC();
      return Optional.of(new LWCXBlockOwnershipProviderImpl(lwc));
    }
    Plugin boltPlugin = Bukkit.getPluginManager().getPlugin("Bolt");
    if (boltPlugin != null && boltPlugin.isEnabled()) {
      RegisteredServiceProvider<BoltAPI> registration = Bukkit.getServicesManager()
          .getRegistration(BoltAPI.class);
      if (registration != null) {
        BoltAPI bolt = registration.getProvider();
        return Optional.of(new BoltBlockOwnershipProviderImpl(bolt));
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<BoostSource> getMcMMOBoostSource() {
    Plugin mcMMO = Bukkit.getPluginManager().getPlugin("McMMO");
    if (mcMMO != null && mcMMO.isEnabled()) {
      Store<UUID, SuperAbilityType> store = Store.memory();
      Provider<SuperAbilityType, BigDecimal> provider = new Provider<>() {
        private static final Map<SuperAbilityType, BigDecimal> amounts = new HashMap<>();

        static {
          amounts.put(SuperAbilityType.TREE_FELLER, BigDecimal.valueOf(1000));
        }

        @Override
        public @NotNull Optional<BigDecimal> get(SuperAbilityType key) {
          return Optional.ofNullable(amounts.get(key));
        }
      };
      return Optional.of(McMMOBoostSourceImpl.create(plugin, provider));
    }
    return Optional.empty();
  }
}
