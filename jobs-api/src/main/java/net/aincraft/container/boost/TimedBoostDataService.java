package net.aincraft.container.boost;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.TimedBoostData;
import net.aincraft.container.boost.TimedBoostDataService.Target.GlobalTarget;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface TimedBoostDataService {

  sealed interface Target permits GlobalTarget, PlayerTarget {

    record GlobalTarget() implements Target {}

    record PlayerTarget(Player player) implements Target {}
  }

  //TODO: handle cleaning of the db
  record ActiveBoostData(String targetIdentifier, String sourceIdentifier, Timestamp started,
                         @Nullable Duration duration, BoostSource boostSource) {

    public boolean isExpired() {
      if (duration == null) {
        return false; // Permanent boost
      }
      long expiresAt = started.getTime() + duration.toMillis();
      return System.currentTimeMillis() > expiresAt;
    }
  }

  List<ActiveBoostData> findApplicableBoosts(Target target);

  List<ActiveBoostData> findBoosts(Target target);

  <T extends TimedBoostData & SerializableBoostData> void addData(T data, Target target);

  /**
   * Remove a timed boost from a target.
   *
   * @param target           the target (player or global)
   * @param sourceIdentifier the boost source key string
   * @return true if a boost was removed, false otherwise
   */
  boolean removeBoost(Target target, String sourceIdentifier);

}
