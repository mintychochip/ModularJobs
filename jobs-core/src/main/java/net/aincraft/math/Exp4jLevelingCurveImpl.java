package net.aincraft.math;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.Job.LevelingCurve;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

final class Exp4jLevelingCurveImpl implements LevelingCurve {

  private final Expression expression;
  private final Map<Integer, BigDecimal> memo = new HashMap<>();

  Exp4jLevelingCurveImpl(String expression) {
    this.expression = new ExpressionBuilder(expression).variable("level").build();
  }

  @Override
  public BigDecimal evaluate(int level) {
    return memo.computeIfAbsent(level,
        ignored -> BigDecimal.valueOf(expression.setVariable("level", level).evaluate()));
  }
}
