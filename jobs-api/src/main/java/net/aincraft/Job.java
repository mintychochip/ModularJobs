package net.aincraft;

import java.math.BigDecimal;
import java.util.Optional;
import net.aincraft.container.PayableType;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;

public interface Job extends Keyed {

  Component getDisplayName();

  String getPlainName();

  Component getDescription();

  LevelingCurve getLevelingCurve();

  Optional<PayableCurve> getCurve(PayableType type);

  int getMaxLevel();

  interface LevelingCurve {

    @Contract(pure = true)
    BigDecimal evaluate(int level);
  }

  interface PayableCurve {

    @Contract(pure = true)
    BigDecimal evaluate(Parameters parameters);

    record Parameters(BigDecimal base, int level, int jobs) {

    }
  }
}
