package net.aincraft.math;

import java.math.BigDecimal;
import java.util.List;
import net.aincraft.JobProgressionView;
import net.aincraft.PayableCurve;
import net.aincraft.PayableCurve.Parameters;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.Payable;
import net.aincraft.container.PayableAmount;
import net.kyori.adventure.key.Key;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.jetbrains.annotations.NotNull;

final class Exp4jPayableCurveImpl extends AbstractExp4jCurveImpl<Parameters> implements
    PayableCurve {

  Exp4jPayableCurveImpl(Expression expression, String expressionString) {
    super(expression, expressionString);
  }

  static PayableCurve create(String expressionString) {
    return new Exp4jPayableCurveImpl(
        new ExpressionBuilder(expressionString).variables("base", "level", "jobs").build(), expressionString);
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
