package net.aincraft.boost;

import java.math.BigDecimal;
import net.aincraft.api.container.Boost;
import net.aincraft.api.container.BoostType;
import net.aincraft.api.container.boost.factories.BoostFactory;

public final class BoostFactoryImpl implements BoostFactory {

  public static final BoostFactory INSTANCE = new BoostFactoryImpl();

  private BoostFactoryImpl() {

  }
  @Override
  public Boost additive(BigDecimal amount) {
    return new AdditiveBoostImpl(amount);
  }

  @Override
  public Boost multiplicative(BigDecimal amount) {
    return new MultiplicativeBoostImpl(amount);
  }
}
