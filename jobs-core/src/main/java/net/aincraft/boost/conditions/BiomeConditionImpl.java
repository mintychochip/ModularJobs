package net.aincraft.boost.conditions;

import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

record BiomeConditionImpl(Key biomeKey) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    Player player = context.player();
    Location location = player.getLocation();
    World world = location.getWorld();
    return biomeKey.equals(world.getBiome(location).getKey());
  }

  static final class CodecImpl implements Codec.Typed<BiomeConditionImpl> {

    @Override
    public Class<BiomeConditionImpl> type() {
      return BiomeConditionImpl.class;
    }

    @Override
    public void encode(Out out, BiomeConditionImpl condition, Writer writer) {
      out.writeKey(condition.biomeKey);
    }

    @Override
    public BiomeConditionImpl decode(In in, Reader reader) {
      return new BiomeConditionImpl(in.readKey());
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:biome_condition");
    }
  }
}
