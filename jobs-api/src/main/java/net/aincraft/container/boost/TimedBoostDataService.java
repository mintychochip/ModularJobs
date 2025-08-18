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
  }

  List<ActiveBoostData> findApplicableBoosts(Target target);

  List<ActiveBoostData> findBoosts(Target target);

  <T extends TimedBoostData & SerializableBoostData> void addData(T data, Target target);

}
