package net.aincraft.container.boost;

import java.time.Duration;
import net.aincraft.container.RuledBoostSource;

public class CombinedBoostDataImpl implements ItemBoostData.Passive, ItemBoostData.Consumable {

  @Override
  public Duration getDuration() {
    return null;
  }

  @Override
  public int[] getApplicableSlots() {
    return new int[0];
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return null;
  }
}
