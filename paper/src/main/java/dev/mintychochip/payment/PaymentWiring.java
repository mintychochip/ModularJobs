package dev.mintychochip.payment;

import com.google.common.cache.CacheLoader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.mintychochip.service.ItemBoostDataService;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.payment.ExploitService.ExploitProtectionType;
import dev.mintychochip.profession.RecipeExperienceDepreciationPolicy;
import dev.mintychochip.protection.BlockOwnershipService;
import dev.mintychochip.service.ExploitProtectionStore;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import dev.mintychochip.upgrade.UpgradeBoostDataService;
import dev.mintychochip.util.LocationKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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
  public final PaymentSettings paymentSettings;
  public final List<Listener> listeners;

  private PaymentWiring(
      BoostEngine boostEngine,
      JobsPaymentHandler paymentHandler,
      ExploitService exploitService,
      PaymentSettings paymentSettings,
      List<Listener> listeners) {
    this.boostEngine = boostEngine;
    this.paymentHandler = paymentHandler;
    this.exploitService = exploitService;
    this.paymentSettings = paymentSettings;
    this.listeners = listeners;
  }

  /**
   * Composes the payment object graph (settings, boost engine, damage tracking, exploit
   * protections, and all payment listeners) without optional recipe/profession gating.
   */
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
        null);
  }

  /**
   * Composes the payment object graph (settings, boost engine, damage tracking, exploit
   * protections, and all payment listeners). When both recipe and profession services are
   * supplied, a {@link CraftRecipeGateListener} is also wired in.
   */
  public static PaymentWiring create(
      Plugin plugin,
      JobService jobService,
      ItemBoostDataService itemBoostDataService,
      TimedBoostDataService timedBoostDataService,
      UpgradeBoostDataService upgradeBoostDataService,
      BlockOwnershipService blockOwnershipService,
      @Nullable RecipeService recipeService,
      @Nullable ProfessionService professionService) {
    PaymentSettings paymentSettings = PaymentSettings.fromPlugin(plugin);
    PaymentEligibility eligibility = new PaymentEligibility(paymentSettings);

    BoostEngine boostEngine = new BoostEngine(
        itemBoostDataService, timedBoostDataService, upgradeBoostDataService);
    PlayerChunkExplorationService chunkExploration = new PlayerChunkExplorationService();
    MobDamageTrackerStore damageStore = new MobDamageTrackerStore();
    EntityValidationService entityValidation = new EntityValidationService(plugin);
    MobDamageTracker mobDamageTracker = new MobDamageTracker(damageStore);
    RecipeExperienceDepreciationApplier recipeDepreciation = null;
    if (recipeService != null && professionService != null) {
      RecipeExperienceDepreciationPolicy policy = RecipeDepreciationSettings.fromPlugin(plugin);
      if (policy.enabled()) {
        recipeDepreciation =
            new RecipeExperienceDepreciationApplier(policy, recipeService, professionService);
      }
    }
    JobsPaymentHandler paymentHandler =
        new JobsPaymentHandler(plugin, boostEngine, jobService, recipeDepreciation);
    ExploitService exploitService = createExploitService(plugin);
    HopperPayDisableStore hopperStore = new HopperPayDisableStore();

    List<Listener> listeners = new ArrayList<>();
    listeners.add(new MobDamageTrackerController(damageStore));
    listeners.add(new JobPaymentListener(
        blockOwnershipService,
        mobDamageTracker,
        paymentHandler,
        entityValidation,
        exploitService,
        chunkExploration,
        eligibility,
        hopperStore));
    listeners.add(new MobTagController(entityValidation));
    listeners.add(new ExploitStoreController(exploitService));
    listeners.add(new PistonProtectionListener(exploitService));
    listeners.add(new OreGeneratorProtectionListener(exploitService));
    listeners.add(new HopperPayListener(hopperStore, exploitService));
    listeners.add(new JobLevelUpListener());
    if (recipeService != null && professionService != null) {
      listeners.add(new CraftRecipeGateListener(recipeService, professionService));
    }

    return new PaymentWiring(
        boostEngine, paymentHandler, exploitService, paymentSettings, List.copyOf(listeners));
  }

  static ExploitService createExploitService(Plugin plugin) {
    Map<Material, Duration> placedMaterials = PlacedProtectionMaterials.load(plugin);
    ExploitProtectionSettings settings = ExploitProtectionSettings.load(plugin);
    return createExploitService(placedMaterials, settings);
  }

  /**
   * Package-visible for tests: build exploit service with an explicit placed material map.
   */
  static ExploitService createExploitService(Map<Material, Duration> placedMaterials) {
    return createExploitService(placedMaterials, ExploitProtectionSettings.defaults());
  }

  static ExploitService createExploitService(
      Map<Material, Duration> placedMaterials, ExploitProtectionSettings settings) {
    Map<Key, ExploitProtectionStore<?>> providers = new HashMap<>();
    providers.put(ExploitProtectionType.WAX.key(),
        new MemoryExploitProtectionStoreImpl<>(
            toTemporal(settings.waxMaterials()),
            Block::getType,
            CacheLoader.from(
                block -> new LocationKey(block.getWorld().getName(), block.getX(), block.getY(),
                    block.getZ()))));
    Map<Material, java.time.temporal.TemporalAmount> placedTemporal = new EnumMap<>(placedMaterials);
    providers.put(ExploitProtectionType.PLACED.key(),
        new MemoryExploitProtectionStoreImpl<Block, Material>(
            placedTemporal,
            Block::getType,
            CacheLoader.from(
                (Block block) -> new LocationKey(block.getWorld().getName(), block.getX(),
                    block.getY(), block.getZ()))));
    providers.put(ExploitProtectionType.DYE_ENTITY.key(),
        new MemoryExploitProtectionStoreImpl<>(
            toTemporal(settings.dyeEntities()),
            Entity::getType,
            CacheLoader.from(Entity::getUniqueId)));
    providers.put(ExploitProtectionType.MILK.key(),
        new MemoryExploitProtectionStoreImpl<>(
            toTemporal(settings.milkEntities()),
            Entity::getType,
            CacheLoader.from(Entity::getUniqueId)));
    providers.put(ExploitProtectionType.STRIP.key(),
        new MemoryExploitProtectionStoreImpl<>(
            toTemporal(settings.stripMaterials()),
            Block::getType,
            CacheLoader.from(
                block -> new LocationKey(block.getWorld().getName(), block.getX(), block.getY(),
                    block.getZ()))));
    return new ExploitService(providers, settings);
  }

  private static <K> Map<K, java.time.temporal.TemporalAmount> toTemporal(Map<K, Duration> source) {
    return new HashMap<>(source);
  }
}
