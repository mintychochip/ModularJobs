package net.aincraft.api.container;

import java.math.BigDecimal;
import net.aincraft.economy.Currency;

final class CurrencyAmountImpl extends PayableAmountImpl implements CurrencyAmount {

  private final Currency currency;

  CurrencyAmountImpl(BigDecimal amount, Currency currency) {
    super(amount);
    this.currency = currency;
  }

  static final class BuilderImpl implements CurrencyAmount.Builder {

    BigDecimal amount;
    Currency currency;

    BuilderImpl(PayableAmountImpl.BuilderImpl builder) {
      this.amount = builder.amount;
    }

    @Override
    public PayableAmount.Builder withAmount(BigDecimal amount) {
      this.amount = amount;
      return this;
    }

    @Override
    public CurrencyAmount.Builder withCurrency(Currency currency) {
      this.currency = currency;
      return this;
    }

    @Override
    public CurrencyAmount build() {
      return new CurrencyAmountImpl(amount, currency);
    }
  }

  @Override
  public Currency getCurrency() {
    return currency;
  }
}
