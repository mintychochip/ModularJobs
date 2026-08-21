package dev.mintychochip.boost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import dev.mintychochip.container.Boost;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link AdditiveBoostImpl}, {@link MultiplicativeBoostImpl}, and {@link
 * BoostFactoryImpl} arithmetic contracts.
 */
class BoostArithmeticTest {

  @Test
  void additiveBoostAddsAmount() {
    Boost boost = new AdditiveBoostImpl(new BigDecimal("25.50"));
    BigDecimal result = boost.boost(new BigDecimal("100"));
    assertEquals(0, new BigDecimal("125.50").compareTo(result), "got " + result);
  }

  @Test
  void additiveBoostWithZeroLeavesBaseUnchanged() {
    Boost boost = new AdditiveBoostImpl(BigDecimal.ZERO);
    BigDecimal base = new BigDecimal("42.00");
    assertEquals(0, base.compareTo(boost.boost(base)));
  }

  @Test
  void multiplicativeBoostScalesAmount() {
    Boost boost = new MultiplicativeBoostImpl(new BigDecimal("1.5"));
    BigDecimal result = boost.boost(new BigDecimal("200"));
    assertEquals(0, new BigDecimal("300.0").compareTo(result), "got " + result);
  }

  @Test
  void multiplicativeIdentityLeavesBaseUnchanged() {
    Boost boost = new MultiplicativeBoostImpl(BigDecimal.ONE);
    BigDecimal base = new BigDecimal("77.25");
    assertEquals(0, base.compareTo(boost.boost(base)));
  }

  @Test
  void factoryCreatesWorkingAdditiveAndMultiplicative() {
    Boost add = BoostFactoryImpl.INSTANCE.additive(new BigDecimal("10"));
    Boost multi = BoostFactoryImpl.INSTANCE.multiplicative(new BigDecimal("2"));

    assertInstanceOf(AdditiveBoostImpl.class, add);
    assertInstanceOf(MultiplicativeBoostImpl.class, multi);

    BigDecimal chained = multi.boost(add.boost(new BigDecimal("50")));
    // (50 + 10) * 2 = 120
    assertEquals(0, new BigDecimal("120").compareTo(chained), "got " + chained);
  }

  @Test
  void combinedAddThenMultiplyDiffersFromMultiplyThenAdd() {
    Boost add = new AdditiveBoostImpl(new BigDecimal("20"));
    Boost multi = new MultiplicativeBoostImpl(new BigDecimal("2"));
    BigDecimal base = new BigDecimal("100");

    BigDecimal addFirst = multi.boost(add.boost(base)); // (100+20)*2 = 240
    BigDecimal multiFirst = add.boost(multi.boost(base)); // 100*2+20 = 220

    assertEquals(0, new BigDecimal("240").compareTo(addFirst));
    assertEquals(0, new BigDecimal("220").compareTo(multiFirst));
    assertNotEquals(addFirst, multiFirst);
  }
}
