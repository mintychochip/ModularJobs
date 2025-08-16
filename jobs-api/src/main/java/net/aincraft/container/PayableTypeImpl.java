package net.aincraft.container;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record PayableTypeImpl(@NotNull PayableHandler handler,
                                                @NotNull Key key) implements PayableType {

  @Override
  public Payable create(PayableAmount amount) {
    return Payable.create(this,amount);
  }
}
