package net.aincraft.api.container;

import java.util.function.Function;

record SimpleBoostImpl(BoostType type, Function<Number, Number> operand) implements Boost {

  @Override
  public Number apply(Number number) {
    return operand.apply(number);
  }
}
