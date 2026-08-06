package net.aincraft.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.TimedBoostData;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.Target.GlobalTarget;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.aincraft.repository.TimedBoostRepository;

public class TimedBoostDataServiceImpl implements TimedBoostDataService {

  private static final String GLOBAL_IDENTIFIER = "global";

  private final TimedBoostRepository timedBoostRepository;

  public TimedBoostDataServiceImpl(TimedBoostRepository timedBoostRepository) {
    this.timedBoostRepository = timedBoostRepository;
  }

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

  @Override
  public List<ActiveBoostData> findBoosts(Target target) {
    return List.copyOf(loadBoosts(target));
  }

  @Override
  public <T extends TimedBoostData & SerializableBoostData> void addData(T data, Target target) {
    String targetIdentifier =
        target instanceof PlayerTarget playerTarget ? playerTarget.playerId().toString()
            : GLOBAL_IDENTIFIER;
    String sourceIdentifier = data.boostSource().key().toString();
    Timestamp timestamp = Timestamp.from(Instant.now());
    Duration duration = data.getDuration().orElse(null);
    timedBoostRepository.addBoost(
        new ActiveBoostData(targetIdentifier, sourceIdentifier, timestamp, duration,
            data.boostSource()));
  }

  @Override
  public boolean removeBoost(Target target, String sourceIdentifier) {
    String targetIdentifier = target instanceof PlayerTarget playerTarget
        ? playerTarget.playerId().toString()
        : GLOBAL_IDENTIFIER;

    ActiveBoostData existing = timedBoostRepository.findBoost(targetIdentifier, sourceIdentifier);
    if (existing == null) {
      return false;
    }

    timedBoostRepository.delete(targetIdentifier, sourceIdentifier);
    return true;
  }

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
