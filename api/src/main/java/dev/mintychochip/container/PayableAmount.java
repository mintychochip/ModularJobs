package dev.mintychochip.container;

import java.math.BigDecimal;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

/** Type. */
@NonExtendable
public sealed interface PayableAmount permits PayableAmountImpl {

  /** Create. */
  static PayableAmount create(BigDecimal amount) {
    return new PayableAmountImpl(amount);
  }

  /** Create. */
  static PayableAmount create(BigDecimal amount, Currency currency) {
    return new PayableAmountImpl(amount, currency);
  }

  /** Value. */
  BigDecimal value();

  /** API member. */
  @NotNull
  Optional<Currency> currency();

  /**
   * Returns a new PayableAmount with the given currency, or this if already has the same currency.
   */
  default PayableAmount withCurrency(Currency currency) {
    return currency()
        .map(c -> c.equals(currency) ? this : create(value(), currency))
        .orElseGet(() -> create(value(), currency));
  }

  /** Returns a new PayableAmount without a currency, or this if already has no currency. */
  default PayableAmount withoutCurrency() {
    return currency().isPresent() ? create(value()) : this;
  }
}
