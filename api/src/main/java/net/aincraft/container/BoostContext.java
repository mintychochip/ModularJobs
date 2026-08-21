package net.aincraft.container;

import java.util.UUID;
import net.aincraft.JobProgressionView;
import dev.conditions.ConditionContext;

/**
 * Immutable context supplied while evaluating a boost for a player and job
 * progression.
 *
 * @param type action that triggered the boost evaluation
 * @param progression current progression view associated with the player
 * @param playerId unique identifier of the player
 * @param worldName name of the world in which the action occurred
 * @param payable payable affected by the boost
 * @param conditions player snapshot for {@link dev.conditions.Condition}
 */
public record BoostContext(
    ActionType type,
    JobProgressionView progression,
    UUID playerId,
    String worldName,
    Payable payable,
    ConditionContext conditions) {

  /**
   * Builds a context with an absent condition snapshot (tests / fail-closed).
   */
  public BoostContext(
      ActionType type,
      JobProgressionView progression,
      UUID playerId,
      String worldName,
      Payable payable) {
    this(type, progression, playerId, worldName, payable, ConditionContext.absent());
  }
}
