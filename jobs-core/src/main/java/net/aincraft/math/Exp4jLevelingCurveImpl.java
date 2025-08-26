package net.aincraft.math;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.Job.LevelingCurve;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

final class Exp4jLevelingCurveImpl implements LevelingCurve {

  private final Expression expression;
  private final Map<Parameters, BigDecimal> memo = new HashMap<>();

  private Exp4jLevelingCurveImpl(Expression expression) {
    this.expression = expression;
  }

  static LevelingCurve create(String expression) {
    return new Exp4jLevelingCurveImpl(new ExpressionBuilder(expression).variable("level").build());
  }

  @Override
  public BigDecimal evaluate(Parameters parameters) {
    return memo.computeIfAbsent(parameters, ignored -> BigDecimal.valueOf(
        expression.setVariable("level", parameters.level()).evaluate()));
  }
}
