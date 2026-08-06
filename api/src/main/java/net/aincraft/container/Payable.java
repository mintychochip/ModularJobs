package net.aincraft.container;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

@NonExtendable
public record Payable(PayableType type, PayableAmount amount) implements ComponentLike {

  private static final int ROUNDING_PLACES = 2;
  @Override
  public @NotNull Component asComponent() {
    return type.render(amount,ROUNDING_PLACES);
  }
}
