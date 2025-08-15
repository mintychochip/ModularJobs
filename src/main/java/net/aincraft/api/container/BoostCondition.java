package net.aincraft.api.container;

import net.aincraft.api.JobProgressionView;
import net.aincraft.api.action.ActionType;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface BoostCondition {

  boolean test(BoostContext context);

  record BoostContext(ActionType type, JobProgressionView progression, Player player) {

    public World world() {
      return player.getWorld();
    }
  }
}
