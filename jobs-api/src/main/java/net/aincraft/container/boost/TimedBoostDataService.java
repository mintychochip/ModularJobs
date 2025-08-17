package net.aincraft.container.boost;

import java.time.Duration;
import java.util.List;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.BoostData.SerializableBoostData;
import net.aincraft.container.boost.BoostData.Timed;
import net.aincraft.container.boost.TimedBoostDataService.Target.GlobalTarget;
import net.aincraft.container.boost.TimedBoostDataService.Target.PlayerTarget;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

public interface TimedBoostDataService {

  sealed interface Target permits GlobalTarget, PlayerTarget {
    String identifier();
    record GlobalTarget(String identifier) implements Target {}
    record PlayerTarget(Player player) implements Target {

      @Override
      public String identifier() {
        return player.getUniqueId().toString();
      }
    }
  }

  interface TimedActiveBoost {
    String getSourceIdentifier();
    long getStartEpochMillis();
    Duration getDuration();
    BoostSource getBoostSource();
  }

  List<TimedActiveBoost> findGlobalBoosts();
  List<TimedActiveBoost> findBoost(Target target);
  <T extends Timed & SerializableBoostData> void addData(T data, Target target);

}
