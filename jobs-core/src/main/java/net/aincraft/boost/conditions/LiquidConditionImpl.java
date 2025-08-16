package net.aincraft.boost.conditions;

import com.google.common.base.Preconditions;
import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.Out;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.potion.PotionEffectType;
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
