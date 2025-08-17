package net.aincraft.container.boost;

import java.time.Duration;
import java.util.Optional;

public interface TimedBoostData {
  RuledBoostSource getBoostSource();
  Optional<Duration> getDuration();
}
