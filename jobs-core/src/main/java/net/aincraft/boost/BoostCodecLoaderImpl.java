package net.aincraft.boost;

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
public final class BoostCodecLoaderImpl implements CodecLoader {

  private static final List<Codec> CODECS = List.of(
      new AdditiveBoostImpl.CodecImpl(),
      new MultiplicativeBoostImpl.CodecImpl()
  );

  public static final CodecLoader INSTANCE = new BoostCodecLoaderImpl();

  private BoostCodecLoaderImpl() {

  }

  @Override
  public Collection<Codec> codecs() {
    return CODECS;
  }
}
