package net.aincraft.boost;

import com.google.inject.Inject;
import java.util.Optional;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import net.aincraft.container.boost.ItemBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class ConsumableBoostController implements Listener {

  private final ItemBoostDataService boostDataService;
  private final TimedBoostDataService timedBoostDataService;

  @Inject
  public ConsumableBoostController(ItemBoostDataService boostDataService,
      TimedBoostDataService timedBoostDataService) {
    this.boostDataService = boostDataService;
    this.timedBoostDataService = timedBoostDataService;
  }

  @EventHandler
  private void onConsumeItem(final PlayerItemConsumeEvent event) {
    ItemStack itemStack = event.getItem();
    Optional<SerializableBoostData> data = boostDataService.getData(itemStack);
    if (data.isEmpty()) {
      return;
    }
    if (data.get() instanceof ConsumableBoostData consumableBoostData) {
      Bukkit.broadcastMessage("added data to " + event.getPlayer().toString());
      timedBoostDataService.addData(consumableBoostData, new PlayerTarget(event.getPlayer()));
    }
  }
}
