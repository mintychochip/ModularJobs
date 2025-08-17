package net.aincraft.boost.data;

import java.time.Duration;
import java.util.Optional;
import net.aincraft.container.boost.ItemBoostData.ConsumableBoostData;
import net.aincraft.container.boost.RuledBoostSource;

public record ConsumableBoostDataBoostDataImpl(RuledBoostSource boostSource, Duration duration) implements
    ConsumableBoostData {

  @Override
  public Optional<Duration> getDuration() {
    return Optional.ofNullable(duration);
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return boostSource;
  }

}
