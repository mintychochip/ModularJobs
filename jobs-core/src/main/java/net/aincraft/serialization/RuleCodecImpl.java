package net.aincraft.serialization;

import net.aincraft.container.Boost;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record RuleCodecImpl() implements Codec.Typed<Rule> {

  @Override
  public void encode(BinaryOut out, Rule object, Writer writer) {
    out.writeUnsignedInt(object.priority());
    writer.write(out, object.condition());
    writer.write(out, object.boost());
  }

  @Override
  public Rule decode(BinaryIn in, Reader reader) {
    int priority = in.readUnsignedInt();
    Condition condition = reader.read(in, Condition.class);
    Boost boost = reader.read(in, Boost.class);
    return new Rule(condition, priority, boost);
  }

  @Override
  public Class<?> type() {
    return Rule.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:rule");
  }
}
