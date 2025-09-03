package net.aincraft.container;

import net.aincraft.JobProgressionView;
import org.bukkit.World;
import org.bukkit.entity.Player;

public record BoostContext(ActionType type, JobProgressionView progression, Player player, Payable payable) {

  public World world() {
    return player.getWorld();
  }
}
