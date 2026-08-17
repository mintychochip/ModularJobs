package net.aincraft.container;

import java.util.UUID;
import net.aincraft.JobProgressionView;

/**
 * Immutable context supplied while evaluating a boost for a player and job
 * progression.
 *
 * @param type action that triggered the boost evaluation
 * @param progression current progression view associated with the player
 * @param playerId unique identifier of the player
 * @param worldName name of the world in which the action occurred
 * @param payable payable affected by the boost
 */
public record BoostContext(
    ActionType type,
    JobProgressionView progression,
    UUID playerId,
    String worldName,
    Payable payable) {
}
