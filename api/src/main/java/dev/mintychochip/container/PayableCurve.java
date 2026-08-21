package dev.mintychochip.container;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Computes a payable amount from a set of named variables.
 *
 * <p>Implementations evaluate the curve against the supplied variable map, for example to scale a
 * reward with a player's level derived from the {@code progression}.
 */
@FunctionalInterface
public interface PayableCurve {

  /**
   * Evaluates the curve for the given variables.
   *
   * @param variables named inputs to the curve, for example the current progression level
   * @return the computed amount
   */
  BigDecimal apply(Map<String, Number> variables);
}
