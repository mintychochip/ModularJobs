package net.aincraft.serialization;

import java.util.ArrayList;
import java.util.List;
import net.aincraft.boost.RuledBoostSourceImpl;
import net.aincraft.container.boost.RuledBoostSource.Policy;
import net.aincraft.container.boost.RuledBoostSource.Rule;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public record RuledBoostCodecImpl() implements Codec.Typed<RuledBoostSourceImpl> {

  @Override
  public void encode(BinaryOut out, RuledBoostSourceImpl object, Writer writer) {
    writer.write(out, object.policy());
    List<Rule> ruleList = object.rules();
    out.writeUnsignedInt(ruleList.size());
    for (Rule rule : ruleList) {
      writer.write(out, rule);
    }
  }

  @Override
  public RuledBoostSourceImpl decode(BinaryIn in, Reader reader) {
    Policy policy = reader.read(in, Policy.class);
    int len = in.readUnsignedInt();
    List<Rule> ruleList = new ArrayList<>();
    for (int i = 0; i < len; ++i) {
      Rule rule = reader.read(in, Rule.class);
      ruleList.add(rule);
    }
    return new RuledBoostSourceImpl(ruleList, policy);
  }

  @Override
  public Class<?> type() {
    return RuledBoostSourceImpl.class;
  }

  @Override
  public @NotNull Key key() {
    return Key.key("modular_jobs:ruled_boost_source");
  }
}
