package dev.mintychochip.container;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

/**
 * A reward amount paired with the type of currency or experience it grants.
 *
 * <p>This record is the canonical description of a reward and renders itself
 * as an Adventure {@link ComponentLike} through its {@link PayableType}.</p>
 *
 * @param type how the payable is paid out and rendered
 * @param amount the reward quantity and its currency
 */
@NonExtendable
public record Payable(PayableType type, PayableAmount amount) implements ComponentLike {

  private static final int ROUNDING_PLACES = 2;
  /**
   * Renders this payable as a text component, rounding the amount to two
   * decimal places.
   *
   * @return a non-{@code null} component representation of this payable
   */
  @Override
  public @NotNull Component asComponent() {
    return type.render(amount,ROUNDING_PLACES);
  }
}
