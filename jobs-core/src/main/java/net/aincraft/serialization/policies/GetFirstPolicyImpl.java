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

record GetFirstPolicyImpl() implements Policy {

  static final Policy INSTANCE = new GetFirstPolicyImpl();

  @Override
  public List<Boost> resolve(Queue<Rule> rules) {
    if (rules.isEmpty()) {
      return List.of();
    }
    Rule rule = rules.poll();
    return List.of(rule.boost());
  }

  static final class CodecImpl implements Codec.Typed<GetFirstPolicyImpl> {

    @Override
    public Class<?> type() {
      return GetFirstPolicyImpl.class;
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:get_first_policy");
    }

    @Override
    public void encode(Out out, GetFirstPolicyImpl object, Writer writer) {

    }

    @Override
    public GetFirstPolicyImpl decode(In in, Reader reader) {
      return new GetFirstPolicyImpl();
    }
  }
}
