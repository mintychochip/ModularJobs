package net.aincraft.api.container;

import java.math.BigDecimal;

public interface Payable {

  static Payable create(PayableType type, BigDecimal amount) {
    return new Payable() {
      @Override
      public PayableType getType() {
        return type;
      }

      @Override
      public BigDecimal getAmount() {
        return amount;
      }
    };
  }

  PayableType getType();
  BigDecimal getAmount();
}
