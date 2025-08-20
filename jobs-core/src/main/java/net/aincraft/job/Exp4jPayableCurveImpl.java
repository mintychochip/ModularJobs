package net.aincraft.job;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import net.aincraft.Job.PayableCurve;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

final class Exp4jPayableCurveImpl implements PayableCurve {

  private final Expression expression;
  private final Map<Parameters, BigDecimal> memo = new HashMap<>();

  Exp4jPayableCurveImpl(String expression) {
    this.expression = new ExpressionBuilder(expression).variables("base", "level", "jobs").build();
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
