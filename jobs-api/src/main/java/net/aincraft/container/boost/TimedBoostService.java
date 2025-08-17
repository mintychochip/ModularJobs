package net.aincraft.container.boost;

import net.aincraft.container.boost.TimedBoostService.Target.PlayerTarget;

public interface TimedBoostService {

  sealed interface Target permits PlayerTarget {

    record PlayerTarget(org.bukkit.entity.Player player) implements Target {

    }
  }

  void addBoost(TimedBoostData timedBoostData, Target audience);
}
