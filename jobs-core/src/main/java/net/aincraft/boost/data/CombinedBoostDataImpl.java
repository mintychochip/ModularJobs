package net.aincraft.boost.data;

import java.time.Duration;
import java.util.Optional;
import net.aincraft.container.boost.ItemBoostData.ConsumableBoostData;
import net.aincraft.container.boost.ItemBoostData.PassiveBoostData;
import net.aincraft.container.boost.RuledBoostSource;

public record CombinedBoostDataImpl(RuledBoostSource boostSource, Duration duration,
                             int[] slots) implements PassiveBoostData,
    ConsumableBoostData {

  @Override
  public Optional<Duration> getDuration() {
    return Optional.ofNullable(duration);
  }

  @Override
  public int[] getApplicableSlots() {
    return slots;
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return boostSource;
  }

}
