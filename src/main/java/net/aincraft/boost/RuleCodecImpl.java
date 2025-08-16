package net.aincraft.boost;

import net.aincraft.api.container.Boost;
import net.aincraft.api.container.Codec;
import net.aincraft.api.container.RuledBoostSource.Rule;
import net.aincraft.api.container.boost.Condition;
import net.aincraft.api.container.boost.In;
import net.aincraft.api.container.boost.Out;
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
