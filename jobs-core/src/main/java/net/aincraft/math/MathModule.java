package net.aincraft.math;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public final class MathModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ExpressionCurveFactory.class).to(ExpressionCurveFactoryImpl.class).in(Singleton.class);
  }
}
