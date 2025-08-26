package net.aincraft.math;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.objecthunter.exp4j.Expression;

abstract class AbstractExp4jCurveImpl<T> {

  protected final Expression expression;
  protected final String expressionString;

  protected final Map<T, BigDecimal> memo = new HashMap<>();

  AbstractExp4jCurveImpl(Expression expression, String expressionString) {
    this.expression = expression;
    this.expressionString = expressionString;
  }

  @Override
  public String toString() {
    return expressionString;
  }
}
