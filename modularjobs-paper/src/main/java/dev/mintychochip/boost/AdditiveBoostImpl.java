package dev.mintychochip.boost;

import dev.mintychochip.container.Boost;
import java.math.BigDecimal;

/**
 * A directional boost that adds a fixed {@code amount} to the payout: the resulting payout is
 * {@code base + amount}. Direction is implied by the sign of {@code amount} (negative subtracts).
 */
public record AdditiveBoostImpl(BigDecimal amount) implements Boost {

  @Override
  public BigDecimal boost(BigDecimal amount) {
    return amount.add(this.amount);
  }
}
