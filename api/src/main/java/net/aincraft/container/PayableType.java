package net.aincraft.container;

import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface PayableType extends Keyed {

  PayableHandler handler();

  Component render(PayableAmount amount, int places);
}
