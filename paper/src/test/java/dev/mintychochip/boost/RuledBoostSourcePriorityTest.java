package dev.mintychochip.boost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import dev.mintychochip.container.Boost;
import dev.mintychochip.container.BoostContext;
import dev.mintychochip.container.boost.Condition;
import dev.mintychochip.container.boost.RuledBoostSource.Rule;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

/**
 * Highest-priority matching rule wins; lower-priority matches do not stack.
 */
class RuledBoostSourcePriorityTest {

  private static final Condition ALWAYS = context -> true;
  private static final Condition NEVER = context -> false;

  @Test
  void highPriorityWinsWhenConditionMatches() {
    Boost high = new MultiplicativeBoostImpl(new BigDecimal("3.0"));
    Boost low = new MultiplicativeBoostImpl(new BigDecimal("1.25"));
    RuledBoostSourceImpl source = new RuledBoostSourceImpl(
        List.of(
            new Rule(ALWAYS, 100, high),
            new Rule(ALWAYS, 10, low)
        ),
        Key.key("modularjobs", "mining_boost"),
        "test"
    );

    List<Boost> result = source.evaluate(dummyContext());
    assertEquals(1, result.size(), "only one rule should apply");
    BigDecimal amount = result.get(0).boost(new BigDecimal("100"));
    assertEquals(0, new BigDecimal("300.0").compareTo(amount),
        "high-priority 3.0x should win: " + amount);
  }

  @Test
  void lowPriorityAppliesWhenHighPriorityDoesNotMatch() {
    Boost high = new MultiplicativeBoostImpl(new BigDecimal("3.0"));
    Boost low = new MultiplicativeBoostImpl(new BigDecimal("1.25"));
    RuledBoostSourceImpl source = new RuledBoostSourceImpl(
        List.of(
            new Rule(NEVER, 100, high),
            new Rule(ALWAYS, 10, low)
        ),
        Key.key("modularjobs", "mining_boost"),
        "test"
    );

    List<Boost> result = source.evaluate(dummyContext());
    assertEquals(1, result.size());
    BigDecimal amount = result.get(0).boost(new BigDecimal("100"));
    assertEquals(0, new BigDecimal("125.00").compareTo(amount),
        "low-priority 1.25x when high does not match: " + amount);
  }

  @Test
  void noMatchReturnsEmpty() {
    RuledBoostSourceImpl source = new RuledBoostSourceImpl(
        List.of(new Rule(NEVER, 100, new AdditiveBoostImpl(BigDecimal.TEN))),
        Key.key("modularjobs", "none"),
        "test"
    );
    assertTrue(source.evaluate(dummyContext()).isEmpty());
  }

  @Test
  void amongMatchingRulesHighestPriorityOnly() {
    // Default-config shape: high conditional + low always
    Boost netherSneak = new MultiplicativeBoostImpl(new BigDecimal("3.0"));
    Boost base = new MultiplicativeBoostImpl(new BigDecimal("1.25"));
    Condition netherSneakCondition = ALWAYS; // matching branch
    RuledBoostSourceImpl source = new RuledBoostSourceImpl(
        List.of(
            new Rule(netherSneakCondition, 100, netherSneak),
            new Rule(ALWAYS, 10, base)
        ),
        Key.key("modularjobs", "mining_boost"),
        "Enhanced mining rewards"
    );

    List<Boost> evaluated = source.evaluate(dummyContext());
    assertEquals(1, evaluated.size());
    // Must NOT stack 3.0 * 1.25
    assertEquals(0, new BigDecimal("300").compareTo(evaluated.get(0).boost(new BigDecimal("100"))));
  }

  private static BoostContext dummyContext() {
    // Conditions used in these tests do not touch context fields
    return new BoostContext(null, null, null, null, null);
  }
}
