package net.aincraft.api.container.boost;

import java.math.BigDecimal;
import java.util.function.BiPredicate;

public enum RelationalOperator implements BiPredicate<BigDecimal, BigDecimal> {

  LESS_THAN((a, b) -> a.compareTo(b) < 0),
  LESS_THAN_OR_EQUAL((a, b) -> a.compareTo(b) <= 0),
  GREATER_THAN((a, b) -> a.compareTo(b) > 0),
  GREATER_THAN_OR_EQUAL((a, b) -> a.compareTo(b) >= 0),
  EQUAL((a, b) -> a.compareTo(b) == 0),
  NOT_EQUAL((a, b) -> a.compareTo(b) != 0);

  private final BiPredicate<BigDecimal, BigDecimal> predicate;

  RelationalOperator(BiPredicate<BigDecimal, BigDecimal> predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(BigDecimal a, BigDecimal b) {
    return predicate.test(a, b);
  }
}
