package net.aincraft.service;

import com.google.inject.Inject;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.TimedBoostData;
import net.aincraft.container.boost.TimedBoostDataService;
import net.aincraft.container.boost.TimedBoostDataService.Target.GlobalTarget;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.aincraft.repository.TimedBoostRepository;
import org.bukkit.entity.Player;

public class TimedBoostDataServiceImpl implements TimedBoostDataService {

  private static final String GLOBAL_IDENTIFIER = "global";

  private final TimedBoostRepository timedBoostRepository;

  @Inject
  public TimedBoostDataServiceImpl(TimedBoostRepository timedBoostRepository) {
    this.timedBoostRepository = timedBoostRepository;
  }

  @Override
  public List<ActiveBoostData> findApplicableBoosts(Target target) {
    List<ActiveBoostData> allBoosts;
    if (target instanceof GlobalTarget) {
      allBoosts = timedBoostRepository.findAllBoosts(GLOBAL_IDENTIFIER);
    } else {
      String playerIdentifier = getPlayerIdentifier((PlayerTarget) target);
      allBoosts = timedBoostRepository.findAllBoosts(playerIdentifier);
      allBoosts.addAll(timedBoostRepository.findAllBoosts(GLOBAL_IDENTIFIER));
    }

    // Filter out expired boosts and clean up database
    return allBoosts.stream()
        .filter(boost -> {
          if (boost.isExpired()) {
            // Remove expired boost from database
            timedBoostRepository.delete(boost.targetIdentifier(), boost.sourceIdentifier());
            return false;
          }
          return true;
        })
        .toList();
  }

  @Override
  public List<ActiveBoostData> findBoosts(Target target) {
    if (target instanceof GlobalTarget) {
      return timedBoostRepository.findAllBoosts(GLOBAL_IDENTIFIER);
    }
    String playerIdentifier = getPlayerIdentifier((PlayerTarget) target);
    return timedBoostRepository.findAllBoosts(playerIdentifier);
  }

  @Override
  public <T extends TimedBoostData & SerializableBoostData> void addData(T data, Target target) {
    String targetIdentifier =
        target instanceof PlayerTarget playerTarget ? playerTarget.player().getUniqueId().toString()
            : "global";
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
        ? playerTarget.player().getUniqueId().toString()
        : GLOBAL_IDENTIFIER;

    ActiveBoostData existing = timedBoostRepository.findBoost(targetIdentifier, sourceIdentifier);
    if (existing == null) {
      return false;
    }

    timedBoostRepository.delete(targetIdentifier, sourceIdentifier);
    return true;
  }

  private static String getPlayerIdentifier(PlayerTarget playerTarget) {
    Player player = playerTarget.player();
    UUID uniqueId = player.getUniqueId();
    return uniqueId.toString();
  }
}
