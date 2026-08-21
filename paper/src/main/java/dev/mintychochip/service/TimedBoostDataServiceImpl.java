package dev.mintychochip.service;

import dev.mintychochip.container.boost.BoostData.SerializableBoostData;
import dev.mintychochip.container.boost.BoostData.TimedBoostData;
import dev.mintychochip.container.boost.TimedBoostDataService;
import dev.mintychochip.container.boost.TimedBoostDataService.Target.GlobalTarget;
import dev.mintychochip.container.boost.TimedBoostDataService.Target.PlayerTarget;
import dev.mintychochip.repository.TimedBoostRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@link TimedBoostDataService} backed by a {@link TimedBoostRepository}.
 *
 * <p>Player-targeted boosts are stored under the player {@code UUID} string; global boosts use the
 * fixed identifier {@code "global"}. Queries for a player also surface global boosts and prune
 * expired entries from storage as a side effect.
 */
public class TimedBoostDataServiceImpl implements TimedBoostDataService {

  private static final String GLOBAL_IDENTIFIER = "global";

  private final TimedBoostRepository timedBoostRepository;

  /** Timed boost data service impl. */
  public TimedBoostDataServiceImpl(TimedBoostRepository timedBoostRepository) {
    this.timedBoostRepository = timedBoostRepository;
  }

  /**
   * Returns non-expired boosts applying to {@code target}; for player targets, this includes the
   * global boosts. Expired boosts encountered are removed from storage.
   */
  @Override
  public List<ActiveBoostData> findApplicableBoosts(Target target) {
    List<ActiveBoostData> allBoosts = new ArrayList<>(loadBoosts(target));
    // Include global boosts for player targets
    if (target instanceof PlayerTarget) {
      allBoosts.addAll(timedBoostRepository.findAllBoosts(GLOBAL_IDENTIFIER));
    }

    long now = System.currentTimeMillis();
    List<ActiveBoostData> applicable = new ArrayList<>();
    for (ActiveBoostData boost : allBoosts) {
      if (boost.isExpired(now)) {
        // Cleanup: remove expired boost from storage
        timedBoostRepository.delete(boost.targetIdentifier(), boost.sourceIdentifier());
      } else {
        applicable.add(boost);
      }
    }
    return applicable;
  }

  /** Returns all boosts targeting {@code target} without pruning expired entries. */
  @Override
  public List<ActiveBoostData> findBoosts(Target target) {
    return List.copyOf(loadBoosts(target));
  }

  /** Persists a new time-based boost for {@code target}. */
  @Override
  public <T extends TimedBoostData & SerializableBoostData> void addData(T data, Target target) {
    String targetIdentifier =
        target instanceof PlayerTarget playerTarget
            ? playerTarget.playerId().toString()
            : GLOBAL_IDENTIFIER;
    String sourceIdentifier = data.boostSource().key().toString();
    Instant timestamp = Instant.now();
    Duration duration = data.getDuration().orElse(null);
    timedBoostRepository.addBoost(
        new ActiveBoostData(
            targetIdentifier, sourceIdentifier, timestamp, duration, data.boostSource()));
  }

  /**
   * Removes the boost for {@code target} with the given source identifier.
   *
   * @return whether a matching boost existed and was removed
   */
  @Override
  public boolean removeBoost(Target target, String sourceIdentifier) {
    String targetIdentifier =
        target instanceof PlayerTarget playerTarget
            ? playerTarget.playerId().toString()
            : GLOBAL_IDENTIFIER;

    ActiveBoostData existing = timedBoostRepository.findBoost(targetIdentifier, sourceIdentifier);
    if (existing == null) {
      return false;
    }

    timedBoostRepository.delete(targetIdentifier, sourceIdentifier);
    return true;
  }

  /** Loads stored boosts for {@code target}, mapping global targets to the global identifier. */
  private List<ActiveBoostData> loadBoosts(Target target) {
    if (target instanceof GlobalTarget) {
      return timedBoostRepository.findAllBoosts(GLOBAL_IDENTIFIER);
    }
    String playerIdentifier = getPlayerIdentifier((PlayerTarget) target);
    return timedBoostRepository.findAllBoosts(playerIdentifier);
  }

  private static String getPlayerIdentifier(PlayerTarget playerTarget) {
    UUID uniqueId = playerTarget.playerId();
    return uniqueId.toString();
  }
}
