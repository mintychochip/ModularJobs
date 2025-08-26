package net.aincraft;

import java.math.BigDecimal;
import org.jetbrains.annotations.Contract;

public interface LevelingCurve {

  @Contract(pure = true)
  BigDecimal evaluate(Parameters parameters);

  record Parameters(int level) {

  }
}
