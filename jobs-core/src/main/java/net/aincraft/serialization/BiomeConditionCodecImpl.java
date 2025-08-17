package net.aincraft.serialization;

import net.aincraft.boost.conditions.BiomeConditionImpl;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record BiomeConditionCodecImpl() implements Codec.Typed<BiomeConditionImpl> {

  @Override
  public Class<BiomeConditionImpl> type() {
    return BiomeConditionImpl.class;
  }

  @Override
  public void encode(BinaryOut out, BiomeConditionImpl condition, Writer writer) {
    out.writeKey(condition.biomeKey());
  }

  @Override
  public BiomeConditionImpl decode(BinaryIn in, Reader reader) {
    return new BiomeConditionImpl(in.readKey());
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:biome_condition");
  }
}
