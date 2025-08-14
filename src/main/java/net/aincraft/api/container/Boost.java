package net.aincraft.api.container;

import java.util.function.Function;

public interface Boost {

  BoostType type();

  Number apply(Number number);

  static Boost multiplicative(BoostType type, Number amount) {
    return create(type, number -> number.doubleValue() * amount.doubleValue());
  }

  static Boost additive(BoostType type, Number amount) {
    return create(type, number -> number.doubleValue() + amount.doubleValue());
  }

  static Boost create(BoostType type, Function<Number, Number> operand) {
    return new SimpleBoostImpl(type,operand);
  }

}
