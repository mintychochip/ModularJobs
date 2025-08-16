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
import org.jetbrains.annotations.NotNull;

record SprintConditionImpl(boolean state) implements Condition {

  @Override
  public boolean applies(BoostContext context) {
    return context.player().isSprinting() == state;
  }

  static final class CodecImpl implements Codec.Typed<SprintConditionImpl> {

    @Override
    public Class<SprintConditionImpl> type() {
      return SprintConditionImpl.class;
    }

    @Override
    public void encode(Out out, SprintConditionImpl condition, Writer writer) {
      out.writeBool(condition.state);
    }

    @Override
    public SprintConditionImpl decode(In in, Reader reader) {
      return new SprintConditionImpl(in.readBool());
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:sprint_condition");
    }
  }
}
