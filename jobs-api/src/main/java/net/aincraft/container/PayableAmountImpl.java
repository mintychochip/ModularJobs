package net.aincraft.container;

import java.math.BigDecimal;
import java.util.Optional;
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
  public BigDecimal amount() {
    return amount;
  }

  @Override
  public @NotNull Optional<Currency> currency() {
    return Optional.ofNullable(currency);
  }
}
