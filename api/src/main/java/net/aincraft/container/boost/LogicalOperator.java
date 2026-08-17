package net.aincraft.container.boost;

import java.util.function.BiPredicate;

/**
 * A boolean logic operator applied to two boolean operands.
 * <p>
 * Each constant wraps a {@link BiPredicate} with the corresponding boolean
 * semantics over two {@code boolean} values.
 */
public enum LogicalOperator implements BiPredicate<Boolean, Boolean> {

  /** Logical conjunction: {@code true} only when both operands are true. */
  AND((a, b) -> a && b),
  /** Logical disjunction: {@code true} when at least one operand is true. */
  OR((a, b) -> a || b),
  /** Exclusive or: {@code true} when exactly one operand is true. */
  XOR((a, b) -> a ^ b),
  /** Negated conjunction: {@code true} except when both operands are true. */
  NAND((a, b) -> !(a && b)),
  /** Negated disjunction: {@code true} only when both operands are false. */
  NOR((a, b) -> !(a || b)),
  /** Negated exclusive or: {@code true} when both operands are equal. */
  XNOR((a, b) -> a.booleanValue() == b.booleanValue()),
  /** Material implication {@code A → B}: {@code true} unless A is true and B is false. */
  IMPLIES((a, b) -> !a || b);

  private final BiPredicate<Boolean, Boolean> predicate;

  LogicalOperator(BiPredicate<Boolean, Boolean> predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(Boolean a, Boolean b) {
    return predicate.test(a, b);
  }
}
