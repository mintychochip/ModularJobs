package net.aincraft.api.container;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface Payable {

  static Payable create(PayableType type, PayableAmount amount) {
    return new Payable() {
      @Override
      public PayableType type() {
        return type;
      }

      @Override
      public PayableAmount amount() {
        return amount;
      }
    };
  }

  PayableType type();

  PayableAmount amount();
}
