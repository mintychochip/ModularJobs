package net.aincraft.math;

import java.math.BigDecimal;
import net.aincraft.PayableCurve;
import net.aincraft.PayableCurve.Parameters;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

final class Exp4jPayableCurveImpl extends AbstractExp4jCurveImpl<Parameters> implements
    PayableCurve {

  Exp4jPayableCurveImpl(Expression expression, String expressionString) {
    super(expression, expressionString);
  }

  static PayableCurve create(String expression) {
    return new Exp4jPayableCurveImpl(
        new ExpressionBuilder(expression).variables("base", "level", "jobs").build(), expression);
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
