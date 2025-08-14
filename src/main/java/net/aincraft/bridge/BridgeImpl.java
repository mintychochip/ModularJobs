package net.aincraft.bridge;

import com.google.common.cache.CacheLoader;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.aincraft.Jobs;
import net.aincraft.api.Bridge;
import net.aincraft.api.container.PayableType;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.service.BlockOwnershipProvider;
import net.aincraft.api.service.ChunkExplorationStore;
import net.aincraft.api.service.EntityValidationService;
import net.aincraft.api.service.ExploitService;
import net.aincraft.api.service.ExploitService.ExploitProtectionType;
import net.aincraft.api.service.JobTaskProvider;
import net.aincraft.api.service.MobDamageTracker;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.api.service.ExploitProtectionStore;
import net.aincraft.database.ConnectionSource;
import net.aincraft.economy.Economy;
import net.aincraft.economy.VaultEconomy;
import net.aincraft.service.CSVJobTaskProviderImpl;
import net.aincraft.service.ExploitServiceImpl;
import net.aincraft.service.MemoryMobDamageTrackerStoreImpl;
import net.aincraft.service.MemoryExploitProtectionStoreImpl;
import net.aincraft.service.MetadataEntityValidationServiceImpl;
import net.aincraft.service.MobDamageTrackerImpl;
import net.aincraft.service.PersistentChunkExplorationStoreImpl;
import net.aincraft.service.ProgressionServiceImpl;
import net.aincraft.service.ownership.BoltBlockOwnershipProviderImpl;
import net.aincraft.service.ownership.LWCXBlockOwnershipProviderImpl;
import net.aincraft.util.LocationKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.popcraft.bolt.BoltAPI;

public final class BridgeImpl implements Bridge {

  public static final ExecutorService ACTOR = Executors.newSingleThreadExecutor();
  private final Jobs plugin;
  private final ConnectionSource connectionSource;
  private final RegistryContainer registryContainer = new RegistryContainerImpl();
  private final ProgressionService progressionService;
  private final KeyResolver keyResolver = new KeyResolverImpl();
  private final EntityValidationService entityValidationService;
  private final ExploitService exploitService;
  private final MobDamageTracker mobDamageTracker;
  private final BlockOwnershipProvider blockOwnershipProvider;
  private final ChunkExplorationStore chunkExplorationStore = new PersistentChunkExplorationStoreImpl();
  private final JobTaskProvider jobTaskProvider;

  public BridgeImpl(Jobs plugin, ConnectionSource connectionSource) {
    this.plugin = plugin;
    this.connectionSource = connectionSource;
    entityValidationService = new MetadataEntityValidationServiceImpl(plugin);
    Map<Key, ExploitProtectionStore<?>> providers = new HashMap<>();
    providers.put(ExploitProtectionType.WAX.key(),
        new MemoryExploitProtectionStoreImpl<>(
            Map.of(Material.COPPER_BLOCK, Duration.ofSeconds(5)), Block::getType,
            CacheLoader.from(
                block -> new LocationKey(block.getWorld().getName(), block.getX(), block.getY(),
                    block.getZ()))));
    providers.put(ExploitProtectionType.PLACED.key(),
        new MemoryExploitProtectionStoreImpl<>(Map.of(Material.STONE, Duration.ofSeconds(60)),
            Block::getType,
            CacheLoader.from(
                block -> new LocationKey(block.getWorld().getName(), block.getX(), block.getY(),
                    block.getZ()))));
    providers.put(ExploitProtectionType.DYE_ENTITY.key(),
        new MemoryExploitProtectionStoreImpl<>(
            Map.of(EntityType.WOLF, Duration.ofMinutes(5), EntityType.SHEEP, Duration.ofSeconds(5)),
            Entity::getType,
            CacheLoader.from(Entity::getUniqueId)));
    providers.put(ExploitProtectionType.MILK.key(),
        new MemoryExploitProtectionStoreImpl<>(
            Map.of(EntityType.COW, Duration.ofSeconds(5), EntityType.GOAT, Duration.ofSeconds(5)),
            Entity::getType,
            CacheLoader.from(Entity::getUniqueId)));
    try {
      jobTaskProvider = CSVJobTaskProviderImpl.create(plugin);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    registryContainer.editRegistry(RegistryKeys.PAYABLE_TYPES,r -> {
      r.register(PayableType.create(
          BufferedExperienceHandlerImpl.create(plugin,ACTOR),Key.key("jobs:experience")));
    });

    exploitService = new ExploitServiceImpl(providers);
    progressionService = new ProgressionServiceImpl(connectionSource);
    mobDamageTracker = new MobDamageTrackerImpl(new MemoryMobDamageTrackerStoreImpl(), plugin);
    Plugin p = Bukkit.getPluginManager().getPlugin("LWC");
    Plugin bolt = Bukkit.getPluginManager().getPlugin("Bolt");
    if (p != null && p.isEnabled() && p instanceof LWCPlugin lp) {
      LWC lwc = lp.getLWC();
      blockOwnershipProvider = new LWCXBlockOwnershipProviderImpl(lwc);
    } else if (bolt != null && bolt.isEnabled()) {
      RegisteredServiceProvider<BoltAPI> registration = Bukkit.getServicesManager()
          .getRegistration(BoltAPI.class);
      BoltAPI boltAPI = registration.getProvider();
      blockOwnershipProvider = new BoltBlockOwnershipProviderImpl(boltAPI);
    } else {
      blockOwnershipProvider = null;
    }
  }

  @Override
  public Jobs plugin() {
    return plugin;
  }

  @Override
  public KeyResolver resolver() {
    return keyResolver;
  }

  @Override
  public ProgressionService progressionService() {
    return progressionService;
  }

  @Override
  public RegistryContainer registryContainer() {
    return registryContainer;
  }

  @Override
  public EntityValidationService spawnerService() {
    return entityValidationService;
  }

  @Override
  public Economy economy() {
    Plugin vault = Bukkit.getServer().getPluginManager().getPlugin("Vault");
    if (vault != null) {
      RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> registration = Bukkit.getServicesManager()
          .getRegistration(net.milkbowl.vault.economy.Economy.class);
      net.milkbowl.vault.economy.Economy provider = registration.getProvider();
      return new VaultEconomy(provider);
    }
    return null;
  }

  @Override
  public JobTaskProvider jobTaskProvider() {
    return jobTaskProvider;
  }

  @Override
  public ExploitService exploitService() {
    return exploitService;
  }

  @Override
  public BlockOwnershipProvider blockOwnershipProvider() {
    return blockOwnershipProvider;
  }

  @Override
  public MobDamageTracker mobDamageTracker() {
    return mobDamageTracker;
  }

  @Override
  public ChunkExplorationStore chunkExplorationStore() {
    return chunkExplorationStore;
  }
}
