package net.aincraft.conditions;

import java.math.BigDecimal;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
import net.aincraft.api.container.boost.PlayerResourceType;
import net.aincraft.api.container.boost.RelationalOperator;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record PlayerResourceConditionImpl(PlayerResourceType type, double expected,
                                   RelationalOperator relationalOperator) implements
    Condition {

  @Override
  public boolean applies(BoostContext context) {
    double actual = switch (type) {
      case HEALTH -> context.player().getHealth();
      case HUNGER -> context.player().getFoodLevel();
      case EXPERIENCE -> context.player().getExp();
      case EXPERIENCE_LEVEL -> context.player().getLevel();
    };
    return relationalOperator.test(BigDecimal.valueOf(actual),BigDecimal.valueOf(expected));
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
      out.writeEnum(condition.relationalOperator);
    }

    @Override
    public Condition decode(In in, Reader reader) {
      PlayerResourceType type = in.readEnum(PlayerResourceType.class);
      double expected = in.readDouble();
      RelationalOperator operator = in.readEnum(RelationalOperator.class);
      return new PlayerResourceConditionImpl(type,expected,operator);
    }

    @Override
    public @NotNull Key key() {
      return Key.key("conditions:player_resource");
    }
  }
}
