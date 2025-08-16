package net.aincraft.internal;

import com.google.common.cache.CacheLoader;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.aincraft.Bridge;
import net.aincraft.Jobs;
import net.aincraft.boost.BoostCodecLoaderImpl;
import net.aincraft.boost.BoostFactoryImpl;
import net.aincraft.boost.conditions.ConditionCodecLoaderImpl;
import net.aincraft.boost.conditions.ConditionFactoryImpl;
import net.aincraft.boost.policies.PolicyFactoryImpl;
import net.aincraft.container.EconomyProvider;
import net.aincraft.container.KeyResolver;
import net.aincraft.container.PayableType;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.aincraft.database.ConnectionSource;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
import net.aincraft.service.BlockOwnershipService;
import net.aincraft.service.CSVJobTaskProviderImpl;
import net.aincraft.service.ChunkExplorationStore;
import net.aincraft.service.EntityValidationService;
import net.aincraft.service.ExploitProtectionStore;
import net.aincraft.service.ExploitService;
import net.aincraft.service.ExploitService.ExploitProtectionType;
import net.aincraft.service.ExploitServiceImpl;
import net.aincraft.service.JobTaskProvider;
import net.aincraft.service.MemoryExploitProtectionStoreImpl;
import net.aincraft.service.MemoryMobDamageTrackerStoreImpl;
import net.aincraft.service.MetadataEntityValidationServiceImpl;
import net.aincraft.service.MobDamageTracker;
import net.aincraft.service.MobDamageTrackerImpl;
import net.aincraft.service.PersistentChunkExplorationStoreImpl;
import net.aincraft.service.ProgressionService;
import net.aincraft.service.ProgressionServiceImpl;
import net.aincraft.service.ownership.BlockOwnershipServiceImpl;
import net.aincraft.util.LocationKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public final class BridgeImpl implements Bridge {

  private final Jobs plugin;
  private final BridgeDependencyResolver dependencyResolver;
  private final ConnectionSource connectionSource;
  private final RegistryContainer registryContainer;
  private final ProgressionService progressionService;
  private final KeyResolver keyResolver = new KeyResolverImpl();
  private final EntityValidationService entityValidationService;
  private final ExploitService exploitService;
  private final MobDamageTracker mobDamageTracker;
  private final EconomyProvider economyProvider;
  private final BlockOwnershipService blockOwnershipService;
  private final ChunkExplorationStore chunkExplorationStore = new PersistentChunkExplorationStoreImpl();
  private final JobTaskProvider jobTaskProvider;

  public BridgeImpl(Jobs plugin, ConnectionSource connectionSource) {
    this.plugin = plugin;
    dependencyResolver = new BridgeDependencyResolverImpl(plugin);
    economyProvider = dependencyResolver.getEconomyProvider().orElse(null);
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
    blockOwnershipService = dependencyResolver.getBlockProtectionAdapter()
        .map(BlockOwnershipServiceImpl::new).orElse(null);
    exploitService = new ExploitServiceImpl(providers);
    progressionService = ProgressionServiceImpl.create(connectionSource);
    mobDamageTracker = MobDamageTrackerImpl.create(new MemoryMobDamageTrackerStoreImpl(), plugin);

    registryContainer = RegistryContainerImpl.create();
    initializeRegistryContainer();
  }

  private void initializeRegistryContainer() {
    registryContainer.editRegistry(RegistryKeys.CODEC, ConditionCodecLoaderImpl.INSTANCE::load);
    registryContainer.editRegistry(RegistryKeys.CODEC, BoostCodecLoaderImpl.INSTANCE::load);
    registryContainer.editRegistry(RegistryKeys.PAYABLE_TYPES, r -> {
      r.register(PayableType.create(BufferedExperienceHandlerImpl.create(plugin),
          Key.key("jobs:experience")));
      r.register(PayableType.create(context -> {
        economyProvider.deposit(context.getPlayer(), context.getPayable().amount());
      }, Key.key("jobs:economy")));
    });
    registryContainer.editRegistry(RegistryKeys.ACTION_TYPES, r -> {
      r.register(() -> Key.key("jobs:block_place"));
      r.register(() -> Key.key("jobs:block_break"));
      r.register(() -> Key.key("jobs:tnt_break"));
      r.register(() -> Key.key("jobs:kill"));
      r.register(() -> Key.key("jobs:dye"));
      r.register(() -> Key.key("jobs:strip_log"));
      r.register(() -> Key.key("jobs:craft"));
      r.register(() -> Key.key("jobs:fish"));
      r.register(() -> Key.key("jobs:smelt"));
      r.register(() -> Key.key("jobs:brew"));
      r.register(() -> Key.key("jobs:enchant"));
      r.register(() -> Key.key("jobs:repair"));
      r.register(() -> Key.key("jobs:breed"));
      r.register(() -> Key.key("jobs:tame"));
      r.register(() -> Key.key("jobs:shear"));
      r.register(() -> Key.key("jobs:milk"));
      r.register(() -> Key.key("jobs:explore"));
      r.register(() -> Key.key("jobs:eat"));
      r.register(() -> Key.key("jobs:collect"));
      r.register(() -> Key.key("jobs:bake"));
      r.register(() -> Key.key("jobs:bucket"));
      r.register(() -> Key.key("jobs:brush"));
      r.register(() -> Key.key("jobs:wax"));
      r.register(() -> Key.key("jobs:villager_trade"));
    });
    dependencyResolver.getMcMMOBoostSource().ifPresent(source -> {
      registryContainer.editRegistry(RegistryKeys.TRANSIENT_BOOST_SOURCES, r -> {
        r.register(source);
      });
    });
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
  public Optional<net.aincraft.container.EconomyProvider> economy() {
    return Optional.ofNullable(economyProvider);
  }

  @Override
  public JobTaskProvider jobTaskProvider() {
    return jobTaskProvider;
  }

  @Override
  public ConditionFactory conditionFactory() {
    return ConditionFactoryImpl.INSTANCE;
  }

  @Override
  public BoostFactory boostFactory() {
    return BoostFactoryImpl.INSTANCE;
  }

  @Override
  public PolicyFactory policyFactory() {
    return PolicyFactoryImpl.INSTANCE;
  }

  @Override
  public ExploitService exploitService() {
    return exploitService;
  }

  @Override
  public MobDamageTracker mobDamageTracker() {
    return mobDamageTracker;
  }

  @Override
  public ChunkExplorationStore chunkExplorationStore() {
    return chunkExplorationStore;
  }

  @Override
  public Optional<BlockOwnershipService> blockOwnershipService() {
    return Optional.ofNullable(blockOwnershipService);
  }
}
