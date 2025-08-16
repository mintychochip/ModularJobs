package net.aincraft.api.container.boost;

import net.aincraft.api.Bridge;
import net.aincraft.api.container.BoostCondition.BoostContext;
import net.aincraft.api.container.boost.Condition.Codec.Typed;
import net.aincraft.api.container.boost.conditions.ConditionFactory;
import net.kyori.adventure.key.Keyed;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface Condition {

  ConditionFactory FACTORY = Bridge.bridge().conditionFactory();

  boolean applies(BoostContext context);

  static Condition biome(Biome biome) {
    return FACTORY.biome(biome);
  }

  static Condition world(World world) {
    return FACTORY.world(world);
  }

  static Condition playerResource(PlayerResourceType type, double expected,
      RelationalOperator operator) {
    return FACTORY.playerResource(type, expected, operator);
  }

  static Condition sneaking(boolean state) {
    return FACTORY.sneaking(state);
  }

  static Condition sprinting(boolean state) {
    return FACTORY.sprinting(state);
  }

  default Condition and(Condition other) {
    return compose(other, LogicalOperator.AND);
  }

  default Condition or(Condition other) {
    return compose(other, LogicalOperator.OR);
  }

  default Condition negate() {
    return FACTORY.negate(this);
  }

  default Condition compose(Condition other, LogicalOperator operator) {
    return FACTORY.compose(this, other, operator);
  }

  @NonExtendable
  sealed interface Codec extends Keyed permits Typed {

    Class<?> type();

    non-sealed interface Typed<C extends Condition> extends Codec {

      void encode(Out out, C condition, Typed.Writer writer);

      Condition decode(In in, Typed.Reader reader);

      interface Writer {

        void write(Out out, Condition node);
      }

      interface Reader {

        Condition read(In in);
      }
    }
  }

}
