package net.aincraft.domain.model;

import java.math.BigDecimal;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable record of a player's progression within a single job.
 *
 * @param playerId   the owning player id
 * @param jobRecord  the job this progression belongs to
 * @param experience the accumulated experience toward the job's levels
 */
public record JobProgressionRecord(@NotNull String playerId, @NotNull JobRecord jobRecord,
                                   @NotNull BigDecimal experience) {

}
