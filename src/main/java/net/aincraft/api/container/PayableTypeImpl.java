package net.aincraft.api.container;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record PayableTypeImpl(@NotNull PayableHandler handler, @NotNull Key key) implements PayableType {

  @Override
  public Payable create(PayableAmount amount) {
    return new Payable() {
      @Override
      public PayableType getType() {
        return PayableTypeImpl.this;
      }

      @Override
      public PayableAmount getAmount() {
        return amount;
      }
    };
  }
}
