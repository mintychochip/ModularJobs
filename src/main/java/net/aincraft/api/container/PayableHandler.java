package net.aincraft.api.container;

import java.math.BigDecimal;
import net.kyori.adventure.key.Keyed;
import org.bukkit.OfflinePlayer;

public interface PayableHandler extends Keyed {
  void set(OfflinePlayer player, BigDecimal amount) throws IllegalArgumentException;
  void add(OfflinePlayer player, BigDecimal amount) throws IllegalArgumentException;
  BigDecimal get(OfflinePlayer player) throws IllegalArgumentException;
}
