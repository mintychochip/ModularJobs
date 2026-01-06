package net.aincraft.container;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public final class PayableAmountImpl implements PayableAmount {

  private final BigDecimal amount;
  private final Currency currency;

  PayableAmountImpl(BigDecimal amount) {
    this.amount = Objects.requireNonNull(amount, "amount cannot be null");
    this.currency = null;
  }

  PayableAmountImpl(BigDecimal amount, Currency currency) {
    this.amount = Objects.requireNonNull(amount, "amount cannot be null");
    this.currency = currency;
  }

  @Override
  public BigDecimal value() {
    return amount;
  }

  @Override
  public @NotNull Optional<Currency> currency() {
    return Optional.ofNullable(currency);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PayableAmountImpl that)) return false;
    return Objects.equals(amount, that.amount) && Objects.equals(currency, that.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, currency);
  }

  @Override
  public String toString() {
    return currency().isEmpty()
        ? amount.toString()
        : amount + " " + currency().get().symbol();
  }
}
