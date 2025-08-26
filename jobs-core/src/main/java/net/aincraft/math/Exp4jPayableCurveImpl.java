package net.aincraft.math;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.Job.PayableCurve;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

final class Exp4jPayableCurveImpl implements PayableCurve {

  private final Expression expression;
  private final Map<Parameters, BigDecimal> memo = new HashMap<>();

  private Exp4jPayableCurveImpl(Expression expression) {
    this.expression = expression;
  }

  static PayableCurve create(String expression) {
    return new Exp4jPayableCurveImpl(
        new ExpressionBuilder(expression).variables("base", "level", "jobs").build());
  }

  @Override
  public BigDecimal evaluate(Parameters parameters) {
    return memo.computeIfAbsent(parameters,
        ignored -> BigDecimal.valueOf(
            expression.setVariable("base", parameters.base().doubleValue())
                .setVariable("level", parameters.level())
                .setVariable("jobs", parameters.jobs()).evaluate()));
  }
}
