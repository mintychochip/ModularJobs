package net.aincraft.serialization;

import net.aincraft.container.Boost;
import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource.Rule;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public final class RuleCodecImpl implements Codec.Typed<Rule> {

  @Override
  public void encode(Out out, Rule object, Writer writer) {
    out.writeInt(object.priority());
    writer.write(out, object.condition());
    writer.write(out, object.boost());
  }

  @Override
  public Rule decode(In in, Reader reader) {
    int priority = in.readInt();
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
