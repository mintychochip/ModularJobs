package net.aincraft.bridge;

import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.google.common.cache.CacheLoader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.aincraft.Jobs;
import net.aincraft.api.Bridge;
import net.aincraft.api.container.BoostSource;
import net.aincraft.api.container.ExpressionPayableCurveImpl;
import net.aincraft.api.container.PayableCurve;
import net.aincraft.api.container.PayableType;
import net.aincraft.api.container.PayableTypes;
import net.aincraft.api.container.Provider;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.service.BlockOwnershipService;
import net.aincraft.api.service.ChunkExplorationStore;
import net.aincraft.api.service.EntityValidationService;
import net.aincraft.api.service.ExploitProtectionStore;
import net.aincraft.api.service.ExploitService;
import net.aincraft.api.service.ExploitService.ExploitProtectionType;
import net.aincraft.api.service.JobTaskProvider;
import net.aincraft.api.service.MobDamageTracker;
import net.aincraft.api.service.ProgressionService;
import net.aincraft.container.JobImpl;
import net.aincraft.database.ConnectionSource;
import net.aincraft.economy.EconomyProvider;
import net.aincraft.hooks.McMMOBoostSourceImpl;
import net.aincraft.service.CSVJobTaskProviderImpl;
import net.aincraft.service.ExploitServiceImpl;
import net.aincraft.service.MemoryExploitProtectionStoreImpl;
import net.aincraft.service.MemoryMobDamageTrackerStoreImpl;
import net.aincraft.service.MetadataEntityValidationServiceImpl;
import net.aincraft.service.MobDamageTrackerImpl;
import net.aincraft.service.PersistentChunkExplorationStoreImpl;
import net.aincraft.service.ProgressionServiceImpl;
import net.aincraft.service.ownership.BlockOwnershipServiceImpl;
import net.aincraft.util.LocationKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public final class BridgeImpl implements Bridge {

  private final Jobs plugin;
  private final BridgeDependencyResolver dependencyResolver;
  private final ConnectionSource connectionSource;
  private final RegistryContainer registryContainer = new RegistryContainerImpl();
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
    blockOwnershipService = dependencyResolver.getBlockOwnershipProvider()
        .map(BlockOwnershipServiceImpl::new).orElse(null);
    exploitService = new ExploitServiceImpl(providers);
    progressionService = ProgressionServiceImpl.create(connectionSource);
    mobDamageTracker = MobDamageTrackerImpl.create(new MemoryMobDamageTrackerStoreImpl(), plugin);
    initializeRegistryContainer();
  }

  private void initializeRegistryContainer() {
    registryContainer.editRegistry(RegistryKeys.PAYABLE_TYPES, r -> {
      r.register(PayableType.create(BufferedExperienceHandlerImpl.create(plugin),
          Key.key("jobs:experience")));
      r.register(PayableType.create(context -> {
        economyProvider.deposit(context.getPlayer(), context.getPayable().getAmount());
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
  public Optional<EconomyProvider> economy() {
    return Optional.ofNullable(economyProvider);
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
