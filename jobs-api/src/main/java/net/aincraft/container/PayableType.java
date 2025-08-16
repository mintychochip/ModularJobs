package net.aincraft.container;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface PayableType extends Keyed permits PayableTypeImpl {

  static PayableType create(PayableHandler handler, Key key) {
    return new PayableTypeImpl(handler,key);
  }

  PayableHandler handler();

  Payable create(PayableAmount amount);

}
