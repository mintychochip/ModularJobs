package net.aincraft.api.container;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface PayableType extends Keyed permits PayableTypeImpl {

  static PayableType create(PayableHandler handler, Key key) {
    return PayableTypeImpl.create(handler,key);
  }

  PayableHandler handler();

  Payable create(PayableAmount amount);

  default Payable create(PayableAmount.Builder builder) {
    return create(builder.build());
  }
}
