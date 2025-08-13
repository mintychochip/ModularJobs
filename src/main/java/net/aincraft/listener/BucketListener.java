package net.aincraft.listener;

import io.papermc.paper.event.entity.EntityDyeEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.aincraft.Jobs;
import net.aincraft.api.Bridge;
import net.aincraft.api.Job;
import net.aincraft.api.JobTask;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.ConfigurationValues;
import net.aincraft.api.context.Context.DyeContext;
import net.aincraft.api.context.Context.EntityContext;
import net.aincraft.api.context.Context.ItemContext;
import net.aincraft.api.context.Context.MaterialContext;
import net.aincraft.api.context.Context.PotionContext;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.api.registry.RegistryView;
import net.aincraft.api.service.ExploitService;
import net.aincraft.api.service.ExploitService.ExploitProtectionType;
import net.aincraft.api.service.BlockOwnershipService;
import net.aincraft.api.service.EntityValidationService;
import net.aincraft.api.service.MobDamageTracker;
import net.aincraft.api.service.MobDamageTracker.DamageContribution;
import net.aincraft.service.CSVJobTaskProviderImpl;
import net.kyori.adventure.key.Key;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BucketListener implements Listener {

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
    Jobs.doTask(player,ActionType.MILK,new EntityContext(entity));
  }

  @EventHandler
  private void onInteract(final PlayerInteractEvent event) {
    RegistryView<Job> jobs = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.JOBS);
    Job job = jobs.getOrThrow(Key.key("jobs", "builder"));
    try {
      JobTask task = Bridge.bridge().jobTaskProvider()
          .getTask(job, ActionType.CONSUME, new ItemContext(ItemStack.of(Material.STONE)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    BlockOwnershipService blockOwnershipService = BlockOwnershipService.blockOwnershipService();
    if (!blockOwnershipService.isProtected(block)) {
      return;
    }
    OfflinePlayer player = blockOwnershipService.getOwner(block);
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
    BlockOwnershipService blockOwnershipService = BlockOwnershipService.blockOwnershipService();
    if (!blockOwnershipService.isProtected(block)) {
      return;
    }
    OfflinePlayer player = blockOwnershipService.getOwner(block);
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
