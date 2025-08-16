package net.aincraft.boost.conditions;

import net.aincraft.api.container.BoostContext;
import net.aincraft.api.container.Codec;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

record LiquidConditionImpl(Material liquid) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    Player player = context.player();
    if (liquid == Material.WATER) {
      return player.isInWater();
    }
    return player.isInLava();
  }

  static final class CodecImpl implements Codec.Typed<LiquidConditionImpl> {

    @Override
    public void encode(Out out, LiquidConditionImpl object, Writer writer) {
      out.writeEnum(object.liquid);
    }

    @Override
    public LiquidConditionImpl decode(In in, Reader reader) {
      return new LiquidConditionImpl(in.readEnum(Material.class));
    }

    @Override
    public Class<?> type() {
      return LiquidConditionImpl.class;
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:liquid_condition");
    }
  }
}
