package net.aincraft.math;

import net.aincraft.LevelingCurve;
import net.aincraft.PayableCurve;
import org.jetbrains.annotations.NotNull;

public interface ExpressionCurveFactory {

  @NotNull
  LevelingCurve levelingCurve(@NotNull String expression);
  @NotNull
  PayableCurve payableCurve(@NotNull String expression);
}
