package dev.mintychochip.repository;

import dev.mintychochip.container.boost.TimedBoostDataService.ActiveBoostData;
import java.util.List;
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

  /** Find boost. */
  ActiveBoostData findBoost(String targetIdentifier, String sourceIdentifier);

  /** Delete. */
  void delete(String targetIdentifier, String sourceIdentifier);

  /** Add boost. */
  void addBoost(ActiveBoostData boost);
}
