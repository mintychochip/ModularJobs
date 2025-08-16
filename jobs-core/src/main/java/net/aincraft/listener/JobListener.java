package net.aincraft.listener;

import java.util.HashMap;
import java.util.Map;
import net.aincraft.Jobs;
import net.aincraft.container.ActionTypes;
import net.aincraft.container.Context.MaterialContext;
import net.aincraft.container.ExpressionPayableCurveImpl;
import net.aincraft.container.JobImpl;
import net.aincraft.container.PayableCurve;
import net.aincraft.container.PayableType;
import net.aincraft.container.PayableTypes;
import net.aincraft.registry.RegistryContainer;
import net.aincraft.registry.RegistryKeys;
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
    Map<PayableType, PayableCurve> curves = new HashMap<>();
    curves.put(PayableTypes.EXPERIENCE,
        new ExpressionPayableCurveImpl(new ExpressionBuilder(
            "25 * level + 10 * level * level + level * level * level").variable("level")
            .build()));
    RegistryContainer.registryContainer().editRegistry(RegistryKeys.JOBS, r -> r.register(new JobImpl("builder",
        Component.text("Builder"), null, curves)));
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
}
