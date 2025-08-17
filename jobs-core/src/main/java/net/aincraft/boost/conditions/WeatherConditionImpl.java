package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.WeatherState;
import org.bukkit.World;

public record WeatherConditionImpl(WeatherState state) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    World world = context.world();
    WeatherState current =
        world.isThundering() ? WeatherState.THUNDERING :
            world.hasStorm()     ? WeatherState.RAINING   :
                WeatherState.CLEAR;
    return this.state == current;
  }

}
