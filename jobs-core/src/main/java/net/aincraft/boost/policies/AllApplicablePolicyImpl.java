package net.aincraft.boost.policies;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Queue;
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
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

record AllApplicablePolicyImpl() implements Policy {

  static Policy INSTANCE = new AllApplicablePolicyImpl();

  @Override
  public List<Boost> resolve(Queue<Rule> rules) {
    return rules.stream().map(Rule::boost).toList();
  }

  static final class CodecImpl implements Codec.Typed<AllApplicablePolicyImpl> {


    @Override
    public Class<?> type() {
      return AllApplicablePolicyImpl.class;
    }

    @Override
    public @NotNull Key key() {
      return Key.key("modular_jobs:all_applicable_policy");
    }

    @Override
    public void encode(Out out, AllApplicablePolicyImpl object, Writer writer) {

    }

    @Override
    public AllApplicablePolicyImpl decode(In in, Reader reader) {
      return new AllApplicablePolicyImpl();
    }
  }
}
