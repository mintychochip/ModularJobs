package net.aincraft.math;

import net.aincraft.Job.LevelingCurve;
import net.aincraft.Job.PayableCurve;
import org.jetbrains.annotations.NotNull;

public interface JobCurveFactory {

  @NotNull
  LevelingCurve levelingCurve(@NotNull String expression);
  @NotNull
  PayableCurve payableCurve(@NotNull String expression);
}
