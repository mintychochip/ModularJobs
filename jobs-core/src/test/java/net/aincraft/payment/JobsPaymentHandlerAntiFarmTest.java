package net.aincraft.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Drives shipped {@link JobsPaymentHandler#applyMultiplier} (tier anti-farm XP scale).
 */
class JobsPaymentHandlerAntiFarmTest {

  @Test
  void zeroMultiplierYieldsZero() {
    assertEquals(0, BigDecimal.ZERO.compareTo(
        JobsPaymentHandler.applyMultiplier(new BigDecimal("10"), 0.0)));
  }

  @Test
  void fullMultiplierUnchanged() {
    BigDecimal base = new BigDecimal("12.5");
    assertEquals(0, base.compareTo(JobsPaymentHandler.applyMultiplier(base, 1.0)));
  }

  @Test
  void halfMultiplierScales() {
    BigDecimal result = JobsPaymentHandler.applyMultiplier(new BigDecimal("10"), 0.5);
    assertEquals(0, new BigDecimal("5.0000").compareTo(result));
  }

  @Test
  void quarterMultiplierScales() {
    BigDecimal result = JobsPaymentHandler.applyMultiplier(new BigDecimal("8"), 0.25);
    assertEquals(0, new BigDecimal("2.0000").compareTo(result));
  }
}
