package net.aincraft.serialization.conditions;

import java.math.BigDecimal;
import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.RelationalOperator;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record PlayerResourceConditionImpl(PlayerResourceType type, double expected,
                                   RelationalOperator operator) implements
    Condition {

  @Override
  public boolean applies(BoostContext context) {
    double actual = type.getValue(context.player());
    return operator.test(BigDecimal.valueOf(actual), BigDecimal.valueOf(expected));
  }

  static final class CodecImpl implements Codec.Typed<PlayerResourceConditionImpl> {

    @Override
    public Class<PlayerResourceConditionImpl> type() {
      return PlayerResourceConditionImpl.class;
    }

    @Override
    public void encode(Out out, PlayerResourceConditionImpl condition, Writer writer) {
      out.writeEnum(condition.type);
      out.writeDouble(condition.expected);
      out.writeEnum(condition.operator);
    }

    @Override
    public PlayerResourceConditionImpl decode(In in, Reader reader) {
      PlayerResourceType type = in.readEnum(PlayerResourceType.class);
      double expected = in.readDouble();
      RelationalOperator operator = in.readEnum(RelationalOperator.class);
      return new PlayerResourceConditionImpl(type, expected, operator);
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:player_resource_condition");
    }
  }
}
