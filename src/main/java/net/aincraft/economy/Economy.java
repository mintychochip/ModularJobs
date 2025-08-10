package net.aincraft.economy;

import java.math.BigDecimal;
import org.bukkit.OfflinePlayer;

public interface Economy {
  boolean deposit(OfflinePlayer player, BigDecimal amount);
  BigDecimal getBalance(OfflinePlayer player);
  boolean withdraw(OfflinePlayer player, BigDecimal money);
}
