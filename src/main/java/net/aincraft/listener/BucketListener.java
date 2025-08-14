package net.aincraft.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.papermc.paper.event.entity.EntityDyeEvent;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.aincraft.Jobs;
import net.aincraft.api.Bridge;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.ConfigurationValues;
import net.aincraft.api.container.Provider;
import net.aincraft.api.context.Context.BlockContext;
import net.aincraft.api.context.Context.DyeContext;
import net.aincraft.api.context.Context.EnchantmentContext;
import net.aincraft.api.context.Context.EntityContext;
import net.aincraft.api.context.Context.ItemContext;
import net.aincraft.api.context.Context.MaterialContext;
import net.aincraft.api.context.Context.PotionContext;
import net.aincraft.api.service.EntityValidationService;
import net.aincraft.api.service.ExploitService;
import net.aincraft.api.service.ExploitService.ExploitProtectionType;
import net.aincraft.api.service.MobDamageTracker;
import net.aincraft.api.service.MobDamageTracker.DamageContribution;
import net.aincraft.util.LocationKey;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
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
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BucketListener implements Listener {

  private final Cache<LocationKey, Player> breakCache = CacheBuilder.newBuilder().expireAfterWrite(
      Duration.ofSeconds(10)).build();

  private static final String CITIZENS = "NPC";

  private static final Predicate<Player> NO_PAY_IN_CREATIVE = player ->
      player.getGameMode() != GameMode.CREATIVE && !ConfigurationValues.PAY_IN_CREATIVE.get();

  private static final Predicate<Player> NO_PAY_WHILE_RIDING = player ->
      player.isInsideVehicle() && !ConfigurationValues.PAY_WHILE_RIDING.get();

  public static final Predicate<Player> PLAYER_WORLD_DISABLED = player -> ConfigurationValues.DISABLED_WORLDS.get()
      .contains(player.getWorld().getName().toLowerCase(
          Locale.ENGLISH));

  public static final Predicate<Player> IS_CITIZEN = player -> player.hasMetadata(CITIZENS);

  private static final Predicate<Player> ADVENTURE_MODE = player -> player.getGameMode()
      == GameMode.ADVENTURE;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBucketEntity(final PlayerBucketEntityEvent event) {
    Player player = event.getPlayer();
    if (!EntityValidationService.entityValidationService()
        .isValid(event.getEntity())) {
      return;
    }
    if (PLAYER_WORLD_DISABLED.or(NO_PAY_IN_CREATIVE).or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE).test(player)) {
      return;
    }
    Jobs.doTask(player, ActionType.BUCKET_ENTITY, new ItemContext(event.getEntityBucket()));
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
    if (PLAYER_WORLD_DISABLED.or(NO_PAY_IN_CREATIVE).or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE).test(player)) {
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
    ExploitService exploitService = ExploitService.exploitService();
    if (exploitService.canProtect(ExploitProtectionType.WAX, block)) {
      if (exploitService.isProtected(ExploitProtectionType.WAX, block)) {
        return;
      }
      exploitService.addProtection(ExploitProtectionType.WAX, block);
    }
    Jobs.doTask(player, ActionType.WAX, new MaterialContext(material));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBlockPlace(final BlockPlaceEvent event) {
    if (!event.canBuild()) {
      return;
    }
    Player player = event.getPlayer();
    if (PLAYER_WORLD_DISABLED.or(NO_PAY_IN_CREATIVE).or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE).test(player)) {
      return;
    }
    Block block = event.getBlock();
    BlockState replacedState = event.getBlockReplacedState();
    Material material = replacedState.getType();
    if (material != Material.AIR && !isReplaceable(material)) {
      return;
    }
    ExploitService exploitService = ExploitService.exploitService();
    if (exploitService.canProtect(ExploitProtectionType.PLACED, block)) {
      exploitService.addProtection(ExploitProtectionType.PLACED, block);
    }
    Jobs.doTask(event.getPlayer(), ActionType.BLOCK_PLACE, new MaterialContext(block
        .getType()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onTameAnimal(final EntityTameEvent event) {
    LivingEntity entity = event.getEntity();
    if (!EntityValidationService.entityValidationService()
        .isValid(entity)) {
      return;
    }
    AnimalTamer owner = event.getOwner();
    if (!(owner instanceof Player player)) {
      return;
    }
    if (PLAYER_WORLD_DISABLED.or(NO_PAY_IN_CREATIVE).or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE).test(player)) {
      return;
    }
    Jobs.doTask(player, ActionType.TAME, new EntityContext(entity));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onEntityShear(final PlayerShearEntityEvent event) {
    Player player = event.getPlayer();
    if (PLAYER_WORLD_DISABLED.or(NO_PAY_IN_CREATIVE).or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE).test(player)) {
      return;
    }
    Entity entity = event.getEntity();
    if (!(entity instanceof Sheep || entity instanceof MushroomCow)
        || !EntityValidationService.entityValidationService()
        .isValid(entity)) {
      return;
    }
    List<ItemStack> drops = event.getDrops();
    if (drops.isEmpty()) {
      return;
    }
    Jobs.doTask(player, ActionType.SHEAR, new ItemContext(drops.getFirst()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBlockBreak(final BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(player)) {
      return;
    }
    Block block = event.getBlock();
    PlayerInventory inventory = player.getInventory();
    ItemStack mainHand = inventory.getItemInMainHand();
    int silkTouch = mainHand.getEnchantmentLevel(Enchantment.SILK_TOUCH);
    ExploitService exploitService = ExploitService.exploitService();
    if (exploitService.canProtect(ExploitProtectionType.PLACED, block)
        && exploitService.isProtected(ExploitProtectionType.PLACED, block)) {
      exploitService.removeProtection(ExploitProtectionType.PLACED, block);
    }
    if (silkTouch > 0) {
      return;
    }
    Jobs.doTask(player, ActionType.BLOCK_BREAK, new BlockContext(block));
    breakCache.put(LocationKey.create(block.getLocation()), player);
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
    Jobs.doTask(player, ActionType.BLOCK_BREAK, new BlockContext(block));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onMilkEntity(final PlayerInteractEntityEvent event) {
    Entity entity = event.getRightClicked();
    if (!(entity instanceof Cow || entity instanceof Goat)) {
      return;
    }
    if (!EntityValidationService.entityValidationService().isValid(entity)) {
      return;
    }
    Player player = event.getPlayer();
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(player)) {
      return;
    }
    PlayerInventory inventory = player.getInventory();
    ItemStack hand = inventory.getItemInMainHand();
    Material type = hand.getType();
    if ((type != Material.BOWL && type != Material.BUCKET) ||
        (type == Material.BOWL && !(entity instanceof MushroomCow))) {
      return;
    }
    ExploitService exploitService = ExploitService.exploitService();
    if (exploitService.canProtect(ExploitProtectionType.MILK, entity)) {
      if (exploitService.isProtected(ExploitProtectionType.MILK, entity)) {
        return;
      }
      exploitService.addProtection(ExploitProtectionType.MILK, entity);
    }
    Jobs.doTask(player, ActionType.MILK, new EntityContext(entity));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBrushBlock(final BlockDropItemEvent event) {
    Player player = event.getPlayer();
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(player)) {
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
    Jobs.doTask(player, ActionType.BRUSH, new ItemContext(items.getFirst().getItemStack()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBreedEntity(final EntityBreedEvent event) {
    LivingEntity breeder = event.getBreeder();
    if (!(breeder instanceof Player player)) {
      return;
    }
    EntityValidationService entityValidationService = EntityValidationService.entityValidationService();
    if (!entityValidationService.isValid(event.getFather())
        || !entityValidationService.isValid(
        event.getMother())) {
      return;
    }
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(player)) {
      return;
    }
    Jobs.doTask(player, ActionType.BREED, new EntityContext(event.getEntity()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onKill(final EntityDeathEvent event) {
    LivingEntity victim = event.getEntity();
    if (!EntityValidationService.entityValidationService().isValid(victim)) {
      return;
    }
    DamageSource damageSource = event.getDamageSource();
    Entity killer = damageSource.getCausingEntity();
    if (killer == null) {
      return;
    }
    @Nullable Player player = resolveKillingPlayer(killer);
    if (player == null ||
        PLAYER_WORLD_DISABLED
            .or(NO_PAY_IN_CREATIVE)
            .or(NO_PAY_WHILE_RIDING)
            .or(ADVENTURE_MODE)
            .or(IS_CITIZEN).test(player)) {
      return;
    }
    boolean victimIsRealPlayer =
        victim instanceof Player && !victim.hasMetadata(CITIZENS);
    if (victimIsRealPlayer && player.getUniqueId().equals(victim.getUniqueId())) {
      return;
    }
    MobDamageTracker damageTracker = MobDamageTracker.mobDamageTracker();
    if (damageTracker.isTracking(victim)) {
      DamageContribution damageContribution = damageTracker.endTracking(victim);
      Collection<@NotNull Entity> contributors = damageContribution.getContributors();
      for (Entity contributor : contributors) {
        if (contributor instanceof Player) {
          double normalized = damageContribution.getContribution(contributor, true);
          //TODO: configure cutoff
          if (normalized > 0.5) {
            Jobs.doTask(player, ActionType.KILL, new EntityContext(victim));
          }
        }
      }
      return;
    }
    Jobs.doTask(player, ActionType.KILL, new EntityContext(victim));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onEat(final PlayerItemConsumeEvent event) {
    Player player = event.getPlayer();
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(player)) {
      return;
    }
    ItemStack itemStack = event.getItem();
    if (itemStack.getItemMeta() instanceof PotionMeta potionMeta) {
      Jobs.doTask(player, ActionType.CONSUME, new PotionContext(potionMeta.getBasePotionType()));
      return;
    }
    Jobs.doTask(player, ActionType.CONSUME, new ItemContext(itemStack));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onFurnaceSmelt(final FurnaceSmeltEvent event) {
    Block block = event.getBlock();
    Optional<OfflinePlayer> playerOptional = Bridge.bridge()
        .blockOwnershipProvider().get(block);
    if (playerOptional.isEmpty()) {
      return;
    }
    OfflinePlayer player = playerOptional.get();
    if (!player.isOnline()) {
      return;
    }
    Player onlinePlayer = player.getPlayer();
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(onlinePlayer)) {
      return;
    }
    //TODO make this not explicit
    double v = block.getLocation().distanceSquared(onlinePlayer.getLocation());
    if (v > 25 * 25) {
      return;
    }
    Jobs.doTask(player, ActionType.SMELT, new ItemContext(event.getResult()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBrewEvent(final BrewEvent event) {
    Block block = event.getBlock();
    Optional<OfflinePlayer> playerOptional = Bridge.bridge()
        .blockOwnershipProvider().get(block);
    if (playerOptional.isEmpty()) {
      return;
    }
    OfflinePlayer player = playerOptional.get();
    if (!player.isOnline()) {
      return;
    }
    Player onlinePlayer = player.getPlayer();
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(onlinePlayer)) {
      return;
    }
    double v = block.getLocation().distanceSquared(onlinePlayer.getLocation());
    if (v > 25 * 25) {
      return;
    }
    Jobs.doTask(player, ActionType.BREW, new ItemContext(event.getContents().getIngredient()));
  }
//
//  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//  private void onExplore(final PlayerMoveEvent event) {
//    Player player = event.getPlayer();
//    if (!player.isOnline()) {
//      return;
//    }
//    Chunk from = event.getFrom().getChunk();
//    Chunk to = event.getTo().getChunk();
//    if (from.equals(to)) {
//      return;
//    }
//    ChunkExplorationStore store = ChunkExplorationStore.chunkExplorationStore();
//    if (!store.hasExplored(player, to)) {
//      store.addExploration(player, to);
////      Jobs.doTask(player,ActionType.EXPLORE,new );
//    }
//  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onDyeEntity(final EntityDyeEvent event) {
    Entity entity = event.getEntity();
    if (entity.isDead() || !EntityValidationService.entityValidationService().isValid(entity)) {
      return;
    }
    Player player = event.getPlayer();
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(player)) {
      return;
    }
    ExploitService exploitService = ExploitService.exploitService();
    if (exploitService.canProtect(ExploitProtectionType.DYE_ENTITY, entity)) {
      if (exploitService.isProtected(ExploitProtectionType.DYE_ENTITY, entity)) {
        return;
      }
      exploitService.addProtection(ExploitProtectionType.DYE_ENTITY, entity);
    }
    Jobs.doTask(player, ActionType.DYE, new DyeContext(event.getColor()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onEnchant(final EnchantItemEvent event) {
    Player player = event.getEnchanter();
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(player)) {
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
      Jobs.doTask(player, ActionType.ENCHANT,
          new EnchantmentContext(enchantment, entry.getValue()));
    }
    Jobs.doTask(player, ActionType.ENCHANT, new ItemContext(result));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onTntBreak(final EntityExplodeEvent event) {
    Entity entity = event.getEntity();
    if (!(entity instanceof TNTPrimed tnt) || !(tnt.getSource() instanceof Player player)) {
      return;
    }
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(player)) {
      return;
    }
    for (Block block : event.blockList()) {
      Jobs.doTask(player, ActionType.TNT_BREAK, new BlockContext(block));
    }
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
    if (PLAYER_WORLD_DISABLED
        .or(NO_PAY_IN_CREATIVE)
        .or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE)
        .or(IS_CITIZEN).test(player)) {
      return;
    }
    PlayerInventory inventory = player.getInventory();
    if (inventory.firstEmpty() == -1 && event.isShiftClick()) {
      Bukkit.broadcastMessage("full inv");
      return;
    }
    CraftingInventory craftingInventory = event.getInventory();
    List<@NotNull ItemStack> contents = Arrays.stream(craftingInventory.getContents())
        .filter(itemStack -> itemStack != null && !itemStack.getType().isAir()).toList();
    Set<@NotNull Material> unique = contents.stream().map(ItemStack::getType).collect(
        Collectors.toSet());
    if (contents.size() == 3 && unique.size() == 1) {
      Jobs.doTask(player, ActionType.REPAIR, new ItemContext(resultStack.clone()));
      return;
    }
    List<@NotNull DyeColor> dyes = contents.stream().map(ItemStack::getType)
        .filter(material -> material.toString().endsWith("_DYE"))
        .map(material -> DyeColor.valueOf(material.name().replace("_DYE", ""))).toList();

    Optional<Material> dyedMaterial = unique.stream().filter(material -> switch (material) {
      case LEATHER_BOOTS, LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS -> true;
      default -> material.toString().contains("SHULKER_BOX");
    }).findFirst();
    if (!dyes.isEmpty() && dyedMaterial.isPresent()) {
      Jobs.doTask(player, ActionType.DYE, new ItemContext(ItemStack.of(dyedMaterial.get())));
      for (DyeColor color : dyes) {
        Jobs.doTask(player, ActionType.DYE, new DyeContext(color));
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
        Bukkit.getScheduler().runTask(Bridge.bridge().plugin(), () -> {
          int before = countSimilarItems(snapShot, reference);
          int after = countSimilarItems(Arrays.asList(inventory.getContents()), reference);
          for (int i = 0; i < Math.max(1, after - before); ++i) {
            Jobs.doTask(player, ActionType.CRAFT, new ItemContext(reference));
          }
        });
      } else {
        for (int i = 0; i < resultStack.getAmount(); ++i) {
          Jobs.doTask(player, ActionType.CRAFT, new ItemContext(resultStack));
        }
      }
    }
  }

  private static int countSimilarItems(Collection<@Nullable ItemStack> contents,
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

  private boolean isReplaceable(Material material) {
    return switch (material) {
      case SHORT_GRASS, TALL_GRASS, DEAD_BUSH, SNOW, FIRE, VINE, AIR, CAVE_AIR, VOID_AIR -> true;
      default -> false;
    };
  }
}
