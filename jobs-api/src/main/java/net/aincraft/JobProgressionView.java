package net.aincraft;

import java.math.BigDecimal;
import org.bukkit.OfflinePlayer;

public interface JobProgressionView {

  BigDecimal experienceForLevel(int level);

  Job job();

  OfflinePlayer player();

  BigDecimal experience();

  int level();
}
