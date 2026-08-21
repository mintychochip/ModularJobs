package dev.mintychochip.container;

import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

/**
 * Describes the kind of a {@link Payable}: what handler pays it out and how it
 * is rendered to the player.
 *
 * <p>Extends {@link Keyed}, giving each payable type a unique
 * {@link net.kyori.adventure.key.Key}. Instances are non-extendable and are
 * resolved from the payable-type registry via {@link PayableTypes}.</p>
 */
@NonExtendable
public interface PayableType extends Keyed {

  /**
   * Returns the handler responsible for paying out payables of this type.
   *
   * @return the handler for this payable type
   */
  PayableHandler handler();

  /**
   * Renders the given amount as a text component using this type's
   * presentation.
   *
   * @param amount the amount to render
   * @param places number of decimal places to display
   * @return the component representation of the amount
   */
  Component render(PayableAmount amount, int places);
}
