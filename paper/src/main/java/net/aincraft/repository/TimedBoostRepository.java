package net.aincraft.repository;

import java.util.List;
import net.aincraft.container.boost.TimedBoostDataService.ActiveBoostData;
import org.jetbrains.annotations.NotNull;

/**
 * Persistence operations for timed (active) boosts, keyed by target identifier and source
 * identifier.
 *
 * <p>A target identifier addresses a global boost ({@code "global"}) or a player by {@code UUID}
 * string; a source identifier addresses the boost origin. Expired boosts may be removed by the
 * service after querying.
 */
public interface TimedBoostRepository {

  /** Returns all active boosts targeting the given identifier. */
  @NotNull
  List<ActiveBoostData> findAllBoosts(String targetIdentifier);

  /** Returns the active boost for the given target and source, or {@code null} if absent. */
  ActiveBoostData findBoost(String targetIdentifier, String sourceIdentifier);

  /** Deletes the active boost for the given target and source. */
  void delete(String targetIdentifier, String sourceIdentifier);

  /** Persists a new active boost. */
  void addBoost(ActiveBoostData boost);
}
