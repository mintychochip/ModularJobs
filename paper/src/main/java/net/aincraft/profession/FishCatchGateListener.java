package net.aincraft.profession;

import java.util.Locale;
import java.util.OptionalInt;
import net.aincraft.service.ProfessionService;
import net.aincraft.util.Messages;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Cancels catching a configured fish whose profession level requirement is unmet.
 *
 * <p>Runs at {@link EventPriority#NORMAL} so a cancelled catch happens before the
 * {@code MONITOR} payment listener, keeping denied catches out of pay logic.
 */
public final class FishCatchGateListener implements Listener {

  public static final String BYPASS_PERMISSION = "modularjobs.bypassfishcatch";

  private final FishCatchGateStore store;
  private final ProfessionService professionService;

  public FishCatchGateListener(
      @NotNull FishCatchGateStore store,
      @NotNull ProfessionService professionService) {
    this.store = store;
    this.professionService = professionService;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onFish(final PlayerFishEvent event) {
    Player player = event.getPlayer();
    if (player.hasPermission(BYPASS_PERMISSION)
        || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
      return;
    }
    if (!(event.getCaught() instanceof Item item)) {
      return;
    }

    String itemKey = item.getItemStack().getType().name().toLowerCase(Locale.ROOT);
    FishCatchGate gate = store.gateFor(itemKey).orElse(null);
    if (gate == null) {
      return;
    }

    OptionalInt level = professionService.level(player.getUniqueId(), gate.professionId());
    if (level.isEmpty() || level.getAsInt() < gate.minLevel()) {
      event.setCancelled(true);
      Messages.send(player, gateMessage(gate));
    }
  }

  private static String gateMessage(FishCatchGate gate) {
    return "<error>Level <primary>" + gate.minLevel()
        + " <error>" + gate.professionId() + " required to catch <secondary>"
        + gate.itemKey() + "</secondary>";
  }
}
