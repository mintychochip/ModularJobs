package net.aincraft.payment;

import com.google.common.cache.CacheLoader;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.payment.ExploitService.ExploitProtectionType;
import net.aincraft.profession.TierAntiFarmEngine;
import net.aincraft.protection.BlockOwnershipService;
import net.aincraft.service.ExploitProtectionStore;
import net.aincraft.service.JobService;
import net.aincraft.service.ProfessionService;
import net.aincraft.service.RecipeService;
import net.aincraft.upgrade.UpgradeBoostDataService;
import net.aincraft.util.KeyResolver;
import net.aincraft.util.LocationKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * Manual composition for payment / exploit / damage tracking (replaces Guice PaymentModule).
 */
public final class PaymentWiring {

  public final BoostEngine boostEngine;
  public final JobsPaymentHandler paymentHandler;
  public final ExploitService exploitService;
  public final List<Listener> listeners;

  private PaymentWiring(
      BoostEngine boostEngine,
      JobsPaymentHandler paymentHandler,
      ExploitService exploitService,
      List<Listener> listeners) {
    this.boostEngine = boostEngine;
    this.paymentHandler = paymentHandler;
    this.exploitService = exploitService;
    this.listeners = listeners;
  }

  public static PaymentWiring create(
      Plugin plugin,
      JobService jobService,
      ItemBoostDataService itemBoostDataService,
      TimedBoostDataService timedBoostDataService,
      UpgradeBoostDataService upgradeBoostDataService,
      BlockOwnershipService blockOwnershipService) {
    return create(
        plugin,
        jobService,
        itemBoostDataService,
        timedBoostDataService,
        upgradeBoostDataService,
        blockOwnershipService,
        null,
        null,
        null,
        null);
  }

  public static PaymentWiring create(
      Plugin plugin,
      JobService jobService,
      ItemBoostDataService itemBoostDataService,
      TimedBoostDataService timedBoostDataService,
      UpgradeBoostDataService upgradeBoostDataService,
      BlockOwnershipService blockOwnershipService,
      @Nullable TierAntiFarmEngine antiFarmEngine,
      @Nullable KeyResolver keyResolver,
      @Nullable RecipeService recipeService,
      @Nullable ProfessionService professionService) {
    BoostEngine boostEngine = new BoostEngine(
        itemBoostDataService, timedBoostDataService, upgradeBoostDataService);
    PlayerChunkExplorationService chunkExploration = new PlayerChunkExplorationService();
    MobDamageTrackerStore damageStore = new MobDamageTrackerStore();
    EntityValidationService entityValidation = new EntityValidationService(plugin);
    MobDamageTracker mobDamageTracker = new MobDamageTracker(damageStore);
    JobsPaymentHandler paymentHandler = new JobsPaymentHandler(
        plugin, boostEngine, jobService, antiFarmEngine, keyResolver);
    ExploitService exploitService = createExploitService();

    java.util.ArrayList<Listener> listeners = new java.util.ArrayList<>();
    listeners.add(new MobDamageTrackerController(damageStore));
    listeners.add(new JobPaymentListener(
        blockOwnershipService,
        mobDamageTracker,
        paymentHandler,
        entityValidation,
        exploitService,
        chunkExploration));
    listeners.add(new MobTagController(entityValidation));
    listeners.add(new ExploitStoreController(exploitService));
    listeners.add(new JobLevelUpListener());
    if (recipeService != null && professionService != null) {
      listeners.add(new CraftRecipeGateListener(recipeService, professionService));
    }

    return new PaymentWiring(boostEngine, paymentHandler, exploitService, List.copyOf(listeners));
  }

  private static ExploitService createExploitService() {
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
    providers.put(ExploitProtectionType.STRIP.key(),
        new MemoryExploitProtectionStoreImpl<>(
            Map.of(Material.OAK_LOG, Duration.ofSeconds(5),
                Material.SPRUCE_LOG, Duration.ofSeconds(5),
                Material.BIRCH_LOG, Duration.ofSeconds(5),
                Material.JUNGLE_LOG, Duration.ofSeconds(5),
                Material.ACACIA_LOG, Duration.ofSeconds(5),
                Material.DARK_OAK_LOG, Duration.ofSeconds(5),
                Material.MANGROVE_LOG, Duration.ofSeconds(5),
                Material.CHERRY_LOG, Duration.ofSeconds(5),
                Material.PALE_OAK_LOG, Duration.ofSeconds(5)),
            Block::getType,
            CacheLoader.from(
                block -> new LocationKey(block.getWorld().getName(), block.getX(), block.getY(),
                    block.getZ()))));
    return new ExploitService(providers);
  }
}
