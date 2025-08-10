package net.aincraft.economy;

import net.aincraft.api.container.PayableAmount;
import org.bukkit.OfflinePlayer;

public interface Economy {
  boolean deposit(OfflinePlayer player, PayableAmount payableAmount);
  boolean withdraw(OfflinePlayer player, PayableAmount payableAmount);
}
