package dev.mintychochip.container;

import dev.mintychochip.JobProgression;
import java.util.UUID;

/** Applies a {@link Payable} reward to a player for a given job progression. */
@FunctionalInterface
public interface PayableHandler {

  /**
   * Pays out the reward described by the given context.
   *
   * @param context details of the player, payable, and progression
   * @throws IllegalArgumentException if the context is invalid or the payable cannot be applied to
   *     this handler
   */
  void pay(PayableContext context);

  /**
   * Immutable context describing a single payout.
   *
   * @param playerId unique identifier of the receiving player
   * @param payable the reward to pay
   * @param jobProgression progression the reward is granted for
   */
  record PayableContext(UUID playerId, Payable payable, JobProgression jobProgression) {}

  /** Controls how a payout is presented visually to the player. */
  @FunctionalInterface
  interface PayableVisualController {
    /**
     * Renders the payout described by the given context.
     *
     * @param context details of the payout to display
     */
    void display(PayableContext context);
  }
}
