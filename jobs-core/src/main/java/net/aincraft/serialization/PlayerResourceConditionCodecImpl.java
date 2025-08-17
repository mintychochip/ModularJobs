package net.aincraft.serialization;

import net.aincraft.boost.conditions.PlayerResourceConditionImpl;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.RelationalOperator;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record PlayerResourceConditionCodecImpl() implements
    Codec.Typed<PlayerResourceConditionImpl> {

  @Override
  public Class<PlayerResourceConditionImpl> type() {
    return PlayerResourceConditionImpl.class;
  }

  @Override
  public void encode(BinaryOut out, PlayerResourceConditionImpl condition, Writer writer) {
    out.writeEnum(condition.type());
    out.writeDouble(condition.expected());
    out.writeEnum(condition.operator());
  }

  @Override
  public PlayerResourceConditionImpl decode(BinaryIn in, Reader reader) {
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
