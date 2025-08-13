package net.aincraft.api.container;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import net.objecthunter.exp4j.Expression;

public final class ExpressionPayableCurveImpl implements PayableCurve {

  private final Expression expression;

  public ExpressionPayableCurveImpl(Expression expression) {
    this.expression = expression;
  }

  @Override
  public BigDecimal apply(Map<String,Number> variables) {
    Set<String> variableNames = expression.getVariableNames();
    variables.forEach((key, value) -> {
      if (variableNames.contains(key)) {
        expression.setVariable(key, value.doubleValue());
      }
    });
    double result = expression.evaluate();
    return BigDecimal.valueOf(result);
  }
}
