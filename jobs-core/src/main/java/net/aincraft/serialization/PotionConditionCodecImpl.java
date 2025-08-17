package net.aincraft.serialization;

import net.aincraft.boost.conditions.PotionConditionImpl;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.kyori.adventure.key.Key;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public record PotionConditionCodecImpl() implements Codec.Typed<PotionConditionImpl> {

  @Override
  public void encode(BinaryOut out, PotionConditionImpl object, Writer writer) {
    out.writeByte((byte) object.type().getId());
    out.writeInt(object.expected());
    out.writeEnum(object.conditionType());
    out.writeEnum(object.relationalOperator());
  }

  @Override
  public PotionConditionImpl decode(BinaryIn in, Reader reader) {
    PotionEffectType potionEffectType = PotionEffectType.getById(in.readByte());
    int expected = in.readInt();
    PotionConditionType conditionType = in.readEnum(PotionConditionType.class);
    RelationalOperator relationalOperator = in.readEnum(RelationalOperator.class);
    return new PotionConditionImpl(potionEffectType, expected, conditionType, relationalOperator);
  }

  @Override
  public Class<?> type() {
    return PotionConditionImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:potion_condition");
  }
}
