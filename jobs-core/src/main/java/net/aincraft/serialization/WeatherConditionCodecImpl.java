package net.aincraft.serialization;

import net.aincraft.boost.conditions.WeatherConditionImpl;
import net.aincraft.container.boost.WeatherState;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record WeatherConditionCodecImpl() implements Codec.Typed<WeatherConditionImpl> {

  @Override
  public void encode(BinaryOut out, WeatherConditionImpl object, Writer writer) {
    out.writeEnum(object.state());
  }

  @Override
  public WeatherConditionImpl decode(BinaryIn in, Reader reader) {
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
