package net.aincraft.listener;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.aincraft.Jobs;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.ConfigurationValues;
import net.aincraft.api.context.Context.EntityContext;
import net.aincraft.api.context.Context.ItemContext;
import net.aincraft.api.context.Context.MaterialContext;
import net.aincraft.api.service.BlockExploitService;
import net.aincraft.api.service.BlockExploitService.ProtectionType;
import net.aincraft.api.service.SpawnerService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Goat;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BucketListener implements Listener {

  private static final Predicate<Player> NO_PAY_IN_CREATIVE = player ->
      player.getGameMode() != GameMode.CREATIVE && !ConfigurationValues.PAY_IN_CREATIVE.get();

  private static final Predicate<Player> NO_PAY_WHILE_RIDING = player ->
      player.isInsideVehicle() && !ConfigurationValues.PAY_WHILE_RIDING.get();

  public static final Predicate<Player> PLAYER_WORLD_DISABLED = world -> ConfigurationValues.DISABLED_WORLDS.get()
      .contains(world.getName().toLowerCase(
          Locale.ENGLISH));

  private static final Predicate<Player> ADVENTURE_MODE = player -> player.getGameMode()
      == GameMode.ADVENTURE;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBucket(final PlayerBucketEntityEvent event) {
    Player player = event.getPlayer();
    if (SpawnerService.spawnerService().isSpawnerEntity(event.getEntity())) {
      return;
    }
    if (PLAYER_WORLD_DISABLED.or(NO_PAY_IN_CREATIVE).or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE).test(player)) {
      return;
    }
    Jobs.doTask(player, ActionType.BUCKET, new ItemContext(event.getEntityBucket()));
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
    BlockExploitService exploitService = BlockExploitService.blockExploitService();
    if (exploitService.canProtect(block)) {
      if (exploitService.isProtected(ProtectionType.WAX, block)) {
        return;
      }
      exploitService.addProtection(ProtectionType.WAX, block);
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
    BlockExploitService exploitService = BlockExploitService.blockExploitService();
    if (exploitService.canProtect(block)) {
      exploitService.addProtection(ProtectionType.PLACED, block);
    }
    Jobs.doTask(event.getPlayer(), ActionType.BLOCK_PLACE, new MaterialContext(block
        .getType()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onTameAnimal(final EntityTameEvent event) {
    LivingEntity entity = event.getEntity();

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
        || SpawnerService.spawnerService().isSpawnerEntity(entity)) {
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
    Player player = event.getPlayer();
    if (PLAYER_WORLD_DISABLED.or(NO_PAY_IN_CREATIVE).or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE).test(player)) {
      return;
    }
    Entity entity = event.getRightClicked();
    if (!(entity instanceof Cow || entity instanceof Goat)) {
      return;
    }
    PlayerInventory inventory = player.getInventory();
    ItemStack hand = inventory.getItemInMainHand();
    Material type = hand.getType();
    if ((type != Material.BOWL && type != Material.BUCKET) ||
        (type == Material.BOWL && !(entity instanceof MushroomCow))) {
      return;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBreakBlock(final BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (PLAYER_WORLD_DISABLED.or(NO_PAY_IN_CREATIVE).or(NO_PAY_WHILE_RIDING)
        .or(ADVENTURE_MODE).test(player)) {
      return;
    }
    PlayerInventory inventory = player.getInventory();
    ItemStack mainHand = inventory.getItemInMainHand();
  }

  private boolean isReplaceable(Material material) {
    return switch (material) {
      case SHORT_GRASS, TALL_GRASS, DEAD_BUSH, SNOW, FIRE, VINE, AIR, CAVE_AIR, VOID_AIR -> true;
      default -> false;
    };
  }
}
