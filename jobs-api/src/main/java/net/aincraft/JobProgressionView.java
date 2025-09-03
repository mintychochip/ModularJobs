package net.aincraft;

import java.math.BigDecimal;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

public interface JobProgressionView {

  BigDecimal experienceForLevel(int level);

  Job job();

  OfflinePlayer player();

  BigDecimal experience();

  int level();
}
