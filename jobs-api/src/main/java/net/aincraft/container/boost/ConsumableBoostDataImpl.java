package net.aincraft.container.boost;

import java.time.Duration;
import net.aincraft.container.RuledBoostSource;

public class ConsumableBoostDataImpl implements ItemBoostData.Consumable {

  @Override
  public Duration getDuration() {
    return null;
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return null;
  }
}
