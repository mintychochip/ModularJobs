package net.aincraft.serialization.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.aincraft.container.boost.WeatherState;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

record WeatherConditionImpl(WeatherState state) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    World world = context.world();
    WeatherState current =
        world.isThundering() ? WeatherState.THUNDERING :
            world.hasStorm()     ? WeatherState.RAINING   :
                WeatherState.CLEAR;
    return this.state == current;
  }

  static final class CodecImpl implements Codec.Typed<WeatherConditionImpl> {

    @Override
    public void encode(Out out, WeatherConditionImpl object, Writer writer) {
      out.writeEnum(object.state);
    }

    @Override
    public WeatherConditionImpl decode(In in, Reader reader) {
      return new WeatherConditionImpl(in.readEnum(WeatherState.class));
    }

    @Override
    public Class<?> type() {
      return WeatherConditionImpl.class;
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:weather_condition");
    }
  }
}
