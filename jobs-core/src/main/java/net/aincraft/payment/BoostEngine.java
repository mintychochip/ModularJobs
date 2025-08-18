package net.aincraft.payment;

import java.util.List;
import net.aincraft.JobProgressionView;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import org.bukkit.entity.Player;


interface BoostEngine {

  List<Boost> evaluate(ActionType actionType, JobProgressionView progression, Player player);

}
