package dev.mintychochip.boost;

import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.SerializableBoostData.ConsumableBoostData;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.container.boost.TimedBoostDataService.Target.PlayerTarget;
import dev.mintychochip.service.ItemBoostDataService;
import java.util.Optional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener that turns consumed consumable items carrying {@link ConsumableBoostData} into active
 * timed boosts for the consuming player. On a {@link PlayerItemConsumeEvent}, if the consumed
 * {@link ItemStack} has consumable boost data, that data is applied to the player via {@link
 * TimedBoostDataService}.
 */
public class ConsumableBoostController implements Listener {

  private final ItemBoostDataService boostDataService;
  private final TimedBoostDataService timedBoostDataService;

  /**
   * Creates a controller that reads boost data off consumed items and applies it via the supplied
   * timed boost service.
   *
   * @param boostDataService service reading boost data from an {@link ItemStack}
   * @param timedBoostDataService service that records active timed boosts for a player
   */
  public ConsumableBoostController(
      ItemBoostDataService boostDataService, TimedBoostDataService timedBoostDataService) {
    this.boostDataService = boostDataService;
    this.timedBoostDataService = timedBoostDataService;
  }

  /** Applies consumable boost data carried by the consumed item to the consuming player. */
  @EventHandler
  public void onConsumeItem(final PlayerItemConsumeEvent event) {
    ItemStack itemStack = event.getItem();
    Optional<SerializableBoostData> data = boostDataService.getData(itemStack);
    if (data.isEmpty()) {
      return;
    }
    if (data.get() instanceof ConsumableBoostData consumableBoostData) {
      timedBoostDataService.addData(
          consumableBoostData, new PlayerTarget(event.getPlayer().getUniqueId()));
    }
  }
}
