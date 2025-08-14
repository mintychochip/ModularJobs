package net.aincraft.api.container;

import net.aincraft.api.JobProgressionView;
import net.aincraft.api.action.ActionType;
import org.bukkit.entity.Player;

public interface BoostCondition {

  boolean test(BoostContext context);

  interface BoostContext {
    ActionType getActionType();
    JobProgressionView getProgression();
    Player getPlayer();
  }
}
