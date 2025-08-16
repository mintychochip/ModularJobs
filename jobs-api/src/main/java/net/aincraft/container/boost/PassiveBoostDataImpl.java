package net.aincraft.container.boost;

import net.aincraft.container.RuledBoostSource;

public class PassiveBoostDataImpl implements ItemBoostData.Passive {


  @Override
  public int[] getApplicableSlots() {
    return new int[0];
  }

  @Override
  public RuledBoostSource getBoostSource() {
    return null;
  }
}
