package net.aincraft.math;

import java.util.HashMap;
import java.util.Map;
import net.aincraft.Job.LevelingCurve;
import net.aincraft.Job.PayableCurve;
import org.jetbrains.annotations.NotNull;

final class JobCurveFactoryImpl implements JobCurveFactory {

  private final Map<String, LevelingCurve> levelingCurves = new HashMap<>();
  private final Map<String, PayableCurve> payableCurves = new HashMap<>();

  @Override
  public @NotNull LevelingCurve levelingCurve(@NotNull String expression) {
    return levelingCurves.computeIfAbsent(expression, Exp4jLevelingCurveImpl::create);
  }

  @Override
  public @NotNull PayableCurve payableCurve(@NotNull String expression) {
    return payableCurves.computeIfAbsent(expression, Exp4jPayableCurveImpl::create);
  }
}
