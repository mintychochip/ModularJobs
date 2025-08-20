package net.aincraft.commands;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public final class CommandModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder<JobsCommand> binder = Multibinder.newSetBinder(binder(), JobsCommand.class);
    binder.addBinding().to(JoinCommand.class);
  }
}
