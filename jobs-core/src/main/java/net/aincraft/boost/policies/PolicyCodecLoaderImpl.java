package net.aincraft.boost.policies;

import java.util.Collection;
import java.util.List;
import net.aincraft.container.Boost;
import net.aincraft.container.BoostContext;
import net.aincraft.container.Codec;
import net.aincraft.container.RuledBoostSource.Policy;
import net.aincraft.container.RuledBoostSource.Rule;
import net.aincraft.container.boost.Condition;
import net.aincraft.container.boost.In;
import net.aincraft.container.boost.LogicalOperator;
import net.aincraft.container.boost.Out;
import net.aincraft.container.boost.PlayerResourceType;
import net.aincraft.container.boost.PotionConditionType;
import net.aincraft.container.boost.RelationalOperator;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.kyori.adventure.key.Key;
import net.aincraft.boost.CodecLoader;
import net.aincraft.boost.policies.AllApplicablePolicyImpl.CodecImpl;

public class PolicyCodecLoaderImpl implements CodecLoader {

  private final List<Codec> CODECS = List.of(
      new CodecImpl(),
      new GetFirstPolicyImpl.CodecImpl(),
      new TopNPolicyImpl.CodecImpl()
  );

  public static final CodecLoader INSTANCE = new PolicyCodecLoaderImpl();

  @Override
  public Collection<Codec> codecs() {
    return CODECS;
  }
}
