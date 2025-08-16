package net.aincraft.container;

import java.math.BigDecimal;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

@NonExtendable
public sealed interface PayableAmount permits PayableAmountImpl {

  static PayableAmount create(BigDecimal amount) {
    return new PayableAmountImpl(amount);
  }

  static PayableAmount create(BigDecimal amount, Currency currency) {
    return new PayableAmountImpl(amount,currency);
  }

  BigDecimal amount();

  @NotNull
  Optional<Currency> currency();
}
