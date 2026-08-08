package net.aincraft.profession;

import java.util.Locale;
import java.util.OptionalInt;
import net.aincraft.service.ProfessionService;
import net.aincraft.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Cancels breaking a material whose configured profession level requirement is unmet.
 *
 * <p>Runs at {@link EventPriority#NORMAL} so a cancelled break happens before the
 * {@code MONITOR} payment listener, keeping denied breaks out of pay/exploit logic.
 */
public final class BlockBreakGateListener implements Listener {

  public static final String BYPASS_PERMISSION = "modularjobs.bypassblockbreak";

  private final BlockBreakGateStore store;
  private final ProfessionService professionService;

  public BlockBreakGateListener(
      @NotNull BlockBreakGateStore store,
      @NotNull ProfessionService professionService) {
    this.store = store;
    this.professionService = professionService;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onBlockBreak(final BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (player.hasPermission(BYPASS_PERMISSION)) {
      return;
    }
    String materialKey = event.getBlock().getType().name().toLowerCase(Locale.ROOT);
    BlockBreakGate gate = store.gateFor(materialKey).orElse(null);
    if (gate == null) {
      return;
    }
    OptionalInt level = professionService.level(player.getUniqueId(), gate.professionId());
    if (level.isEmpty() || level.getAsInt() < gate.minLevel()) {
      event.setCancelled(true);
      Messages.send(player, gateMessage(gate));
    }
  }

  private static String gateMessage(BlockBreakGate gate) {
    return "<error>Level <primary>" + gate.minLevel()
        + " <error>" + gate.professionId()
        + " required to break <secondary>" + gate.materialKey() + "</secondary>";
  }
}
