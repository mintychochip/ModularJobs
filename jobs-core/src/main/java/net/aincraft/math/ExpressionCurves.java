package net.aincraft.math;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.aincraft.LevelingCurve;
import net.aincraft.PayableCurve;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Utility for creating cached expression-based curves.
 */
public final class ExpressionCurves {

  private static final Map<String, LevelingCurve> LEVELING_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, PayableCurve> PAYABLE_CACHE = new ConcurrentHashMap<>();

  private ExpressionCurves() {}

  public static @NotNull LevelingCurve levelingCurve(@NotNull String expression) {
    return LEVELING_CACHE.computeIfAbsent(expression, expr -> {
      Expression exp = new ExpressionBuilder(expr).variable("level").build();
      Map<LevelingCurve.Parameters, BigDecimal> memo = new ConcurrentHashMap<>();
      return new LevelingCurve() {
        @Override
        public BigDecimal evaluate(Parameters params) {
          return memo.computeIfAbsent(params, p ->
              BigDecimal.valueOf(exp.setVariable("level", p.level()).evaluate()));
        }
        @Override
        public String toString() { return expr; }
      };
    });
  }

  public static @NotNull PayableCurve payableCurve(@NotNull String expression) {
    return PAYABLE_CACHE.computeIfAbsent(expression, expr -> {
      Expression exp = new ExpressionBuilder(expr).variables("base", "level", "jobs").build();
      Map<PayableCurve.Parameters, BigDecimal> memo = new ConcurrentHashMap<>();
      return new PayableCurve() {
        @Override
        public BigDecimal evaluate(Parameters params) {
          return memo.computeIfAbsent(params, p ->
              BigDecimal.valueOf(
                  exp.setVariable("base", p.base().doubleValue())
                     .setVariable("level", p.level())
                     .setVariable("jobs", p.jobs())
                     .evaluate()));
        }
        @Override
        public String toString() { return expr; }
      };
    });
  }
}
