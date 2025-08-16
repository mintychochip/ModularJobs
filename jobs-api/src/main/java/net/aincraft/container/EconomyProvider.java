package net.aincraft.container;

import org.bukkit.OfflinePlayer;

public interface EconomyProvider {
  boolean deposit(OfflinePlayer player, PayableAmount payableAmount);
}
