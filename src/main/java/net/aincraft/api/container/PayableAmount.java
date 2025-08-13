package net.aincraft.api.container;

import java.math.BigDecimal;
import net.aincraft.economy.Currency;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Nullable;

@NonExtendable
public sealed interface PayableAmount permits PayableAmountImpl {

  static PayableAmount create(BigDecimal amount) {
    return new PayableAmountImpl(amount);
  }

  static PayableAmount create(BigDecimal amount, Currency currency) {
    return new PayableAmountImpl(amount,currency);
  }

  BigDecimal getAmount();

  @Nullable
  Currency getCurrency();
}
