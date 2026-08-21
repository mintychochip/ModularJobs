package dev.mintychochip.boost.conditions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.container.BoostContext;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.LogicalOperator;
import org.junit.jupiter.api.Test;

/**
 * Pure condition composition / negation without requiring a live Player or World.
 */
class ComposableConditionTest {

  private static final Condition ALWAYS = ctx -> true;
  private static final Condition NEVER = ctx -> false;
  private static final BoostContext EMPTY_CONTEXT = new BoostContext(null, null, null, null, null);

  @Test
  void andRequiresBothTrue() {
    Condition both = BoostFactoryCompose.compose(ALWAYS, ALWAYS, LogicalOperator.AND);
    Condition mixed = BoostFactoryCompose.compose(ALWAYS, NEVER, LogicalOperator.AND);
    assertTrue(both.applies(EMPTY_CONTEXT));
    assertFalse(mixed.applies(EMPTY_CONTEXT));
  }

  @Test
  void orAcceptsEitherTrue() {
    Condition either = BoostFactoryCompose.compose(ALWAYS, NEVER, LogicalOperator.OR);
    Condition none = BoostFactoryCompose.compose(NEVER, NEVER, LogicalOperator.OR);
    assertTrue(either.applies(EMPTY_CONTEXT));
    assertFalse(none.applies(EMPTY_CONTEXT));
  }

  @Test
  void xorIsExclusive() {
    Condition xorTrue = BoostFactoryCompose.compose(ALWAYS, NEVER, LogicalOperator.XOR);
    Condition xorFalse = BoostFactoryCompose.compose(ALWAYS, ALWAYS, LogicalOperator.XOR);
    assertTrue(xorTrue.applies(EMPTY_CONTEXT));
    assertFalse(xorFalse.applies(EMPTY_CONTEXT));
  }

  @Test
  void negateInvertsInnerCondition() {
    Condition notAlways = BoostFactoryCompose.negate(ALWAYS);
    Condition notNever = BoostFactoryCompose.negate(NEVER);
    assertFalse(notAlways.applies(EMPTY_CONTEXT));
    assertTrue(notNever.applies(EMPTY_CONTEXT));
  }

  @Test
  void factoryCompositionUsesDataBagAdapter() {
    Condition factoryAnd = BoostFactoryCompose.compose(ALWAYS, NEVER, LogicalOperator.AND);
    Condition factoryOr = BoostFactoryCompose.compose(ALWAYS, NEVER, LogicalOperator.OR);
    Condition factoryNot = BoostFactoryCompose.negate(ALWAYS);

    assertFalse(factoryAnd.applies(EMPTY_CONTEXT));
    assertTrue(factoryOr.applies(EMPTY_CONTEXT));
    assertFalse(factoryNot.applies(EMPTY_CONTEXT));
  }

  /**
   * Thin access to the production DataBag adapter.
   */
  private static final class BoostFactoryCompose {
    static Condition compose(Condition a, Condition b, LogicalOperator op) {
      return dev.mintychochip.boost.BoostFactoryImpl.INSTANCE.compose(a, b, op);
    }

    static Condition negate(Condition c) {
      return dev.mintychochip.boost.BoostFactoryImpl.INSTANCE.negate(c);
    }
  }
}
