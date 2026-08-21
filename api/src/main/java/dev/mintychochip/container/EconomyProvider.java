package dev.mintychochip.container;

import java.util.UUID;

/**
 * Abstraction over the server economy used to pay player rewards.
 *
 * <p>Implementations decide how deposit failures are surfaced; callers must
 * not assume a return value of {@code false} is atomic or recoverable.</p>
 */
public interface EconomyProvider {

  /**
   * Returns whether the underlying economy is available and able to receive
   * deposits at this time.
   *
   * @return {@code true} if currency deposits are supported and currently
   *     possible, {@code false} otherwise
   */
  boolean isCurrencySupported();

  /**
   * Deposits the given amount into the player's account.
   *
   * @param playerId unique identifier of the receiving player
   * @param payableAmount amount to deposit, including its currency
   * @return {@code true} if the deposit was applied, {@code false} if it was
   *     declined or failed
   */
  boolean deposit(UUID playerId, PayableAmount payableAmount);
}
