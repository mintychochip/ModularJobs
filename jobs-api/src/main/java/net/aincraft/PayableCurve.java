package net.aincraft;

import java.math.BigDecimal;
import net.aincraft.container.BoostSource;
import org.jetbrains.annotations.Contract;

public interface PayableCurve {

  @Contract(pure = true)
  BigDecimal evaluate(Parameters parameters);

  record Parameters(BigDecimal base, int level, int jobs) {

  }
}
