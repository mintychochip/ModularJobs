package net.aincraft.gui;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Guice module for GUI components.
 * Note: TriumphGUI handles event registration internally, so we don't need
 * to bind GUIs as Listeners anymore.
 */
public final class GuiModule extends AbstractModule {

  @Override
  protected void configure() {
    // Bind as singleton for dependency injection
    bind(PetSelectionGui.class).in(Singleton.class);
    bind(JobBrowseGui.class).in(Singleton.class);
    bind(UpgradeTreeGui.class).in(Singleton.class);
  }
}
