package net.aincraft.api.container;

import java.util.function.Function;

public interface Boost {

  BoostType getType();

  Number apply(Number number);

  static Boost multiplicative(BoostType type, Number amount) {
    return create(type, number -> number.doubleValue() * amount.doubleValue());
  }

  static Boost additive(BoostType type, Number amount) {
    return create(type, number -> number.doubleValue() + amount.doubleValue());
  }

  static Boost create(BoostType type, Function<Number, Number> operand) {
    return new Boost() {
      @Override
      public BoostType getType() {
        return type;
      }

      @Override
      public Number apply(Number number) {
        return operand.apply(number);
      }
    };
  }
}
