package net.aincraft.math;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.time.Duration;
import net.aincraft.LevelingCurve;
import net.aincraft.PayableCurve;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Utility for creating cached expression-based curves.
 */
public final class ExpressionCurves {

  private static final int MAX_EXPRESSIONS = 100;
  private static final int MAX_MEMO_ENTRIES = 1000;

  private static final Cache<String, LevelingCurve> LEVELING_CACHE = Caffeine.newBuilder()
      .maximumSize(MAX_EXPRESSIONS)
      .expireAfterAccess(Duration.ofHours(1))
      .build();

  private static final Cache<String, PayableCurve> PAYABLE_CACHE = Caffeine.newBuilder()
      .maximumSize(MAX_EXPRESSIONS)
      .expireAfterAccess(Duration.ofHours(1))
      .build();

  private ExpressionCurves() {}

  public static @NotNull LevelingCurve levelingCurve(@NotNull String expression) {
    LevelingCurve cached = LEVELING_CACHE.getIfPresent(expression);
    if (cached != null) {
      return cached;
    }
    LevelingCurve curve = createLevelingCurve(expression);
    LEVELING_CACHE.put(expression, curve);
    return curve;
  }

  private static LevelingCurve createLevelingCurve(String expr) {
    Expression exp = new ExpressionBuilder(expr).variable("level").build();
    Cache<LevelingCurve.Parameters, BigDecimal> memo = Caffeine.newBuilder()
        .maximumSize(MAX_MEMO_ENTRIES)
        .build();
    return new LevelingCurve() {
      @Override
      public BigDecimal evaluate(Parameters params) {
        BigDecimal cached = memo.getIfPresent(params);
        if (cached != null) {
          return cached;
        }
        BigDecimal result = BigDecimal.valueOf(exp.setVariable("level", params.level()).evaluate());
        memo.put(params, result);
        return result;
      }
      @Override
      public String toString() { return expr; }
    };
  }

  public static @NotNull PayableCurve payableCurve(@NotNull String expression) {
    PayableCurve cached = PAYABLE_CACHE.getIfPresent(expression);
    if (cached != null) {
      return cached;
    }
    PayableCurve curve = createPayableCurve(expression);
    PAYABLE_CACHE.put(expression, curve);
    return curve;
  }

  private static PayableCurve createPayableCurve(String expr) {
    Expression exp = new ExpressionBuilder(expr).variables("base", "level", "jobs").build();
    Cache<PayableCurve.Parameters, BigDecimal> memo = Caffeine.newBuilder()
        .maximumSize(MAX_MEMO_ENTRIES)
        .build();
    return new PayableCurve() {
      @Override
      public BigDecimal evaluate(Parameters params) {
        BigDecimal cached = memo.getIfPresent(params);
        if (cached != null) {
          return cached;
        }
        BigDecimal result = BigDecimal.valueOf(
            exp.setVariable("base", params.base().doubleValue())
               .setVariable("level", params.level())
               .setVariable("jobs", params.jobs())
               .evaluate());
        memo.put(params, result);
        return result;
      }
      @Override
      public String toString() { return expr; }
    };
  }
}
