package net.aincraft.boost.conditions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.LogicalOperator;
import org.junit.jupiter.api.Test;

/**
 * Pure condition composition / negation without requiring a live Player or World.
 */
class ComposableConditionTest {

  private static final Condition ALWAYS = ctx -> true;
  private static final Condition NEVER = ctx -> false;
  private static final BoostContext EMPTY_CONTEXT = new BoostContext(null, null, null, null);

  @Test
  void andRequiresBothTrue() {
    Condition both = Conditions.compose(ALWAYS, ALWAYS, LogicalOperator.AND);
    Condition mixed = Conditions.compose(ALWAYS, NEVER, LogicalOperator.AND);
    assertTrue(both.applies(EMPTY_CONTEXT));
    assertFalse(mixed.applies(EMPTY_CONTEXT));
  }

  @Test
  void orAcceptsEitherTrue() {
    Condition either = Conditions.compose(ALWAYS, NEVER, LogicalOperator.OR);
    Condition none = Conditions.compose(NEVER, NEVER, LogicalOperator.OR);
    assertTrue(either.applies(EMPTY_CONTEXT));
    assertFalse(none.applies(EMPTY_CONTEXT));
  }

  @Test
  void xorIsExclusive() {
    Condition xorTrue = Conditions.compose(ALWAYS, NEVER, LogicalOperator.XOR);
    Condition xorFalse = Conditions.compose(ALWAYS, ALWAYS, LogicalOperator.XOR);
    assertTrue(xorTrue.applies(EMPTY_CONTEXT));
    assertFalse(xorFalse.applies(EMPTY_CONTEXT));
  }

  @Test
  void negateInvertsInnerCondition() {
    Condition notAlways = Conditions.negate(ALWAYS);
    Condition notNever = Conditions.negate(NEVER);
    assertFalse(notAlways.applies(EMPTY_CONTEXT));
    assertTrue(notNever.applies(EMPTY_CONTEXT));
  }

  @Test
  void factoryComposeAndNegateMatchStaticHelpers() {
    Condition factoryAnd = BoostFactoryCompose.compose(ALWAYS, NEVER, LogicalOperator.AND);
    Condition factoryOr = BoostFactoryCompose.compose(ALWAYS, NEVER, LogicalOperator.OR);
    Condition factoryNot = BoostFactoryCompose.negate(ALWAYS);

    assertFalse(factoryAnd.applies(EMPTY_CONTEXT));
    assertTrue(factoryOr.applies(EMPTY_CONTEXT));
    assertFalse(factoryNot.applies(EMPTY_CONTEXT));
  }

  /**
   * Thin access to {@link net.aincraft.boost.BoostFactoryImpl} compose/negate without pulling
   * Bukkit-heavy condition factories into every assertion.
   */
  private static final class BoostFactoryCompose {
    static Condition compose(Condition a, Condition b, LogicalOperator op) {
      return net.aincraft.boost.BoostFactoryImpl.INSTANCE.compose(a, b, op);
    }

    static Condition negate(Condition c) {
      return net.aincraft.boost.BoostFactoryImpl.INSTANCE.negate(c);
    }
  }
}
