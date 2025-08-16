package net.aincraft.serialization.policies;

import java.util.List;
import java.util.Queue;
import net.aincraft.container.Boost;
import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource.Policy;
import net.aincraft.container.RuledBoostSource.Rule;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.Out;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record TopNPolicyImpl(int n) implements Policy {

  @Override
  public List<Boost> resolve(Queue<Rule> rules) {
    return rules.stream().map(Rule::boost).limit(n).toList();
  }

  static final class CodecImpl implements Codec.Typed<TopNPolicyImpl> {

    @Override
    public void encode(Out out, TopNPolicyImpl object, Writer writer) {
      out.writeInt(object.n);
    }

    @Override
    public TopNPolicyImpl decode(In in, Reader reader) {
      return new TopNPolicyImpl(in.readInt());
    }

    @Override
    public Class<?> type() {
      return TopNPolicyImpl.class;
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:top_n_policy");
    }
  }
}
