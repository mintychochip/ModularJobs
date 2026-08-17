package net.aincraft.container.boost;

import java.math.BigDecimal;
import java.util.function.BiPredicate;

/**
 * A numeric comparison operator applied to two {@link BigDecimal} operands.
 * <p>
 * Each constant compares its operands with {@link BigDecimal#compareTo(BigDecimal)},
 * which ignores scale, so values of equal magnitude (e.g. {@code 1} and
 * {@code 1.00}) are considered equal.
 */
public enum RelationalOperator implements BiPredicate<BigDecimal, BigDecimal> {

  /** {@code a < b}. */
  LESS_THAN((a, b) -> a.compareTo(b) < 0),
  /** {@code a <= b}. */
  LESS_THAN_OR_EQUAL((a, b) -> a.compareTo(b) <= 0),
  /** {@code a > b}. */
  GREATER_THAN((a, b) -> a.compareTo(b) > 0),
  /** {@code a >= b}. */
  GREATER_THAN_OR_EQUAL((a, b) -> a.compareTo(b) >= 0),
  /** {@code a == b} (by magnitude, ignoring scale). */
  EQUAL((a, b) -> a.compareTo(b) == 0),
  /** {@code a != b} (by magnitude, ignoring scale). */
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
