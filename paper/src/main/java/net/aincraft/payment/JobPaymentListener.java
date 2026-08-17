package net.aincraft.payment;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.papermc.paper.event.entity.EntityDyeEvent;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.aincraft.PluginProvider;
import net.aincraft.container.ActionTypes;
import net.aincraft.paper.BukkitContexts;
import net.aincraft.payment.ExploitService.ExploitProtectionType;
import net.aincraft.protection.BlockOwnershipService;
import net.aincraft.payment.DamageContribution;
import net.aincraft.util.LocationKey;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.damage.DamageSource;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Goat;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central Bukkit listener that turns gameplay events into job payments.
 * <p>
 * Every handler applies {@link PaymentEligibility} to gate pay, and most also apply exploit/
 * protection checks (placed-block timers, milk/wax/dye/strip cooldowns, silk-touch, generator
 * re-arm, hopper-disable, and furnace distance). Kill rewards consult {@link MobDamageTracker}
 * and {@link KillContributionPayout} so every qualifying damage contributor is paid. Crafting,
 * enchant, and furnace flow are additionally guarded against duplicate or farmed awards. All
 * handlers run at {@link EventPriority#MONITOR} and pay-through {@link JobsPaymentHandler}.
 */
final class JobPaymentListener implements Listener {

  private final BlockOwnershipService blockOwnershipService;
  private final MobDamageTracker mobDamageTracker;
  private final JobsPaymentHandler paymentHandler;
  private final EntityValidationService entityValidationService;
  private final ExploitService exploitService;
  private final PlayerChunkExplorationService chunkExplorationStore;
  private final HopperPayDisableStore hopperPayDisableStore;
  private final Cache<LocationKey, Player> breakCache = CacheBuilder.newBuilder().expireAfterWrite(
      Duration.ofSeconds(10)).build();

  private static final String CITIZENS = "NPC";

  public static final Predicate<Player> IS_CITIZEN = player -> player.hasMetadata(CITIZENS);

  private final PaymentEligibility eligibility;

  JobPaymentListener(BlockOwnershipService blockOwnershipService, MobDamageTracker mobDamageTracker, JobsPaymentHandler paymentHandler,
      EntityValidationService entityValidationService, ExploitService exploitService, PlayerChunkExplorationService chunkExplorationStore,
      PaymentEligibility eligibility) {
    this(blockOwnershipService, mobDamageTracker, paymentHandler, entityValidationService,
        exploitService, chunkExplorationStore, eligibility, new HopperPayDisableStore());
  }

  JobPaymentListener(BlockOwnershipService blockOwnershipService, MobDamageTracker mobDamageTracker, JobsPaymentHandler paymentHandler,
      EntityValidationService entityValidationService, ExploitService exploitService, PlayerChunkExplorationService chunkExplorationStore,
      PaymentEligibility eligibility, HopperPayDisableStore hopperPayDisableStore) {
    this.blockOwnershipService = blockOwnershipService;
    this.mobDamageTracker = mobDamageTracker;
    this.paymentHandler = paymentHandler;
    this.entityValidationService = entityValidationService;
    this.exploitService = exploitService;
    this.chunkExplorationStore = chunkExplorationStore;
    this.eligibility = eligibility;
    this.hopperPayDisableStore = hopperPayDisableStore;
  }

  /** @return true when the player must not receive job pay */
  boolean blocksPay(Player player) {
    return eligibility.blocksPay(player);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBucketEntity(final PlayerBucketEntityEvent event) {
    Player player = event.getPlayer();
    if (!entityValidationService
        .isValid(event.getEntity())) {
      return;
    }
    if (eligibility.blocksPay(player)) {
      return;
    }
    paymentHandler.pay(player, ActionTypes.BUCKET_ENTITY, BukkitContexts.item(event.getEntityBucket()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onWaxBlock(final PlayerInteractEvent event) {
    Action action = event.getAction();
    Block block = event.getClickedBlock();
    if (action != Action.RIGHT_CLICK_BLOCK || block == null) {
      return;
    }
    Material material = block.getType();
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    ItemStack itemStack = event.getItem();
    if (itemStack == null || itemStack.getType() != Material.HONEYCOMB) {
      return;
    }
    String raw = material.toString();
    if (!raw.contains("COPPER") || raw.contains("WAXED")) {
      return;
    }
    if (exploitService.canProtect(ExploitProtectionType.WAX, block)) {
      if (exploitService.isProtected(ExploitProtectionType.WAX, block)) {
        return;
      }
      exploitService.addProtection(ExploitProtectionType.WAX, block);
    }
    paymentHandler.pay(player, ActionTypes.WAX, BukkitContexts.material(material));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onStripLog(final PlayerInteractEvent event) {
    Action action = event.getAction();
    Block block = event.getClickedBlock();
    if (action != Action.RIGHT_CLICK_BLOCK || block == null) {
      return;
    }
    Material material = block.getType();
    String materialName = material.toString();
    // Ignore already-stripped logs to avoid duplicate payment.
    if (materialName.startsWith("STRIPPED_")) {
      return;
    }
    if (!(materialName.endsWith("_LOG")
        || materialName.endsWith("_STEM")
        || materialName.endsWith("_WOOD")
        || materialName.endsWith("_HYPHAE"))) {
      return;
    }
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    ItemStack itemStack = event.getItem();
    if (itemStack == null || !itemStack.getType().toString().endsWith("_AXE")) {
      return;
    }
    if (exploitService.canProtect(ExploitProtectionType.STRIP, block)) {
      if (exploitService.isProtected(ExploitProtectionType.STRIP, block)) {
        return;
      }
      exploitService.addProtection(ExploitProtectionType.STRIP, block);
    }
    paymentHandler.pay(player, ActionTypes.STRIP_LOG, BukkitContexts.material(material));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBlockPlace(final BlockPlaceEvent event) {
    if (!event.canBuild()) {
      return;
    }
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    Block block = event.getBlock();
    BlockState replacedState = event.getBlockReplacedState();
    Material material = replacedState.getType();
    if (material != Material.AIR && !isReplaceable(material)) {
      return;
    }
    if (exploitService.canProtect(ExploitProtectionType.PLACED, block)) {
      exploitService.addProtection(ExploitProtectionType.PLACED, block);
    }
    paymentHandler.pay(event.getPlayer(), ActionTypes.BLOCK_PLACE, BukkitContexts.material(block
        .getType()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onTameAnimal(final EntityTameEvent event) {
    LivingEntity entity = event.getEntity();
    if (!entityValidationService
        .isValid(entity)) {
      return;
    }
    AnimalTamer owner = event.getOwner();
    if (!(owner instanceof Player player)) {
      return;
    }
    if (eligibility.blocksPay(player)) {
      return;
    }
    paymentHandler.pay(player, ActionTypes.TAME, BukkitContexts.entity(entity));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onEntityShear(final PlayerShearEntityEvent event) {
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    Entity entity = event.getEntity();
    if (!(entity instanceof Sheep || entity instanceof MushroomCow)
        || !entityValidationService
        .isValid(entity)) {
      return;
    }
    List<ItemStack> drops = event.getDrops();
    if (drops.isEmpty()) {
      return;
    }
    paymentHandler.pay(player, ActionTypes.SHEAR, BukkitContexts.item(drops.getFirst()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBlockBreak(final BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    Block block = event.getBlock();
    PlayerInventory inventory = player.getInventory();
    ItemStack mainHand = inventory.getItemInMainHand();
    int silkTouch = mainHand.getEnchantmentLevel(Enchantment.SILK_TOUCH);

    // Placed blocks remain protected until the configured timer expires.
    if (exploitService.canProtect(ExploitProtectionType.PLACED, block)
        && exploitService.isProtected(ExploitProtectionType.PLACED, block)) {
      return;
    }

    // Optional silk-touch protection.
    if (exploitService.settings().silkTouchDeny()
        && silkTouch > 0
        && exploitService.canProtect(ExploitProtectionType.PLACED, block)) {
      return;
    }

    paymentHandler.pay(player, ActionTypes.BLOCK_BREAK, BukkitContexts.block(block));
    breakCache.put(LocationKey.create(block.getLocation()), player);

    // Re-arm protection after a paid break.
    if (exploitService.settings().rearmAfterBreak()
        && exploitService.canProtect(ExploitProtectionType.PLACED, block)) {
      exploitService.addProtection(ExploitProtectionType.PLACED, block);
    }
  }

  @EventHandler
  private void onBlockPhysics(final BlockPhysicsEvent event) {
    Block block = event.getBlock();
    Block sourceBlock = event.getSourceBlock();
    if (block.equals(sourceBlock)) {
      return;
    }
    Material material = block.getType();
    int blockY = block.getY();
    int sourceBlockY = sourceBlock.getY();
    switch (material) {
      case SUGAR_CANE:
      case BAMBOO:
      case KELP_PLANT:
        if (blockY <= sourceBlockY) {
          return;
        }
        break;
      case WEEPING_VINES:
      case WEEPING_VINES_PLANT:
        if (blockY >= sourceBlockY) {
          return;
        }
        break;
      default:
        return;
    }
    //TODO:can perform action in world
    LocationKey sourceKey = LocationKey.create(sourceBlock.getLocation());
    Player player = breakCache.getIfPresent(sourceKey);
    if (player == null) {
      return;
    }
    breakCache.invalidate(sourceKey);
    breakCache.put(LocationKey.create(block.getLocation()), player);
    // Physics cascades respect placed-block protection.
    if (exploitService.canProtect(ExploitProtectionType.PLACED, block)
        && exploitService.isProtected(ExploitProtectionType.PLACED, block)) {
      return;
    }
    paymentHandler.pay(player, ActionTypes.BLOCK_BREAK, BukkitContexts.block(block));
    if (exploitService.settings().rearmAfterBreak()
        && exploitService.canProtect(ExploitProtectionType.PLACED, block)) {
      exploitService.addProtection(ExploitProtectionType.PLACED, block);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onMilkEntity(final PlayerInteractEntityEvent event) {
    Entity entity = event.getRightClicked();
    if (!(entity instanceof Cow || entity instanceof Goat)) {
      return;
    }
    if (!entityValidationService.isValid(entity)) {
      return;
    }
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    PlayerInventory inventory = player.getInventory();
    ItemStack hand = inventory.getItemInMainHand();
    Material type = hand.getType();
    if ((type != Material.BOWL && type != Material.BUCKET) ||
        (type == Material.BOWL && !(entity instanceof MushroomCow))) {
      return;
    }
    // Apply the milk cooldown.
    if (exploitService.canProtect(ExploitProtectionType.MILK, entity)) {
      if (exploitService.isProtected(ExploitProtectionType.MILK, entity)) {
        return;
      }
      exploitService.addProtection(ExploitProtectionType.MILK, entity);
    }
    paymentHandler.pay(player, ActionTypes.MILK, BukkitContexts.entity(entity));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBrushBlock(final BlockDropItemEvent event) {
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    Block block = event.getBlock();
    Material material = block.getType();
    if (!material.toString().contains("SUSPICIOUS_")) {
      return;
    }
    List<Item> items = event.getItems();
    if (items.isEmpty()) {
      return;
    }
    paymentHandler.pay(player, ActionTypes.BRUSH, BukkitContexts.item(items.getFirst().getItemStack()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBreedEntity(final EntityBreedEvent event) {
    LivingEntity breeder = event.getBreeder();
    if (!(breeder instanceof Player player)) {
      return;
    }
    if (!entityValidationService.isValid(event.getFather())
        || !entityValidationService.isValid(
        event.getMother())) {
      return;
    }
    if (eligibility.blocksPay(player)) {
      return;
    }
    paymentHandler.pay(player, ActionTypes.BREED, BukkitContexts.entity(event.getEntity()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onKill(final EntityDeathEvent event) {
    LivingEntity victim = event.getEntity();
    if (!entityValidationService.isValid(victim)) {
      return;
    }
    DamageSource damageSource = event.getDamageSource();
    Entity killer = damageSource.getCausingEntity();
    if (killer == null) {
      return;
    }
    @Nullable Player player = resolveKillingPlayer(killer);
    if (player == null ||
        eligibility.blocksPay(player)) {
      return;
    }
    boolean victimIsRealPlayer =
        victim instanceof Player && !victim.hasMetadata(CITIZENS);
    if (victimIsRealPlayer && player.getUniqueId().equals(victim.getUniqueId())) {
      return;
    }

    // Use tracked damage contributions when available.
    if (mobDamageTracker.isTracking(victim)) {
      DamageContribution contribution = mobDamageTracker.endTracking(victim);
      if (exploitService.settings().monsterDamageRequired() && !isMonsterDamageBossExempt(victim)) {
        double playerDamage = totalPlayerDamage(contribution);
        double maxHealth = maxHealthOf(victim);
        if (maxHealth <= 0
            || playerDamage / maxHealth < exploitService.settings().monsterDamageFraction()) {
          return;
        }
      }
      KillContributionPayout.payContributors(
          contribution,
          eligibility.settings().killContributionCutoff(),
          eligibility,
          paymentHandler,
          victim);
      return;
    }
    // No tracked player damage: only allow if monster-damage gate is off
    if (exploitService.settings().monsterDamageRequired() && !isMonsterDamageBossExempt(victim)) {
      return;
    }
    paymentHandler.pay(player, ActionTypes.KILL, BukkitContexts.entity(victim));
  }

  private static double maxHealthOf(LivingEntity victim) {
    var attr = victim.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
    if (attr != null) {
      return attr.getValue();
    }
    return victim.getHealth();
  }

  private static boolean isMonsterDamageBossExempt(LivingEntity victim) {
    return switch (victim.getType()) {
      case ENDER_DRAGON, WITHER, WARDEN -> true;
      default -> false;
    };
  }

  private static double totalPlayerDamage(DamageContribution contribution) {
    double total = 0;
    for (Entity contributor : contribution.getContributors()) {
      if (contributor instanceof Player) {
        total += contribution.getContribution(contributor, false);
      }
    }
    return total;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onEat(final PlayerItemConsumeEvent event) {
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    ItemStack itemStack = event.getItem();
    if (itemStack.getItemMeta() instanceof PotionMeta potionMeta) {
      paymentHandler.pay(player, ActionTypes.CONSUME, BukkitContexts.potion(potionMeta.getBasePotionType()));
      return;
    }
    paymentHandler.pay(player, ActionTypes.CONSUME, BukkitContexts.item(itemStack));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPickupItem(final EntityPickupItemEvent event) {
    Entity entity = event.getEntity();
    if (!(entity instanceof Player player)) {
      return;
    }
    if (eligibility.blocksPay(player)) {
      return;
    }
    Item item = event.getItem();
    paymentHandler.pay(player, ActionTypes.COLLECT, BukkitContexts.item(item.getItemStack()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onHarvestBerries(final PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    Block block = event.getClickedBlock();
    if (block == null) {
      return;
    }
    Material material = block.getType();
    // Check for harvestable blocks: sweet berry bush, cave vines with berries, or cocoa
    if (material != Material.SWEET_BERRY_BUSH
        && material != Material.CAVE_VINES
        && material != Material.CAVE_VINES_PLANT
        && material != Material.COCOA) {
      return;
    }
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    BlockState state = block.getState();

    // Check age/state requirements for each block type
    if (material == Material.SWEET_BERRY_BUSH) {
      if (state.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
        if (ageable.getAge() < 2) {
          return; // Only harvestable at age 2 or 3
        }
      }
    } else if (material == Material.COCOA) {
      if (state.getBlockData() instanceof org.bukkit.block.data.type.Cocoa cocoa) {
        if (cocoa.getAge() < cocoa.getMaximumAge()) {
          return; // Only harvestable when fully grown
        }
      }
    } else if (material == Material.CAVE_VINES || material == Material.CAVE_VINES_PLANT) {
      if (state.getBlockData() instanceof org.bukkit.block.data.type.CaveVinesPlant caveVines) {
        if (!caveVines.isBerries()) {
          return; // Only harvestable when berries are present
        }
      }
    }

    // Determine the collected item
    ItemStack collectedItem = switch (material) {
      case SWEET_BERRY_BUSH -> new ItemStack(Material.SWEET_BERRIES);
      case COCOA -> new ItemStack(Material.COCOA_BEANS);
      default -> new ItemStack(Material.GLOW_BERRIES); // CAVE_VINES*
    };

    paymentHandler.pay(player, ActionTypes.COLLECT, BukkitContexts.item(collectedItem));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onFish(final PlayerFishEvent event) {
    if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
      return;
    }
    Entity caught = event.getCaught();
    if (!(caught instanceof Item item)) {
      return;
    }
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    ItemStack itemStack = item.getItemStack();
    paymentHandler.pay(player, ActionTypes.FISH, BukkitContexts.item(itemStack));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onFurnaceSmelt(final FurnaceSmeltEvent event) {
    Block block = event.getBlock();
    OfflinePlayer owner = blockOwnershipService.getOwner(block).orElse(null);
    if (owner == null || !owner.isOnline()) {
      return;
    }
    Player player = owner.getPlayer();
    assert player != null;
    if (eligibility.blocksPay(player)) {
      return;
    }
    // Hopper-filled containers do not pay smelt rewards.
    if (hopperPayDisableStore.isDisabled(block)) {
      return;
    }
    double v = block.getLocation().distanceSquared(player.getLocation());
    if (v > eligibility.settings().furnaceMaxDistanceSquared()) {
      return;
    }
    ItemStack result = event.getResult();
    paymentHandler.pay(player, ActionTypes.SMELT, BukkitContexts.item(result));
    // Also pay for BAKE action if the smelted result is food
    if (result.getType().isEdible()) {
      paymentHandler.pay(player, ActionTypes.BAKE, BukkitContexts.item(result));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBrewEvent(final BrewEvent event) {
    Block block = event.getBlock();
    OfflinePlayer owner = blockOwnershipService.getOwner(block).orElse(null);
    if (owner == null || !owner.isOnline()) {
      return;
    }
    Player player = owner.getPlayer();
    assert player != null;
    if (eligibility.blocksPay(player)) {
      return;
    }
    // Hopper-filled containers do not pay brew rewards.
    if (hopperPayDisableStore.isDisabled(block)) {
      return;
    }
    double v = block.getLocation().distanceSquared(player.getLocation());
    if (v > eligibility.settings().furnaceMaxDistanceSquared()) {
      return;
    }
    paymentHandler.pay(player, ActionTypes.BREW, BukkitContexts.item(event.getContents().getIngredient()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onExplore(final PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    Chunk from = event.getFrom().getChunk();
    Chunk to = event.getTo().getChunk();
    if (from.equals(to)) {
      return;
    }
    if (!chunkExplorationStore.hasExplored(player, to)) {
      chunkExplorationStore.addExploration(player, to);
      paymentHandler.pay(player, ActionTypes.EXPLORE, BukkitContexts.chunk(to));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onDyeEntity(final EntityDyeEvent event) {
    Entity entity = event.getEntity();
    if (entity.isDead() || !entityValidationService.isValid(entity)) {
      return;
    }
    Player player = event.getPlayer();
    if (eligibility.blocksPay(player)) {
      return;
    }
    if (exploitService.canProtect(ExploitProtectionType.DYE_ENTITY, entity)) {
      if (exploitService.isProtected(ExploitProtectionType.DYE_ENTITY, entity)) {
        return;
      }
      exploitService.addProtection(ExploitProtectionType.DYE_ENTITY, entity);
    }
    paymentHandler.pay(player, ActionTypes.DYE, BukkitContexts.dye(event.getColor()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onEnchant(final EnchantItemEvent event) {
    Player player = event.getEnchanter();
    if (eligibility.blocksPay(player)) {
      return;
    }
    Inventory inventory = event.getInventory();
    if (!(inventory instanceof EnchantingInventory enchantingInventory)) {
      return;
    }
    ItemStack result = enchantingInventory.getItem();
    if (result == null) {
      return;
    }
    Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();
    for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
      Enchantment enchantment = entry.getKey();
      if (enchantment == null) {
        continue;
      }
      paymentHandler.pay(player, ActionTypes.ENCHANT,
          BukkitContexts.enchantment(enchantment, entry.getValue()));
    }
    paymentHandler.pay(player, ActionTypes.ENCHANT, BukkitContexts.item(result));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onTntBreak(final EntityExplodeEvent event) {
    Entity entity = event.getEntity();
    if (!(entity instanceof TNTPrimed tnt) || !(tnt.getSource() instanceof Player player)) {
      return;
    }
    if (eligibility.blocksPay(player)) {
      return;
    }
    for (Block block : event.blockList()) {
      // TNT cascades respect placed-block protection.
      if (exploitService.canProtect(ExploitProtectionType.PLACED, block)
          && exploitService.isProtected(ExploitProtectionType.PLACED, block)) {
        continue;
      }
      paymentHandler.pay(player, ActionTypes.TNT_BREAK, BukkitContexts.block(block));
      if (exploitService.settings().rearmAfterBreak()
          && exploitService.canProtect(ExploitProtectionType.PLACED, block)) {
        exploitService.addProtection(ExploitProtectionType.PLACED, block);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onVillagerTrade(final InventoryClickEvent event) {
    if (!(event.getInventory() instanceof MerchantInventory merchantInventory)) {
      return;
    }
    if (event.getSlotType() != SlotType.RESULT) {
      return;
    }
    if (!(merchantInventory.getHolder() instanceof Villager)) {
      return;
    }
    ItemStack resultItem = event.getCurrentItem();
    if (resultItem == null || resultItem.getType().isAir()) {
      return;
    }
    HumanEntity entity = event.getWhoClicked();
    if (!(entity instanceof Player player)) {
      return;
    }
    if (eligibility.blocksPay(player)) {
      return;
    }
    paymentHandler.pay(player, ActionTypes.VILLAGER_TRADE, BukkitContexts.item(resultItem));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onCraft(final CraftItemEvent event) {
    switch (event.getAction()) {
      case NOTHING:
      case PLACE_ONE:
      case PLACE_SOME:
      case PLACE_ALL:
        return;
      default:
        break;
    }
    ItemStack resultStack = event.getCurrentItem();
    if (event.getSlotType() != SlotType.RESULT || resultStack == null) {
      return;
    }
    if (!event.isLeftClick() && !event.isRightClick()) {
      return;
    }
    HumanEntity entity = event.getWhoClicked();
    if (!(entity instanceof Player player)) {
      return;
    }
    if (eligibility.blocksPay(player)) {
      return;
    }
    PlayerInventory inventory = player.getInventory();
    if (inventory.firstEmpty() == -1 && event.isShiftClick()) {
      Bukkit.broadcastMessage("full inv");
      return;
    }
    CraftingInventory craftingInventory = event.getInventory();
    List<ItemStack> contents = Arrays.stream(craftingInventory.getContents())
        .filter(itemStack -> itemStack != null && !itemStack.getType().isAir()).toList();
    Set<Material> unique = contents.stream().map(ItemStack::getType).collect(
        Collectors.toSet());
    if (contents.size() == 3 && unique.size() == 1) {
      paymentHandler.pay(player, ActionTypes.REPAIR, BukkitContexts.item(resultStack.clone()));
      return;
    }
    List<DyeColor> dyes = contents.stream().map(ItemStack::getType)
        .filter(material -> material.toString().endsWith("_DYE"))
        .map(material -> DyeColor.valueOf(material.name().replace("_DYE", ""))).toList();

    Optional<Material> dyedMaterial = unique.stream().filter(material -> switch (material) {
      case LEATHER_BOOTS, LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS -> true;
      default -> material.toString().contains("SHULKER_BOX");
    }).findFirst();
    if (!dyes.isEmpty() && dyedMaterial.isPresent()) {
      paymentHandler.pay(player, ActionTypes.DYE, BukkitContexts.item(ItemStack.of(dyedMaterial.get())));
      for (DyeColor color : dyes) {
        paymentHandler.pay(player, ActionTypes.DYE, BukkitContexts.dye(color));
      }
      return;
    }
    if (resultStack.getAmount() > 0) {
      ItemStack reference = resultStack.clone();
      if (event.isShiftClick()) {
        List<ItemStack> snapShot = Arrays.stream(inventory.getContents())
            .filter(Objects::nonNull)
            .map(ItemStack::clone)
            .toList();
        Bukkit.getScheduler().runTask(PluginProvider.get(), () -> {
          int before = countSimilarItems(snapShot, reference);
          int after = countSimilarItems(Arrays.asList(inventory.getContents()), reference);
          for (int i = 0; i < Math.max(1, after - before); ++i) {
            paymentHandler.pay(player, ActionTypes.CRAFT, BukkitContexts.item(reference));
          }
        });
      } else {
        for (int i = 0; i < resultStack.getAmount(); ++i) {
          paymentHandler.pay(player, ActionTypes.CRAFT, BukkitContexts.item(resultStack));
        }
      }
    }
  }

  private static int countSimilarItems(Collection<ItemStack> contents,
      ItemStack reference) {
    int amount = 0;
    for (ItemStack content : contents) {
      if (content != null && content.isSimilar(reference)) {
        amount += content.getAmount();
      }
    }
    return amount;
  }

  @Internal
  private static Player resolveKillingPlayer(@NotNull Entity killer) {
    if (killer instanceof Player player) {
      return player;
    }
    if (killer instanceof Projectile projectile
        && projectile.getShooter() instanceof Player player) {
      return player;
    }
    if (killer instanceof Tameable tameable && tameable.getOwner() instanceof Player player) {
      return player;
    }
    return null;
  }

  private boolean isOre(Material material) {
    String name = material.name();
    return name.endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
  }

  private boolean isReplaceable(Material material) {
    return switch (material) {
      case SHORT_GRASS, TALL_GRASS, DEAD_BUSH, SNOW, FIRE, VINE, AIR, CAVE_AIR, VOID_AIR -> true;
      default -> false;
    };
  }
}
