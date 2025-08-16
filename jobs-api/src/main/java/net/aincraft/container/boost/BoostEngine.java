package net.aincraft.container.boost;

import java.util.List;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import org.bukkit.entity.Player;

/**
 *
 */
public interface BoostEngine {

  List<Boost> evaluate(ActionType actionType, Player player);

}
