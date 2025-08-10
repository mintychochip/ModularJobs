package net.aincraft.api.container;

import net.aincraft.economy.Currency;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface CurrencyAmount extends PayableAmount permits CurrencyAmountImpl {

  Currency getCurrency();

  interface Builder extends PayableAmount.Builder {

    CurrencyAmount build();
  }
}
