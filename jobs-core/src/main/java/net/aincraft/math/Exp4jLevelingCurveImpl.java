package net.aincraft.math;

import java.math.BigDecimal;
import net.aincraft.LevelingCurve;
import net.aincraft.LevelingCurve.Parameters;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

final class Exp4jLevelingCurveImpl extends AbstractExp4jCurveImpl<Parameters> implements
    LevelingCurve {

  Exp4jLevelingCurveImpl(Expression expression, String expressionString) {
    super(expression, expressionString);
  }

  static LevelingCurve create(String expression) {
    return new Exp4jLevelingCurveImpl(new ExpressionBuilder(expression).variable("level").build(),
        expression);
  }

  @Override
  public BigDecimal evaluate(Parameters parameters) {
    return memo.computeIfAbsent(parameters, ignored -> BigDecimal.valueOf(
        expression.setVariable("level", parameters.level()).evaluate()));
  }
}
