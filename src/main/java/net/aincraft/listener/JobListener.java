package net.aincraft.listener;

import io.papermc.paper.event.entity.EntityDyeEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.aincraft.Jobs;
import net.aincraft.api.Job;
import net.aincraft.api.action.ActionTypes;
import net.aincraft.api.container.Checks;
import net.aincraft.api.container.ExpressionPayableCurveImpl;
import net.aincraft.api.container.Payable;
import net.aincraft.api.container.PayableAmount;
import net.aincraft.api.container.PayableCurve;
import net.aincraft.api.container.PayableType;
import net.aincraft.api.container.PayableTypes;
import net.aincraft.api.context.Context;
import net.aincraft.api.context.Context.DyeContext;
import net.aincraft.api.context.Context.EntityContext;
import net.aincraft.api.context.Context.MaterialContext;
import net.aincraft.api.event.JobsPrePaymentEvent;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.container.JobImpl;
import net.aincraft.economy.Currency;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;

public class JobListener implements Listener {

  static {
    RegistryContainer.registryContainer().editRegistry(RegistryKeys.JOBS, registry -> {
      Map<PayableType, PayableCurve> curves = new HashMap<>();
      curves.put(PayableTypes.EXPERIENCE,
          new ExpressionPayableCurveImpl(new ExpressionBuilder("25 * level + 10 * level * level + level * level * level").variable("level").build()));
      JobImpl job = new JobImpl("builder", Component.text("Fisherman"), Component.text("test"),
          curves);
      List<Payable> payables = new ArrayList<>();
      Currency currency = new Currency() {
        @Override
        public String identifier() {
          return "gold";
        }

        @Override
        public String symbol() {
          return "";
        }
      };
      payables.add(PayableTypes.EXPERIENCE.create(
          PayableAmount.create(new BigDecimal("15.00000051512323"))));
      payables.add(PayableTypes.ECONOMY.create(
          PayableAmount.builder().withAmount(BigDecimal.TWO).withCurrency(currency)));
      job.addTask(ActionTypes.BLOCK_PLACE, new MaterialContext(Material.STONE), payables);
      job.addTask(ActionTypes.BLOCK_BREAK, new MaterialContext(Material.STONE), payables);
      job.addTask(ActionTypes.DYE, new DyeContext(DyeColor.BLUE), payables);
      job.addTask(ActionTypes.KILL, Key.key("minecraft:creeper"), payables);
      job.addTask(ActionTypes.TAME, Key.key("minecraft:wolf"), payables);
      job.addTask(ActionTypes.STRIP_LOG, new MaterialContext(Material.ACACIA_LOG), payables);
      job.addTask(ActionTypes.FISH, new MaterialContext(Material.COD), payables);
      registry.register(job);
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBlockPlace(final BlockPlaceEvent event) {
    if (!event.canBuild()) {
      return;
    }
    Player player = event.getPlayer();
    if (Checks.World.WORLD_DISABLED.test(player.getWorld()) || Checks.Player.NOT_PAY_IN_CREATIVE.or(
        Checks.Player.NOT_PAY_WHILE_RIDING).test(player)) {
      return;
    }
    Jobs.doTask(event.getPlayer(), ActionTypes.BLOCK_PLACE, new MaterialContext(event.getBlock()
        .getType()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onBlockBreak(final BlockBreakEvent event) {
    Jobs.doTask(event.getPlayer(), ActionTypes.BLOCK_BREAK, new MaterialContext(event.getBlock()
        .getType()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onDyeEntity(final EntityDyeEvent event) {
    Jobs.doTask(event.getPlayer(), ActionTypes.DYE, new Context.DyeContext(event.getColor()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onKillEntity(final EntityDeathEvent event) {
    DamageSource damageSource = event.getDamageSource();
    Entity causingEntity = damageSource.getCausingEntity();
    if (!(causingEntity instanceof Player player)) {
      return;
    }
    LivingEntity dead = event.getEntity();
    Jobs.doTask(player, ActionTypes.KILL, new EntityContext(dead));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerFish(final PlayerFishEvent event) {
    Player player = event.getPlayer();
    if (event.getState() != State.CAUGHT_FISH || !(event.getCaught() instanceof Item item)) {
      return;
    }
    ItemStack stack = item.getItemStack();
    Jobs.doTask(player, ActionTypes.FISH, new MaterialContext(stack.getType()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onTame(final EntityTameEvent event) {
    LivingEntity entity = event.getEntity();
    if (Checks.World.WORLD_DISABLED.test(entity.getWorld())) {
      return;
    }
    AnimalTamer owner = event.getOwner();
    if (!(owner instanceof Player player) || Checks.Player.NOT_PAY_IN_CREATIVE.or(
        Checks.Player.NOT_PAY_WHILE_RIDING).test(player)) {
      return;
    }
    Jobs.doTask(player, ActionTypes.TAME, new EntityContext(event.getEntity()));
  }


}
