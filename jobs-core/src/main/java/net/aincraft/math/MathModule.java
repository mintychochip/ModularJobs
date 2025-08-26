package net.aincraft.math;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public final class MathModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(JobCurveFactory.class).to(JobCurveFactoryImpl.class).in(Singleton.class);
  }
}
