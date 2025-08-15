package net.aincraft.api.container;

import java.math.BigDecimal;
import java.util.Optional;
import net.aincraft.economy.Currency;
import org.jetbrains.annotations.NotNull;

public final class PayableAmountImpl implements PayableAmount {

  private final BigDecimal amount;

  private Currency currency = null;

  PayableAmountImpl(BigDecimal amount) {
    this.amount = amount;
  }

  public PayableAmountImpl(BigDecimal amount, Currency currency) {
    this.amount = amount;
    this.currency = currency;
  }

  @Override
  public BigDecimal getAmount() {
    return amount;
  }

  @Override
  public @NotNull Optional<Currency> getCurrency() {
    return Optional.ofNullable(currency);
  }
}
