package net.aincraft.gui;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.bukkit.event.Listener;

public final class GuiModule extends AbstractModule {

  @Override
  protected void configure() {
    // Bind as singleton so the same instance is used for injection and listener registration
    bind(PetSelectionGui.class).in(Singleton.class);

    Multibinder<Listener> binder = Multibinder.newSetBinder(binder(), Listener.class);
    binder.addBinding().to(PetSelectionGui.class);
  }
}
