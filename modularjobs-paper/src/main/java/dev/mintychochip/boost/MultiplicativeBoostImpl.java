package dev.mintychochip.boost;

import dev.mintychochip.container.Boost;
import java.math.BigDecimal;

/**
 * A boost that scales the payout by a multiplicative factor: the resulting payout is {@code base *
 * amount}. A factor of {@code 1} is a no-op, {@code >1} increases pay, and {@code 0..1} reduces
 * pay.
 */
public record MultiplicativeBoostImpl(BigDecimal amount) implements Boost {

  @Override
  public BigDecimal boost(BigDecimal amount) {
    return amount.multiply(this.amount);
  }
}
