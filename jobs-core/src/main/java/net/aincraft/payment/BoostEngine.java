package net.aincraft.payment;

import java.util.List;
import java.util.Map;
import net.aincraft.JobProgression;
import net.aincraft.JobProgressionView;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Boost;
import net.aincraft.container.Context;
import net.aincraft.container.Payable;
import net.kyori.adventure.key.Key;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;


interface BoostEngine {

  Map<Key, Boost> evaluate(OfflinePlayer player, ActionType type, Context context,
      JobProgression progression, Payable payable);

}
