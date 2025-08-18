package net.aincraft.payment;

import net.aincraft.container.ActionType;
import net.aincraft.container.Context;
import org.bukkit.OfflinePlayer;

interface JobsPaymentHandler {

  void pay(OfflinePlayer player, ActionType type, Context context);
}
