package net.aincraft.container.boost;

import java.util.function.BiPredicate;

public enum LogicalOperator implements BiPredicate<Boolean, Boolean> {

  AND((a, b) -> a && b),
  OR((a, b) -> a || b),
  XOR((a, b) -> a ^ b),
  NAND((a, b) -> !(a && b)),
  NOR((a, b) -> !(a || b)),
  XNOR((a, b) -> a == b),
  IMPLIES((a, b) -> !a || b); // A → B

  private final BiPredicate<Boolean, Boolean> predicate;

  LogicalOperator(BiPredicate<Boolean, Boolean> predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(Boolean a, Boolean b) {
    return predicate.test(a, b);
  }
}
