package net.aincraft.api.container;

import java.math.BigDecimal;
import net.aincraft.economy.Currency;

sealed class PayableAmountImpl implements PayableAmount permits CurrencyAmountImpl {

  private final BigDecimal amount;

  PayableAmountImpl(BigDecimal amount) {
    this.amount = amount;
  }

  @Override
  public BigDecimal getAmount() {
    return amount;
  }

  static final class BuilderImpl implements PayableAmount.Builder {

    BigDecimal amount;

    @Override
    public Builder withAmount(BigDecimal amount) {
      this.amount = amount;
      return this;
    }

    @Override
    public CurrencyAmount.Builder withCurrency(Currency currency) {
      return new CurrencyAmountImpl.BuilderImpl(this).withCurrency(currency);
    }

    @Override
    public PayableAmount build() {
      return new PayableAmountImpl(amount);
    }
  }
}
