package net.aincraft.boost;

import java.math.BigDecimal;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostType;
import net.aincraft.api.container.boost.factories.BoostFactory;

public class BoostFactoryImpl implements BoostFactory {

  @Override
  public Boost additive(BigDecimal amount) {
    return new AdditiveBoostImpl(amount);
  }

  @Override
  public Boost multiplicative(BigDecimal amount) {
    return new MultiplicativeBoostImpl(amount);
  }
}
