package net.aincraft.listener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.aincraft.Jobs;
import net.aincraft.api.action.ActionType;
import net.aincraft.api.container.ExpressionPayableCurveImpl;
import net.aincraft.api.container.Payable;
import net.aincraft.api.container.PayableAmount;
import net.aincraft.api.container.PayableCurve;
import net.aincraft.api.container.PayableType;
import net.aincraft.api.container.PayableTypes;
import net.aincraft.api.context.Context.MaterialContext;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.aincraft.container.JobImpl;
import net.aincraft.economy.Currency;
import net.kyori.adventure.text.Component;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;

public class JobListener implements Listener {

  static {
    RegistryContainer.registryContainer().editRegistry(RegistryKeys.JOBS, registry -> {
      Map<PayableType, PayableCurve> curves = new HashMap<>();
      curves.put(PayableTypes.EXPERIENCE,
          new ExpressionPayableCurveImpl(new ExpressionBuilder(
              "25 * level + 10 * level * level + level * level * level").variable("level")
              .build()));
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
          PayableAmount.create(BigDecimal.TWO,currency)));
//      job.addTask(ActionType.BLOCK_PLACE, new MaterialContext(Material.STONE), payables);
//      job.addTask(ActionType.BLOCK_BREAK, new MaterialContext(Material.STONE), payables);
//      job.addTask(ActionType.DYE, new DyeContext(DyeColor.BLUE), payables);
//      job.addTask(ActionType.KILL, Key.key("minecraft:creeper"), payables);
//      job.addTask(ActionType.TAME, Key.key("minecraft:wolf"), payables);
//      job.addTask(ActionType.STRIP_LOG, new MaterialContext(Material.ACACIA_LOG), payables);
//      job.addTask(ActionType.FISH, new MaterialContext(Material.COD), payables);
//      job.addTask(ActionType.BUCKET_ENTITY,
//          new ItemContext(ItemStack.of(Material.PUFFERFISH_BUCKET)), payables);
//      job.addTask(ActionType.BUCKET_ENTITY, new ItemContext(ItemStack.of(Material.COD_BUCKET)),
//          payables);
//      job.addTask(ActionType.WAX, new MaterialContext(Material.COPPER_BLOCK), payables);
//      job.addTask(ActionType.SHEAR, new ItemContext(ItemStack.of(Material.WHITE_WOOL)), payables);
//      job.addTask(ActionType.BRUSH, new ItemContext(ItemStack.of(Material.ARCHER_POTTERY_SHERD)),
//          payables);
//      job.addTask(ActionType.BREED, Key.key("minecraft:sheep"), payables);
//      job.addTask(ActionType.KILL, Key.key("minecraft:warden"), payables);
//      job.addTask(ActionType.CONSUME, new PotionContext(PotionType.STRONG_POISON), payables);
//      job.addTask(ActionType.CONSUME, new ItemContext(ItemStack.of(Material.COOKED_BEEF)),
//          payables);
//      job.addTask(ActionType.SMELT, new ItemContext(ItemStack.of(Material.COOKED_BEEF)), payables);
//      job.addTask(ActionType.BREW, new ItemContext(ItemStack.of(Material.NETHER_WART)), payables);
//      job.addTask(ActionType.MILK, Key.key("minecraft:cow"), payables);
      registry.register(job);
    });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  private void onPlayerFish(final PlayerFishEvent event) {
    Player player = event.getPlayer();
    if (event.getState() != State.CAUGHT_FISH || !(event.getCaught() instanceof Item item)) {
      return;
    }
    ItemStack stack = item.getItemStack();
    Jobs.doTask(player, ActionType.FISH, new MaterialContext(stack.getType()));
  }
}
