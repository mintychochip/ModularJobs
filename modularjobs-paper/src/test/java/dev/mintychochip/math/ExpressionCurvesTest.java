package dev.mintychochip.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.LevelingCurve;
import dev.mintychochip.PayableCurve;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Drives shipped {@link ExpressionCurves} against real exp4j evaluation + caching. */
class ExpressionCurvesTest {

  @Test
  void levelingCurveEvaluatesLinearExpression() {
    LevelingCurve curve = ExpressionCurves.levelingCurve("level * 100");
    BigDecimal at1 = curve.evaluate(new LevelingCurve.Parameters(1));
    BigDecimal at5 = curve.evaluate(new LevelingCurve.Parameters(5));
    BigDecimal at10 = curve.evaluate(new LevelingCurve.Parameters(10));

    assertEquals(0, new BigDecimal("100.0").compareTo(at1), "level 1 => 100, got " + at1);
    assertEquals(0, new BigDecimal("500.0").compareTo(at5), "level 5 => 500, got " + at5);
    assertEquals(0, new BigDecimal("1000.0").compareTo(at10), "level 10 => 1000, got " + at10);
  }

  @Test
  void levelingCurveEvaluatesQuadraticExpression() {
    LevelingCurve curve = ExpressionCurves.levelingCurve("level * level + 50");
    BigDecimal result = curve.evaluate(new LevelingCurve.Parameters(4));
    // 4*4 + 50 = 66
    assertEquals(0, new BigDecimal("66.0").compareTo(result), "got " + result);
  }

  @Test
  void payableCurveUsesBaseLevelAndJobs() {
    PayableCurve curve = ExpressionCurves.payableCurve("base * (1 + level * 0.1) / jobs");
    BigDecimal result = curve.evaluate(new PayableCurve.Parameters(new BigDecimal("100"), 5, 2));
    // 100 * (1 + 0.5) / 2 = 75
    assertEquals(0, new BigDecimal("75.0").compareTo(result), "got " + result);
  }

  @Test
  void levelingCurveCachesSameExpressionInstance() {
    LevelingCurve a = ExpressionCurves.levelingCurve("level * 2");
    LevelingCurve b = ExpressionCurves.levelingCurve("level * 2");
    assertSame(a, b, "identical expression strings must share cached curve");
  }

  @Test
  void payableCurveCachesSameExpressionInstance() {
    PayableCurve a = ExpressionCurves.payableCurve("base + level");
    PayableCurve b = ExpressionCurves.payableCurve("base + level");
    assertSame(a, b);
  }

  @Test
  void memoizedParametersReturnSameNumericResult() {
    LevelingCurve curve = ExpressionCurves.levelingCurve("level * 3 + 7");
    LevelingCurve.Parameters params = new LevelingCurve.Parameters(8);
    BigDecimal first = curve.evaluate(params);
    BigDecimal second = curve.evaluate(params);
    assertEquals(0, first.compareTo(second));
    assertEquals(0, new BigDecimal("31.0").compareTo(first));
  }

  @Test
  void differentExpressionsYieldDifferentResults() {
    LevelingCurve linear = ExpressionCurves.levelingCurve("level * 10");
    LevelingCurve doubleLinear = ExpressionCurves.levelingCurve("level * 20");
    BigDecimal l = linear.evaluate(new LevelingCurve.Parameters(3));
    BigDecimal d = doubleLinear.evaluate(new LevelingCurve.Parameters(3));
    assertTrue(d.compareTo(l) > 0);
    assertEquals(0, new BigDecimal("30.0").compareTo(l));
    assertEquals(0, new BigDecimal("60.0").compareTo(d));
  }
}
