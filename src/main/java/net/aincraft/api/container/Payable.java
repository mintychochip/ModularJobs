package net.aincraft.api.container;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface Payable {

  static Payable create(PayableType type, PayableAmount amount) {
    return new Payable() {
      @Override
      public PayableType getType() {
        return type;
      }

      @Override
      public PayableAmount getAmount() {
        return amount;
      }
    };
  }

  PayableType getType();

  PayableAmount getAmount();
}
