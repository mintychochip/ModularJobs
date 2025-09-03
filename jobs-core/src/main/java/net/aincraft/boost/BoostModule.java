package net.aincraft.boost;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.bukkit.event.Listener;

public final class BoostModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder<Listener> binder = Multibinder.newSetBinder(binder(), Listener.class);
    binder.addBinding().to(ConsumableBoostController.class);
  }
}
