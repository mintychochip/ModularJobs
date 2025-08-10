package net.aincraft.api.container;

import java.math.BigDecimal;
import net.aincraft.api.container.PayableAmountImpl.BuilderImpl;
import net.aincraft.economy.Currency;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

/**
 * Represents a virtual numeric value, such as for experience or currency.
 * <p>
 * Use {@link #builder()} to construct either a simple {@link PayableAmount}
 * or a currency-aware {@link CurrencyAmount} by providing currency metadata.
 * </p>
 *
 * @see CurrencyAmount
 */
@NonExtendable
public sealed interface PayableAmount permits CurrencyAmount, PayableAmountImpl {

  /**
   * Creates a basic {@link PayableAmount} with the given value.
   *
   * @param amount the amount
   * @return a new {@link PayableAmount}
   */
  static PayableAmount create(BigDecimal amount) {
    return new PayableAmountImpl(amount);
  }

  /**
   * Begins building a {@link PayableAmount}. The builder promotes to a
   * {@link CurrencyAmount.Builder} when currency metadata is provided.
   *
   * <pre>{@code
   * PayableAmount plain = PayableAmount.builder()
   *     .withAmount(BigDecimal.TEN)
   *     .build();
   *
   * CurrencyAmount currency = PayableAmount.builder()
   *     .withIdentifier("gold")
   *     .withSymbol("🪙")
   *     .withAmount(BigDecimal.valueOf(100))
   *     .build();
   * }</pre>
   */
  static Builder builder() {
    return new BuilderImpl();
  }

  /**
   * Returns the numeric amount.
   */
  BigDecimal getAmount();

  /**
   * Promoting builder for {@link PayableAmount} or {@link CurrencyAmount}.
   */
  interface Builder {

    /**
     * Sets the amount.
     */
    Builder withAmount(BigDecimal amount);

    /**
     * Promotes to a {@link CurrencyAmount.Builder} by setting the currency identifier.
     */
    CurrencyAmount.Builder withCurrency(Currency currency);

    /**
     * Builds the result.
     * Returns {@link CurrencyAmount} if currency data was set.
     */
    PayableAmount build();
  }
}
