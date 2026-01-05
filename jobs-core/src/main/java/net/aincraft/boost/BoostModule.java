package net.aincraft.boost;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.container.boost.factories.PolicyFactory;
import org.bukkit.event.Listener;

public final class BoostModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(BoostFactory.class).toInstance(BoostFactoryImpl.INSTANCE);
    bind(ConditionFactory.class).toInstance(BoostFactoryImpl.INSTANCE);
    bind(PolicyFactory.class).toInstance(BoostFactoryImpl.INSTANCE);

    Multibinder<Listener> binder = Multibinder.newSetBinder(binder(), Listener.class);
    binder.addBinding().to(ConsumableBoostController.class);
  }
}
