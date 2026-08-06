package net.aincraft;

import java.math.BigDecimal;
import java.util.UUID;

public interface JobProgressionView {

  BigDecimal experienceForLevel(int level);

  Job job();

  UUID playerId();

  BigDecimal experience();

  int level();
}
